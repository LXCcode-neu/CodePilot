package com.codepliot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 补丁评审配置属性。
 * <p>
 * 绑定 {@code codepilot.patch-review} 前缀下的配置项，控制 AI 补丁评审的启用状态、
 * 最低分数阈值及高风险处理策略。
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "codepilot.patch-review")
public class PatchReviewProperties {

    /** 是否启用补丁评审功能，默认开启 */
    private boolean enabled = true;
    /** 补丁评审最低通过分数（满分 100），低于此分数将被拒绝 */
    private int minScore = 70;
    /** 是否在评审结果为高风险时直接失败，默认开启 */
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
