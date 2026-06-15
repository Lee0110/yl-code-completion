package com.lyl.ylcodecompletion.status;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.ui.AnimatedIcon;
import com.intellij.util.Consumer;
import com.intellij.util.messages.MessageBusConnection;
import com.lyl.ylcodecompletion.YlMessageBundle;
import com.lyl.ylcodecompletion.settings.YlCompletionSettingsConfigurable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.event.MouseEvent;

public final class YlStatusBarWidget implements StatusBarWidget, StatusBarWidget.IconPresentation {

    public static final String ID = "YlCodeCompletion.StatusBar";

    private final Project project;
    private StatusBar statusBar;
    private MessageBusConnection connection;

    public YlStatusBarWidget(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @NotNull String ID() {
        return ID;
    }

    @Override
    public @Nullable WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        this.statusBar = statusBar;
        this.connection = ApplicationManager.getApplication()
                .getMessageBus().connect(this);
        connection.subscribe(YlBusyState.TOPIC, (YlBusyState.Listener) status -> updateUi());
        updateUi();
    }

    @Override
    public void dispose() {
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
        statusBar = null;
    }

    @Override
    public @Nullable Icon getIcon() {
        YlBusyState.Status s = YlBusyState.getInstance().currentStatus();
        return switch (s) {
            case LOADING -> AnimatedIcon.Default.INSTANCE;
            case ERROR -> AllIcons.General.BalloonError;
            case IDLE -> AllIcons.Actions.IntentionBulbGrey;
        };
    }

    @Override
    public @Nullable String getTooltipText() {
        YlBusyState.Status s = YlBusyState.getInstance().currentStatus();
        return YlMessageBundle.message("statusbar.tooltip." + s.name().toLowerCase());
    }

    @Override
    public @Nullable Consumer<MouseEvent> getClickConsumer() {
        return event -> ShowSettingsUtil.getInstance().showSettingsDialog(
                project, YlCompletionSettingsConfigurable.class);
    }

    private void updateUi() {
        if (statusBar != null) {
            statusBar.updateWidget(ID);
        }
    }
}
