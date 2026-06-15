package com.lyl.ylcodecompletion.llm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record LlmResponse(
        @NotNull String text,
        @Nullable String finishReason
) {
}
