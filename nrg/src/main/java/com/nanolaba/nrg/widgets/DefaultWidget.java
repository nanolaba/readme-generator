package com.nanolaba.nrg.widgets;

public abstract class DefaultWidget implements NRGWidget {

    private boolean enabled = true;

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
