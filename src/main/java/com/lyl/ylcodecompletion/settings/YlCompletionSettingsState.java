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

    public boolean enabled = true;
    public String baseUrl = "https://api.deepseek.com/beta";
    public String model = "deepseek-v4-pro";
    public int maxTokens = 128;
    public double temperature = 0.2;
    public double topP = 0.95;
    public int debounceMs = 300;
    public int timeoutMs = 8000;
    public int contextMaxChars = 4000;
    public int triggerMinPrefixLength = 1;
    public String disabledExtensions = "";
    /**
     * 标记 keychain 里是否存有 API key，用于 UI 显示 "&lt;stored&gt;" 占位，
     * 避免每次打开设置页都去读 keychain 弹密码框。
     */
    public boolean hasApiKey = false;

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
