package com.knowbase.library.service;

import com.knowbase.library.config.VersionPolicySettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VersionUpdateStrategyTest {

    @Test
    void disabledPolicyUsesOverwrite() {
        VersionPolicySettings policy = new VersionPolicySettings();
        policy.setEnabled(false);
        policy.setUpdateStrategy("keep-history");
        assertEquals(VersionUpdateStrategy.OVERWRITE, VersionUpdateStrategy.from(policy));
    }

    @Test
    void mapsConfiguredStrategies() {
        VersionPolicySettings policy = new VersionPolicySettings();
        policy.setEnabled(true);

        policy.setUpdateStrategy("overwrite");
        assertEquals(VersionUpdateStrategy.OVERWRITE, VersionUpdateStrategy.from(policy));

        policy.setUpdateStrategy("incremental");
        assertEquals(VersionUpdateStrategy.INCREMENTAL, VersionUpdateStrategy.from(policy));

        policy.setUpdateStrategy("keep-history");
        assertEquals(VersionUpdateStrategy.KEEP_HISTORY, VersionUpdateStrategy.from(policy));
    }
}
