package com.lyl.ylcodecompletion.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "YlCompletionSettings",
        storages = @Storage("ylCodeCompletion.xml")
)
public final class YlCompletionSettingsState implements PersistentStateComponent<YlCompletionSettingsState> {

    public static final String DEFAULT_BASE_URL = "https://api.deepseek.com/beta";
    public static final String DEFAULT_MODEL = "deepseek-v4-flash";
    public static final int DEFAULT_MAX_TOKENS = 64;
    public static final double DEFAULT_TEMPERATURE = 0.15;
    public static final double DEFAULT_TOP_P = 0.90;
    public static final int DEFAULT_DEBOUNCE_MS = 150;
    public static final int DEFAULT_TIMEOUT_MS = 5000;
    public static final int DEFAULT_CONTEXT_MAX_CHARS = 4000;
    public static final int DEFAULT_TRIGGER_MIN_PREFIX_LENGTH = 2;

    public boolean enabled = true;
    public String baseUrl = DEFAULT_BASE_URL;
    public String apiKey = "";
    public String model = DEFAULT_MODEL;
    public int maxTokens = DEFAULT_MAX_TOKENS;
    public double temperature = DEFAULT_TEMPERATURE;
    public double topP = DEFAULT_TOP_P;
    public int debounceMs = DEFAULT_DEBOUNCE_MS;
    public int timeoutMs = DEFAULT_TIMEOUT_MS;
    public int contextMaxChars = DEFAULT_CONTEXT_MAX_CHARS;
    public int triggerMinPrefixLength = DEFAULT_TRIGGER_MIN_PREFIX_LENGTH;
    public String disabledExtensions = "";

    public static YlCompletionSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(YlCompletionSettingsState.class);
    }

    @Override
    public @Nullable YlCompletionSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull YlCompletionSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
