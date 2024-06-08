package com.nanolaba.nrg.widgets;

public abstract class DefaultWidget implements NRGWidget {

    @Override
    public String toString() {
        return getName();
    }
}
