package com.lyl.ylcodecompletion.completion;

import org.jetbrains.annotations.NotNull;

public record YlCompletionContext(
        @NotNull String filePath,
        @NotNull String languageId,
        @NotNull String prefix,
        @NotNull String suffix,
        int caretOffset
) {
}
