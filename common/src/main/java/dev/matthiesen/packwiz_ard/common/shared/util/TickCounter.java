/**
 * Code adapted from "Packwiz Modpack Loader" by EvieTheOwl
 * Original Repository: https://git.gay/EvieTheOwl/packwiz-modpack-loader
 * Licensed under the MIT License.
 * Copyright (c) 2023 The Tiny Taters
 * Modified Copyright (c) 2026 Adam Matthiesen (Matthiesen-dev)
 */
package dev.matthiesen.packwiz_ard.common.shared.util;

public final class TickCounter {

    private final int tickThreshold;
    private final int maxValue;

    private int counter = 0;

    public TickCounter(int tickThreshold) {
        this(tickThreshold, Integer.MAX_VALUE);
    }

    public TickCounter(int tickThreshold, int maxValue) {
        this.tickThreshold = tickThreshold;
        this.maxValue = maxValue;
    }

    public boolean test() {
        if(counter >= tickThreshold) {
            reset();
            return true;
        }
        return false;
    }

    public void increment() {
        if(counter >= maxValue || counter < 0) reset();
        else counter++;
    }

    public void reset() {
        counter = 0;
    }
}
