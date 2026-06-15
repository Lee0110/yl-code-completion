package com.lyl.ylcodecompletion.status;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.lyl.ylcodecompletion.YlMessageBundle;
import org.jetbrains.annotations.NotNull;

public final class YlStatusBarWidgetFactory implements StatusBarWidgetFactory {

    @Override
    public @NotNull String getId() {
        return YlStatusBarWidget.ID;
    }

    @Override
    public @NotNull String getDisplayName() {
        return YlMessageBundle.message("statusbar.displayName");
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return true;
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new YlStatusBarWidget(project);
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }
}
