package com.knowbase.library.config;

public class VersionPolicySettings {

    private boolean enabled = true;
    /** overwrite | incremental | keep-history */
    private String updateStrategy = "keep-history";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUpdateStrategy() {
        return updateStrategy;
    }

    public void setUpdateStrategy(String updateStrategy) {
        this.updateStrategy = updateStrategy;
    }
}
