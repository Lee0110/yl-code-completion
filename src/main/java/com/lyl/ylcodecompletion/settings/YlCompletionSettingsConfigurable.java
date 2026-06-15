package com.lyl.ylcodecompletion.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.Configurable;
import com.lyl.ylcodecompletion.YlMessageBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

public final class YlCompletionSettingsConfigurable implements Configurable {

    private YlCompletionSettingsComponent component;
    /**
     * 内存中缓存的"已保存到 keychain 的 api key"，用于 isModified 比较。
     * 第一次 createComponent / reset 时是 null（异步加载中），
     * 加载完成后会被回填，apply 后也同步更新。
     */
    private volatile String storedApiKey;
    private volatile boolean storedApiKeyLoaded;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return YlMessageBundle.message("settings.displayName");
    }

    @Override
    public @Nullable JComponent createComponent() {
        component = new YlCompletionSettingsComponent();
        component.load(YlCompletionSettingsState.getInstance(), null);
        loadApiKeyIntoUiAsync();
        return component.getPanel();
    }

    @Override
    public boolean isModified() {
        if (component == null) return false;
        // apiKey 还没加载完时，不把"用户没动"的输入框视为已修改
        if (!storedApiKeyLoaded && component.getApiKey().isEmpty()) {
            return component.isModifiedExceptApiKey(YlCompletionSettingsState.getInstance());
        }
        return component.isModified(YlCompletionSettingsState.getInstance(), storedApiKey);
    }

    @Override
    public void apply() {
        if (component == null) return;
        component.apply(YlCompletionSettingsState.getInstance());
        String newKey = component.getApiKey();
        String toStore = newKey.isEmpty() ? null : newKey;
        storedApiKey = toStore;
        storedApiKeyLoaded = true;
        YlPasswordSafe.storeApiKeyAsync(toStore);
    }

    @Override
    public void reset() {
        if (component == null) return;
        component.load(YlCompletionSettingsState.getInstance(), null);
        storedApiKeyLoaded = false;
        loadApiKeyIntoUiAsync();
    }

    @Override
    public void disposeUIResources() {
        component = null;
        storedApiKey = null;
        storedApiKeyLoaded = false;
    }

    private void loadApiKeyIntoUiAsync() {
        YlCompletionSettingsComponent target = component;
        ModalityState modality = ModalityState.stateForComponent(target.getPanel());
        YlPasswordSafe.loadApiKeyAsync().whenComplete((key, err) ->
                ApplicationManager.getApplication().invokeLater(() -> {
                    storedApiKey = key;
                    storedApiKeyLoaded = true;
                    if (component == target) {
                        component.setApiKey(key == null ? "" : key);
                    }
                }, modality));
    }
}
