package com.nanolaba.nrg.widgets;

/**
 * Convenience base class for {@link NRGWidget} implementations.
 *
 * <p>Adds a mutable {@code enabled} flag (default {@code true}) so widgets can be temporarily
 * suppressed — used internally by {@code TableOfContentsWidget} when running its secondary
 * generator pass — and a {@link #toString()} that returns the widget's name for friendlier
 * debug logs. Subclasses still implement {@link #getName()} and {@link #getBody}.
 */
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
