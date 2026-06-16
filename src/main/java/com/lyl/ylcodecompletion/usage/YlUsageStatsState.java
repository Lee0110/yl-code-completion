package com.lyl.ylcodecompletion.usage;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.lyl.ylcodecompletion.llm.LlmUsage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@State(
        name = "YlCompletionUsageStats",
        storages = @Storage("ylCodeCompletionUsage.xml")
)
public final class YlUsageStatsState implements PersistentStateComponent<YlUsageStatsState.UsageState> {

    private final UsageState state = new UsageState();

    public static YlUsageStatsState getInstance() {
        return ApplicationManager.getApplication().getService(YlUsageStatsState.class);
    }

    @Override
    public synchronized @Nullable UsageState getState() {
        ensureCurrentMonth();
        return state;
    }

    @Override
    public synchronized void loadState(@NotNull UsageState loadedState) {
        XmlSerializerUtil.copyBean(loadedState, state);
        ensureCurrentMonth();
    }

    public synchronized void recordCompletion(@NotNull String model, @Nullable LlmUsage usage) {
        ensureCurrentMonth();
        ModelUsage bucket = bucketFor(model);
        bucket.requestCount++;
        if (usage == null) {
            return;
        }
        bucket.promptTokens += usage.promptTokens();
        bucket.completionTokens += usage.completionTokens();
        bucket.totalTokens += usage.effectiveTotalTokens();
        bucket.promptCacheHitTokens += usage.promptCacheHitTokens();
        bucket.promptCacheMissTokens += usage.billablePromptMissTokens();
    }

    public synchronized @NotNull UsageSnapshot snapshot() {
        ensureCurrentMonth();
        List<ModelSnapshot> models = state.models.stream()
                .sorted(Comparator.comparing(m -> m.model))
                .map(m -> new ModelSnapshot(
                        m.model,
                        m.requestCount,
                        m.promptTokens,
                        m.completionTokens,
                        m.totalTokens,
                        m.promptCacheHitTokens,
                        m.promptCacheMissTokens,
                        YlPricing.estimateCny(
                                m.model,
                                m.promptCacheHitTokens,
                                m.promptCacheMissTokens,
                                m.promptTokens,
                                m.completionTokens
                        )
                ))
                .toList();

        long requestCount = 0;
        long totalTokens = 0;
        double estimatedCostCny = 0.0;
        for (ModelSnapshot model : models) {
            requestCount += model.requestCount();
            totalTokens += model.totalTokens();
            estimatedCostCny += model.estimatedCostCny();
        }

        return new UsageSnapshot(state.month, requestCount, totalTokens, estimatedCostCny, models);
    }

    private void ensureCurrentMonth() {
        String currentMonth = YearMonth.now().toString();
        if (state.models == null) {
            state.models = new ArrayList<>();
        }
        if (state.month == null || state.month.isBlank() || !currentMonth.equals(state.month)) {
            state.month = currentMonth;
            state.models.clear();
        }
    }

    private @NotNull ModelUsage bucketFor(@NotNull String model) {
        String normalizedModel = model.isBlank() ? "unknown" : model.trim();
        for (ModelUsage usage : state.models) {
            if (normalizedModel.equals(usage.model)) {
                return usage;
            }
        }
        ModelUsage usage = new ModelUsage();
        usage.model = normalizedModel;
        state.models.add(usage);
        return usage;
    }

    public static final class UsageState {
        public String month = YearMonth.now().toString();
        public List<ModelUsage> models = new ArrayList<>();
    }

    public static final class ModelUsage {
        public String model = "";
        public long requestCount = 0;
        public long promptTokens = 0;
        public long completionTokens = 0;
        public long totalTokens = 0;
        public long promptCacheHitTokens = 0;
        public long promptCacheMissTokens = 0;
    }

    public record UsageSnapshot(
            @NotNull String month,
            long requestCount,
            long totalTokens,
            double estimatedCostCny,
            @NotNull List<ModelSnapshot> models
    ) {
    }

    public record ModelSnapshot(
            @NotNull String model,
            long requestCount,
            long promptTokens,
            long completionTokens,
            long totalTokens,
            long promptCacheHitTokens,
            long promptCacheMissTokens,
            double estimatedCostCny
    ) {
    }
}
