package com.example.wallsight.client.ui;

import com.example.wallsight.client.config.ConfigPaths;
import com.example.wallsight.client.config.WallSightConfig;
import com.example.wallsight.client.logic.ColorSyncModel;
import com.example.wallsight.client.logic.ColorTarget;
import com.example.wallsight.client.logic.EspMath;
import com.example.wallsight.client.logic.HexColor;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.ColorPickerComponent;
import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.core.VerticalAlignment;
import java.util.EnumMap;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The WallSight settings screen. Opened with Right Shift (see WallSightKeys) or from Mod Menu.
 * The 1.0.2 controls are unchanged; this version adds the colour section and a scroll area.
 */
public class WallSightScreen extends BaseOwoScreen<FlowLayout> {
    private static final int PANEL_WIDTH = 250;
    private static final int ACCENT = -10929;
    private static final int MUTED = -6641219;
    private static final int DIVIDER = -12959408;
    private static final int TEXT_VALID = 0xFFE0E0E0;
    private static final int TEXT_INVALID = 0xFFFF5555;

    private final WallSightConfig config = WallSightConfig.get();
    private final ColorSyncModel colors = new ColorSyncModel(config::colorRgb);
    private final EnumMap<ColorTarget, TextBoxComponent> hexBoxes = new EnumMap<>(ColorTarget.class);
    private final EnumMap<ColorTarget, BoxComponent> swatches = new EnumMap<>(ColorTarget.class);
    private final EnumMap<ColorTarget, ButtonComponent> pickButtons = new EnumMap<>(ColorTarget.class);
    private ColorPickerComponent picker;

    @Nullable
    private final Screen parent;

    /** Opened from the keybind: closing returns to the game. */
    public WallSightScreen() {
        this(null);
    }

    /** Opened from Mod Menu: closing returns to {@code parent}. */
    public WallSightScreen(@Nullable Screen parent) {
        super(Component.literal("WallSight"));
        this.parent = parent;
    }

    @NotNull
    @Override
    protected OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.surface(Surface.VANILLA_TRANSLUCENT);
        root.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);

        FlowLayout panel = UIContainers.verticalFlow(Sizing.fixed(PANEL_WIDTH), Sizing.fill(92));
        panel.gap(6);
        panel.padding(Insets.of(10));
        panel.surface(Surface.DARK_PANEL);
        panel.child(heading("WallSight"));

        // Everything between the title and the Done button scrolls, so the longer panel stays usable
        // at any GUI scale.
        FlowLayout content = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        content.gap(6);
        content.padding(Insets.right(8)); // room for the scrollbar

        content.child(caption("Local to your client. Everything stays off until the master toggle is on."));
        content.child(checkbox("Master toggle", config.masterEnabled, on -> config.masterEnabled = on));
        content.child(divider());
        content.child(heading("Knockback"));
        content.child(checkbox("Cancel my own knockback", config.noKnockback, on -> config.noKnockback = on));
        content.child(caption("Wind charges and fireballs still knock you back normally."));
        content.child(divider());
        content.child(heading("ESP through walls"));
        content.child(checkbox("Players", config.highlightPlayers, on -> config.highlightPlayers = on));
        content.child(checkbox("Hostile mobs", config.highlightHostile, on -> config.highlightHostile = on));
        content.child(checkbox(
                "Block containers (chests, ender chests)", config.highlightContainers, on -> config.highlightContainers = on));
        content.child(radiusSlider());
        content.child(divider());
        addColourSection(content);

        ScrollContainer<FlowLayout> scroll = UIContainers.verticalScroll(Sizing.fill(100), Sizing.expand(), content);
        panel.child(scroll);
        panel.child(footer());
        root.child(panel);

        installColourSync();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void removed() {
        super.removed();
        config.sanitize();
        config.save(ConfigPaths.file());
    }

    // ---------------------------------------------------------------- colours

    private void addColourSection(FlowLayout content) {
        content.child(heading("ESP colours"));
        content.child(caption(
                "Type a HEX colour such as #55FFFF, or drag in the picker. Click a name to choose which colour the picker edits."));
        for (ColorTarget target : ColorTarget.values()) {
            content.child(colourRow(target));
        }
        picker = new ColorPickerComponent();
        picker.horizontalSizing(Sizing.fill(100));
        picker.verticalSizing(Sizing.fixed(64));
        picker.selectedColor(Color.ofRgb(colors.rgb(colors.selected())));
        picker.onChanged().subscribe(color -> colors.applyPicker(color.rgb()));
        content.child(picker);
        ButtonComponent resetButton = UIComponents.button(Component.literal("Reset colours"), button -> {
            colors.reset(t -> t.defaultRgb);
            // a box holding invalid text matches no colour change, so refresh every box explicitly
            for (ColorTarget t : ColorTarget.values()) {
                hexBoxes.get(t).text(HexColor.format(colors.rgb(t)));
            }
        });
        content.child(UIComponents.wrapVanillaWidget(resetButton));
    }

    private FlowLayout colourRow(ColorTarget target) {
        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.gap(4);
        row.alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);

        ButtonComponent pick = UIComponents.button(Component.literal(pickLabel(target)), button -> colors.select(target));
        // Size the wrapper, not the vanilla ButtonComponent: the wrapper is the owo layout child.
        var pickWrapper = UIComponents.wrapVanillaWidget(pick);
        pickWrapper.horizontalSizing(Sizing.fixed(86));
        pickButtons.put(target, pick);

        TextBoxComponent hex = UIComponents.textBox(Sizing.fixed(62), HexColor.format(colors.rgb(target)));
        hex.setMaxLength(7);
        hex.onChanged().subscribe(text -> onHexTyped(target, text));
        hexBoxes.put(target, hex);

        BoxComponent swatch = UIComponents.box(Sizing.fixed(16), Sizing.fixed(16));
        swatch.fill(true);
        swatch.color(Color.ofRgb(colors.rgb(target)));
        swatches.put(target, swatch);

        row.child(pickWrapper);
        row.child(UIComponents.wrapVanillaWidget(hex));
        row.child(swatch);
        return row;
    }

    private String pickLabel(ColorTarget target) {
        return colors.selected() == target ? "[" + target.label + "]" : target.label;
    }

    private void onHexTyped(ColorTarget target, String text) {
        ColorSyncModel.HexResult result = colors.applyHex(target, text);
        hexBoxes.get(target).setTextColor(result == ColorSyncModel.HexResult.INVALID ? TEXT_INVALID : TEXT_VALID);
    }

    /** Wires the model's change notifications to the widgets and to the live config. */
    private void installColourSync() {
        colors.setListener(new ColorSyncModel.Listener() {
            @Override
            public void colorChanged(ColorTarget target, int rgb, ColorSyncModel.Source source) {
                config.setColor(target, rgb);
                swatches.get(target).color(Color.ofRgb(rgb));
                if (source != ColorSyncModel.Source.HEX_TYPED) {
                    hexBoxes.get(target).text(HexColor.format(rgb)); // leave the user's own typing alone
                }
                if (source != ColorSyncModel.Source.PICKER && target == colors.selected()) {
                    picker.selectedColor(Color.ofRgb(rgb));
                }
            }

            @Override
            public void selectionChanged(ColorTarget selected) {
                for (ColorTarget t : ColorTarget.values()) {
                    pickButtons.get(t).setMessage(Component.literal(pickLabel(t)));
                }
                picker.selectedColor(Color.ofRgb(colors.rgb(selected)));
            }
        });
    }

    // ---------------------------------------------------------------- building blocks

    private LabelComponent heading(String text) {
        LabelComponent label = UIComponents.label(Component.literal(text));
        label.color(Color.ofArgb(ACCENT));
        label.shadow(true);
        return label;
    }

    private LabelComponent caption(String text) {
        LabelComponent label = UIComponents.label(Component.literal(text));
        label.color(Color.ofArgb(MUTED));
        label.maxWidth(PANEL_WIDTH - 40);
        return label;
    }

    // Wrap vanilla widgets before adding them to owo layouts.
    private UIComponent checkbox(String text, boolean value, Consumer<Boolean> onChanged) {
        CheckboxComponent box = UIComponents.checkbox(Component.literal(text));
        box.checked(value);
        box.onChanged(onChanged);
        return UIComponents.wrapVanillaWidget(box);
    }

    private UIComponent radiusSlider() {
        DiscreteSliderComponent slider =
                UIComponents.discreteSlider(Sizing.fill(100), EspMath.MIN_RADIUS, EspMath.MAX_RADIUS);
        slider.setFromDiscreteValue(config.espRadius);
        slider.message(value -> Component.literal("Radius: " + value + " blocks"));
        slider.onChanged().subscribe(value -> config.espRadius = EspMath.clampRadius((int) Math.round(value)));
        return UIComponents.wrapVanillaWidget(slider);
    }

    private BoxComponent divider() {
        BoxComponent box = UIComponents.box(Sizing.fill(100), Sizing.fixed(1));
        box.fill(true);
        box.color(Color.ofArgb(DIVIDER));
        return box;
    }

    private FlowLayout footer() {
        FlowLayout footer = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        footer.horizontalAlignment(HorizontalAlignment.RIGHT);
        ButtonComponent doneButton = UIComponents.button(Component.literal("Done"), button -> this.onClose());
        footer.child(UIComponents.wrapVanillaWidget(doneButton));
        return footer;
    }
}
