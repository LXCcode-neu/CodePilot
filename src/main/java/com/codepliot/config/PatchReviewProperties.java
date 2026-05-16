package com.codepliot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "codepilot.patch-review")
public class PatchReviewProperties {

    private boolean enabled = true;
    private int minScore = 70;
    private boolean failOnHighRisk = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMinScore() {
        return minScore;
    }

    public void setMinScore(int minScore) {
        this.minScore = minScore;
    }

    public boolean isFailOnHighRisk() {
        return failOnHighRisk;
    }

    public void setFailOnHighRisk(boolean failOnHighRisk) {
        this.failOnHighRisk = failOnHighRisk;
    }
}
