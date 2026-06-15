package com.lyl.ylcodecompletion.llm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record LlmRequest(
        @NotNull String model,
        @NotNull String prompt,
        @Nullable String suffix,
        int maxTokens,
        double temperature,
        double topP,
        @Nullable List<String> stop
) {
}
