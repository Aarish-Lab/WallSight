package com.example.wallsight.client.logic;

import java.util.function.ToIntFunction;

/**
 * The single source of truth that keeps the HEX text boxes, the swatches and the colour picker in
 * step. The widgets never talk to each other: each reports into this model, and the model tells the
 * listener what changed and where the change came from, so the UI only refreshes the other widgets.
 * Every update is de-duplicated by value, which is what stops echo loops (a widget being refreshed
 * reports the same value back, which changes nothing and ends the chain).
 */
public final class ColorSyncModel {

    public enum Source { HEX_TYPED, PICKER, RESET }

    public enum HexResult { APPLIED, UNCHANGED, INVALID }

    public interface Listener {
        void colorChanged(ColorTarget target, int rgb, Source source);

        void selectionChanged(ColorTarget selected);
    }

    private final int[] rgb = new int[ColorTarget.values().length];
    private ColorTarget selected = ColorTarget.PLAYERS;
    private Listener listener;

    public ColorSyncModel(ToIntFunction<ColorTarget> initial) {
        for (ColorTarget t : ColorTarget.values()) {
            rgb[t.ordinal()] = initial.applyAsInt(t) & 0xFFFFFF;
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public int rgb(ColorTarget target) {
        return rgb[target.ordinal()];
    }

    public ColorTarget selected() {
        return selected;
    }

    public void select(ColorTarget target) {
        if (target == selected) {
            return;
        }
        selected = target;
        if (listener != null) {
            listener.selectionChanged(target);
        }
    }

    /** Text typed into a HEX box. Invalid text changes nothing (the last valid colour is kept). */
    public HexResult applyHex(ColorTarget target, String text) {
        int value = HexColor.parse(text);
        if (value == HexColor.INVALID) {
            return HexResult.INVALID;
        }
        return set(target, value, Source.HEX_TYPED) ? HexResult.APPLIED : HexResult.UNCHANGED;
    }

    /** A colour chosen in the picker; it always edits the currently selected target. */
    public boolean applyPicker(int pickedRgb) {
        return set(selected, pickedRgb & 0xFFFFFF, Source.PICKER);
    }

    public void reset(ToIntFunction<ColorTarget> defaults) {
        for (ColorTarget t : ColorTarget.values()) {
            set(t, defaults.applyAsInt(t) & 0xFFFFFF, Source.RESET);
        }
    }

    private boolean set(ColorTarget target, int value, Source source) {
        if (rgb[target.ordinal()] == value) {
            return false;
        }
        rgb[target.ordinal()] = value;
        if (listener != null) {
            listener.colorChanged(target, value, source);
        }
        return true;
    }
}
