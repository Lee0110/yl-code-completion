package com.lyl.ylcodecompletion.completion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SuffixDeduplicator {

    private static final int MAX_COMPARE = 200;

    private SuffixDeduplicator() {
    }

    /**
     * 去除 modelText 中与 documentSuffix 起始部分重复的尾巴。
     * 例如 modelText = "ne;\n}"，documentSuffix = ";\n}..."，
     * 最大公共后缀-前缀长度为 3，结果返回 "ne"。
     * 若 modelText 整体是 documentSuffix 的前缀，返回 null（建议丢弃）。
     */
    public static @Nullable String dedup(@NotNull String modelText, @NotNull String documentSuffix) {
        if (modelText.isEmpty()) return null;
        if (documentSuffix.isEmpty()) return modelText;

        String suffixHead = documentSuffix.length() > MAX_COMPARE
                ? documentSuffix.substring(0, MAX_COMPARE)
                : documentSuffix;

        // 整段已存在
        if (suffixHead.startsWith(modelText)) {
            return null;
        }

        int max = Math.min(modelText.length(), suffixHead.length());
        for (int k = max; k > 0; k--) {
            if (modelText.regionMatches(modelText.length() - k, suffixHead, 0, k)) {
                String trimmed = modelText.substring(0, modelText.length() - k);
                return trimmed.isEmpty() ? null : trimmed;
            }
        }
        return modelText;
    }
}
