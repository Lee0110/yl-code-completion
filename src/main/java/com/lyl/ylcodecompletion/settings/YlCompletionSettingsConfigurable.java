package com.lyl.ylcodecompletion.settings;

import com.intellij.openapi.options.Configurable;
import com.lyl.ylcodecompletion.YlMessageBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

public final class YlCompletionSettingsConfigurable implements Configurable {

    private YlCompletionSettingsComponent component;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return YlMessageBundle.message("settings.displayName");
    }

    @Override
    public @Nullable JComponent createComponent() {
        component = new YlCompletionSettingsComponent();
        component.load(YlCompletionSettingsState.getInstance());
        return component.getPanel();
    }

    @Override
    public boolean isModified() {
        if (component == null) return false;
        return component.isModified(YlCompletionSettingsState.getInstance());
    }

    @Override
    public void apply() {
        if (component == null) return;
        YlCompletionSettingsState state = YlCompletionSettingsState.getInstance();
        component.apply(state);
    }

    @Override
    public void reset() {
        if (component == null) return;
        component.load(YlCompletionSettingsState.getInstance());
    }

    @Override
    public void disposeUIResources() {
        component = null;
    }
}
