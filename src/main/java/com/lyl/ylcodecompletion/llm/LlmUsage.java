package com.lyl.ylcodecompletion.llm;

/**
 * Token usage returned by DeepSeek's FIM completion API.
 */
public record LlmUsage(
        long promptTokens,
        long completionTokens,
        long totalTokens,
        long promptCacheHitTokens,
        long promptCacheMissTokens
) {
    public long effectiveTotalTokens() {
        if (totalTokens > 0) {
            return totalTokens;
        }
        return promptTokens + completionTokens;
    }

    public long billablePromptMissTokens() {
        long knownPromptTokens = promptCacheHitTokens + promptCacheMissTokens;
        long uncategorizedPromptTokens = Math.max(0, promptTokens - knownPromptTokens);
        return promptCacheMissTokens + uncategorizedPromptTokens;
    }
}
