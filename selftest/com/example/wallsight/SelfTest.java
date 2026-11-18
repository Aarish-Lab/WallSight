package com.example.wallsight;

import com.example.wallsight.client.config.WallSightConfig;
import com.example.wallsight.client.logic.ColorSyncModel;
import com.example.wallsight.client.logic.ColorSyncModel.HexResult;
import com.example.wallsight.client.logic.ColorSyncModel.Source;
import com.example.wallsight.client.logic.ColorTarget;
import com.example.wallsight.client.logic.EspMath;
import com.example.wallsight.client.logic.HexColor;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/** Plain-Java tests (no JUnit): exits non-zero on the first failed check. */
public final class SelfTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        hex();
        sync();
        esp();
        config();
        System.out.println("\nALL " + checks + " CHECKS PASSED");
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            System.out.println("FAIL: " + what);
            System.exit(1);
        }
    }

    // ------------------------------------------------------------ HEX validation
    private static void hex() {
        check(HexColor.parse("#55FFFF") == 0x55FFFF, "uppercase with hash");
        check(HexColor.parse("55ffff") == 0x55FFFF, "lowercase without hash");
        check(HexColor.parse("  #55FfFf  ") == 0x55FFFF, "mixed case, surrounding spaces");
        check(HexColor.parse("#000000") == 0 && HexColor.isValid("#000000"), "black is valid (0 is not 'invalid')");
        check(HexColor.parse("FFFFFF") == 0xFFFFFF, "white");
        for (String bad : new String[] {
            null, "", "#", "#FFF", "#FFFFF", "#FFFFFFF", "##55FFFF", "#GGGGGG", "#55FFF-", "+55FFFF", "5 5FFFF",
            "#55FFF\u0663", "#\uFF15\uFF15FFFF", "0x55FFFF", "#55FFFF;"
        }) {
            check(HexColor.parse(bad) == HexColor.INVALID, "rejects " + (bad == null ? "null" : "'" + bad + "'"));
        }
        check(HexColor.format(0).equals("#000000"), "format 0");
        check(HexColor.format(0x55FFFF).equals("#55FFFF"), "format aqua");
        check(HexColor.format(0x1FF00AA).equals("#FF00AA"), "format masks to 24 bits");
        for (int rgb = 0; rgb <= 0xFFFFFF; rgb += 4099) {
            check(HexColor.parse(HexColor.format(rgb)) == rgb, "round trip " + rgb);
        }
    }

    // ------------------------------------------------------------ colour picker / HEX / swatch sync
    /** Stand-in for the widgets: it echoes back like the real ones do (a refreshed box fires onChanged). */
    private static final class FakeUi implements ColorSyncModel.Listener {
        final ColorSyncModel model;
        final WallSightConfig config;
        final Map<ColorTarget, String> hexText = new EnumMap<>(ColorTarget.class);
        final Map<ColorTarget, Integer> swatch = new EnumMap<>(ColorTarget.class);
        int pickerRgb;
        int events;

        FakeUi(WallSightConfig config) {
            this.config = config;
            model = new ColorSyncModel(config::colorRgb);
            for (ColorTarget t : ColorTarget.values()) {
                hexText.put(t, HexColor.format(model.rgb(t)));
                swatch.put(t, model.rgb(t));
            }
            pickerRgb = model.rgb(model.selected());
            model.setListener(this);
        }

        @Override
        public void colorChanged(ColorTarget target, int rgb, Source source) {
            events++;
            check(events < 50, "event storm: echo loop detected");
            config.setColor(target, rgb);
            swatch.put(target, rgb);
            if (source != Source.HEX_TYPED) {
                hexText.put(target, HexColor.format(rgb));
                model.applyHex(target, hexText.get(target)); // the text box echoes its refreshed text
            }
            if (source != Source.PICKER && target == model.selected()) {
                pickerRgb = rgb;
                model.applyPicker(pickerRgb); // the picker echoes its refreshed colour
            }
        }

        @Override
        public void selectionChanged(ColorTarget selected) {
            events++;
            pickerRgb = model.rgb(selected);
            model.applyPicker(pickerRgb);
        }

        HexResult type(ColorTarget t, String text) {
            events = 0;
            hexText.put(t, text);
            return model.applyHex(t, text);
        }

        void drag(int rgb) {
            events = 0;
            pickerRgb = rgb;
            model.applyPicker(rgb);
        }
    }

    private static void sync() {
        WallSightConfig cfg = new WallSightConfig();
        cfg.sanitize();
        FakeUi ui = new FakeUi(cfg);
        check(ui.model.selected() == ColorTarget.PLAYERS && ui.pickerRgb == 0x55FFFF, "starts on players, aqua");

        // typing a valid HEX updates swatch, picker and config
        check(ui.type(ColorTarget.PLAYERS, "#ff0000") == HexResult.APPLIED, "valid hex applied");
        check(ui.swatch.get(ColorTarget.PLAYERS) == 0xFF0000, "swatch follows hex");
        check(ui.pickerRgb == 0xFF0000, "picker follows hex");
        check(cfg.colorRgb(ColorTarget.PLAYERS) == 0xFF0000 && cfg.playerColor.equals("#FF0000"), "config follows hex");
        check(ui.hexText.get(ColorTarget.PLAYERS).equals("#ff0000"), "the user's own text is not rewritten");
        check(ui.events < 8, "bounded echo, events=" + ui.events);

        // invalid HEX changes nothing
        check(ui.type(ColorTarget.PLAYERS, "#12") == HexResult.INVALID, "partial hex invalid");
        check(ui.type(ColorTarget.PLAYERS, "#zzzzzz") == HexResult.INVALID, "non-hex invalid");
        check(ui.swatch.get(ColorTarget.PLAYERS) == 0xFF0000 && ui.pickerRgb == 0xFF0000, "last valid colour kept");
        check(cfg.colorRgb(ColorTarget.PLAYERS) == 0xFF0000, "config keeps last valid colour");
        check(ui.type(ColorTarget.PLAYERS, "#FF0000") == HexResult.UNCHANGED, "same colour again is a no-op");

        // dragging the picker rewrites the hex text, swatch and config for the selected target
        ui.drag(0x123456);
        check(ui.hexText.get(ColorTarget.PLAYERS).equals("#123456"), "hex follows picker");
        check(ui.swatch.get(ColorTarget.PLAYERS) == 0x123456, "swatch follows picker");
        check(cfg.playerColor.equals("#123456"), "config follows picker");
        check(ui.events < 8, "bounded echo from picker, events=" + ui.events);

        // typing in a box that is not selected leaves the picker alone
        ui.type(ColorTarget.HOSTILE, "#00FF00");
        check(ui.swatch.get(ColorTarget.HOSTILE) == 0x00FF00, "other swatch updates");
        check(ui.pickerRgb == 0x123456, "picker keeps editing the selected target");
        check(cfg.hostileColor.equals("#00FF00"), "other config entry updates");

        // selecting another target loads its colour into the picker without changing any value
        ui.events = 0;
        ui.model.select(ColorTarget.HOSTILE);
        check(ui.pickerRgb == 0x00FF00, "picker shows the newly selected colour");
        check(cfg.playerColor.equals("#123456") && cfg.hostileColor.equals("#00FF00"), "selecting changes no colour");
        ui.drag(0x0000FF);
        check(cfg.hostileColor.equals("#0000FF") && cfg.playerColor.equals("#123456"), "picker now edits hostile only");

        // reset restores the original colours everywhere
        ui.events = 0;
        ui.model.reset(t -> t.defaultRgb);
        for (ColorTarget t : ColorTarget.values()) {
            check(ui.model.rgb(t) == t.defaultRgb && ui.swatch.get(t) == t.defaultRgb, "reset " + t);
            check(ui.hexText.get(t).equals(HexColor.format(t.defaultRgb)), "reset hex text " + t);
            check(cfg.colorRgb(t) == t.defaultRgb, "reset config " + t);
        }
        check(ui.pickerRgb == ui.model.rgb(ui.model.selected()), "reset refreshes the picker");
    }

    // ------------------------------------------------------------ distance slider / range maths
    private static void esp() {
        int[][] clamp = {{-5, 8}, {0, 8}, {7, 8}, {8, 8}, {9, 9}, {64, 64}, {128, 128}, {129, 128},
            {Integer.MAX_VALUE, 128}, {Integer.MIN_VALUE, 8}};
        for (int[] c : clamp) {
            check(EspMath.clampRadius(c[0]) == c[1], "clamp " + c[0] + " -> " + c[1]);
        }
        // the slider reports doubles; the screen rounds then clamps
        check(EspMath.clampRadius((int) Math.round(7.6)) == 8, "slider 7.6 -> 8");
        check(EspMath.clampRadius((int) Math.round(63.5)) == 64, "slider 63.5 -> 64");
        check(EspMath.clampRadius((int) Math.round(128.4)) == 128, "slider 128.4 -> 128");

        double r = EspMath.radiusSq(8);
        check(r == 64.0, "radiusSq(8)");
        check(EspMath.within(8, 0, 0, r), "exactly on the radius is inside (inclusive)");
        check(!EspMath.within(8.0001, 0, 0, r), "just outside");
        check(EspMath.within(4, 4, 4, r) && !EspMath.within(5, 5, 5, r), "3D distance, not per-axis");
        check(EspMath.within(-4, -4, -4, r), "negative offsets");
        check(EspMath.radiusSq(1000) == 128.0 * 128.0, "radiusSq clamps too");
        check(EspMath.chunkRadius(8) == 1 && EspMath.chunkRadius(16) == 1 && EspMath.chunkRadius(17) == 2, "chunk radius");
        check(EspMath.chunkRadius(128) == 8, "chunk radius at max");
        // every block inside the radius lies inside the scanned chunk square
        for (int radius = 8; radius <= 128; radius += 7) {
            int cr = EspMath.chunkRadius(radius);
            for (int offset = -radius; offset <= radius; offset += 5) {
                int playerBlock = 1000 + offset % 16;
                int chunkOfBlock = (playerBlock + offset) >> 4;
                check(Math.abs(chunkOfBlock - (playerBlock >> 4)) <= cr, "container within radius is scanned, r=" + radius);
            }
        }
    }

    // ------------------------------------------------------------ configuration persistence
    private static void config() throws IOException {
        Path dir = Files.createTempDirectory("wallsight-test");
        Path file = dir.resolve("sub").resolve("wallsight.json");

        // defaults (everything off until the master toggle, as in 1.0.2)
        WallSightConfig d = new WallSightConfig();
        d.sanitize();
        check(!d.masterEnabled && d.noKnockback && d.highlightPlayers && d.highlightHostile && d.highlightContainers,
                "default toggles");
        check(d.espRadius == 128, "default radius");
        check(d.colorArgb(ColorTarget.PLAYERS) == 0xFF55FFFF, "default player colour = 1.0.2 colour");
        check(d.colorArgb(ColorTarget.HOSTILE) == 0xFFFF5555, "default hostile colour = 1.0.2 colour");
        check(d.colorArgb(ColorTarget.CONTAINERS) == 0xFFFFAA00, "default container colour = 1.0.2 colour");
        check(d.colorArgb(ColorTarget.PLAYERS) == -11141121 && d.colorArgb(ColorTarget.HOSTILE) == -43691
                && d.colorArgb(ColorTarget.CONTAINERS) == -22016, "ARGB ints identical to the constants in the 1.0.2 jar");

        // missing file keeps whatever is live
        WallSightConfig.install(new WallSightConfig());
        WallSightConfig.load(file);
        check(!WallSightConfig.get().masterEnabled, "missing file -> defaults");

        // save, then load into a fresh instance
        WallSightConfig c = new WallSightConfig();
        c.masterEnabled = true;
        c.noKnockback = false;
        c.highlightHostile = false;
        c.espRadius = 42;
        c.setColor(ColorTarget.PLAYERS, 0x123456);
        c.setColor(ColorTarget.CONTAINERS, 0xABCDEF);
        c.save(file);
        c.save(file); // overwrite path
        String text = Files.readString(file);
        check(text.contains("\"playerColor\": \"#123456\""), "colour stored as readable hex");
        check(Files.list(file.getParent()).count() == 1, "no temp files left behind");
        WallSightConfig.install(new WallSightConfig());
        WallSightConfig.load(file);
        WallSightConfig r = WallSightConfig.get();
        check(r.masterEnabled && !r.noKnockback && !r.highlightHostile && r.highlightPlayers && r.highlightContainers,
                "toggles persist");
        check(r.espRadius == 42, "radius persists");
        check(r.colorRgb(ColorTarget.PLAYERS) == 0x123456 && r.colorRgb(ColorTarget.CONTAINERS) == 0xABCDEF
                && r.colorRgb(ColorTarget.HOSTILE) == ColorTarget.HOSTILE.defaultRgb, "colours persist");

        // a 1.0.2 config (no colour fields) still loads and gets default colours
        Files.writeString(file, "{\n \"masterEnabled\": true,\n \"noKnockback\": false,\n \"highlightPlayers\": true,\n"
                + " \"highlightHostile\": false,\n \"highlightContainers\": true,\n \"espRadius\": 64\n}\n");
        WallSightConfig.load(file);
        r = WallSightConfig.get();
        check(r.masterEnabled && !r.noKnockback && !r.highlightHostile && r.espRadius == 64, "1.0.2 config values kept");
        check(r.colorRgb(ColorTarget.PLAYERS) == 0x55FFFF && r.colorRgb(ColorTarget.HOSTILE) == 0xFF5555
                && r.colorRgb(ColorTarget.CONTAINERS) == 0xFFAA00, "1.0.2 config gets default colours");

        // out-of-range radius and bad colours are repaired
        Files.writeString(file, "{\"espRadius\": 100000, \"playerColor\": \"red\", \"hostileColor\": \"#ff00ff\","
                + " \"containerColor\": null}");
        WallSightConfig.load(file);
        r = WallSightConfig.get();
        check(r.espRadius == 128, "huge radius clamped");
        check(r.colorRgb(ColorTarget.PLAYERS) == 0x55FFFF, "invalid colour name -> default");
        check(r.hostileColor.equals("#FF00FF"), "lower-case hex normalised");
        check(r.colorRgb(ColorTarget.CONTAINERS) == 0xFFAA00, "null colour -> default");
        Files.writeString(file, "{\"espRadius\": -3}");
        WallSightConfig.load(file);
        check(WallSightConfig.get().espRadius == 8, "negative radius clamped");

        // unknown fields are ignored
        Files.writeString(file, "{\"masterEnabled\": true, \"somethingNew\": [1,2,3]}");
        WallSightConfig.load(file);
        check(WallSightConfig.get().masterEnabled, "unknown fields ignored");

        // broken files never throw and fall back to the previous good config
        WallSightConfig.install(new WallSightConfig());
        for (String bad : new String[] {"", "   ", "not json", "{", "[1,2]", "{\"espRadius\": \"wide\"}", "null"}) {
            Files.writeString(file, bad);
            WallSightConfig.load(file);
            check(WallSightConfig.get() != null && WallSightConfig.get().espRadius == 128, "survives broken file: " + bad);
        }
        Files.write(file, new byte[] {(byte) 0xC3, 0x28, (byte) 0xFF, 0x00});
        WallSightConfig.load(file);
        check(WallSightConfig.get().espRadius == 128, "survives binary garbage");
        boolean threw = false;
        try {
            WallSightConfig.parse("null");
        } catch (JsonParseException e) {
            threw = true;
        }
        check(threw, "parse() reports an empty config");

        // setColor keeps the string and the cache in step, and masks to 24 bits
        WallSightConfig s = new WallSightConfig();
        s.setColor(ColorTarget.HOSTILE, 0x1AABBCC);
        check(s.hostileColor.equals("#AABBCC") && s.colorRgb(ColorTarget.HOSTILE) == 0xAABBCC, "setColor masks to 24 bits");
        check(s.colorArgb(ColorTarget.HOSTILE) == 0xFFAABBCC, "colorArgb is opaque");
        check(new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1).length() > 0, "file readable");
    }
}
