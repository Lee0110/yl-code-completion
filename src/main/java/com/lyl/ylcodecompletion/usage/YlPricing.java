package com.lyl.ylcodecompletion.usage;

import com.lyl.ylcodecompletion.llm.LlmUsage;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class YlPricing {

    private static final double MILLION = 1_000_000.0;

    private YlPricing() {
    }

    public static double estimateCny(@NotNull String model, @NotNull LlmUsage usage) {
        ModelPrice price = priceFor(model);
        return usage.promptCacheHitTokens() / MILLION * price.inputCacheHitPerMillionCny()
                + usage.billablePromptMissTokens() / MILLION * price.inputCacheMissPerMillionCny()
                + usage.completionTokens() / MILLION * price.outputPerMillionCny();
    }

    public static double estimateCny(
            @NotNull String model,
            long promptCacheHitTokens,
            long promptCacheMissTokens,
            long promptTokens,
            long completionTokens
    ) {
        return estimateCny(model, new LlmUsage(
                promptTokens,
                completionTokens,
                promptTokens + completionTokens,
                promptCacheHitTokens,
                promptCacheMissTokens
        ));
    }

    private static @NotNull ModelPrice priceFor(@NotNull String model) {
        String normalized = model.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "deepseek-v4-flash", "deepseek-chat" -> new ModelPrice(0.02, 1.0, 2.0);
            case "deepseek-v4-pro", "deepseek-reasoner" -> new ModelPrice(0.025, 3.0, 6.0);
            default -> ModelPrice.ZERO;
        };
    }

    private record ModelPrice(
            double inputCacheHitPerMillionCny,
            double inputCacheMissPerMillionCny,
            double outputPerMillionCny
    ) {
        private static final ModelPrice ZERO = new ModelPrice(0.0, 0.0, 0.0);
    }
}
