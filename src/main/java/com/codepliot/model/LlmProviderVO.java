package com.codepliot.model;

import java.util.List;

public record LlmProviderVO(
        String provider,
        String displayName,
        String defaultBaseUrl,
        List<LlmAvailableModelVO> defaultModels,
        Boolean supportsTools
) {
}
