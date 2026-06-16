package com.lyl.ylcodecompletion.status;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.ui.AnimatedIcon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.util.Consumer;
import com.intellij.util.messages.MessageBusConnection;
import com.lyl.ylcodecompletion.YlMessageBundle;
import com.lyl.ylcodecompletion.settings.YlCompletionSettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.Icon;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;

public final class YlStatusBarWidget implements StatusBarWidget, StatusBarWidget.IconPresentation {

    public static final String ID = "YlCodeCompletion.StatusBar";
    private static final int POPUP_GAP_PX = 8;
    private static final int POPUP_RIGHT_ALIGN_OFFSET_PX = 16;

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
        if (!YlCompletionSettingsState.getInstance().enabled) {
            return AllIcons.Actions.IntentionBulbGrey;
        }
        YlBusyState.Status s = YlBusyState.getInstance().currentStatus();
        return switch (s) {
            case LOADING -> AnimatedIcon.Default.INSTANCE;
            case ERROR -> AllIcons.General.BalloonError;
            case IDLE -> AllIcons.Actions.IntentionBulbGrey;
        };
    }

    @Override
    public @Nullable String getTooltipText() {
        if (!YlCompletionSettingsState.getInstance().enabled) {
            return YlMessageBundle.message("statusbar.tooltip.disabled");
        }
        YlBusyState.Status s = YlBusyState.getInstance().currentStatus();
        return YlMessageBundle.message("statusbar.tooltip." + s.name().toLowerCase());
    }

    @Override
    public @Nullable Consumer<MouseEvent> getClickConsumer() {
        return this::showPopup;
    }

    private void showPopup(@NotNull MouseEvent event) {
        JComponent source = event.getComponent() instanceof JComponent component ? component : null;
        JComponent panel = new YlStatusPopupPanel(project, this::updateUi).createPanel();
        var popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, panel)
                .setRequestFocus(false)
                .setResizable(false)
                .setMovable(true)
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)
                .createPopup();
        if (source != null) {
            popup.showInScreenCoordinates(source, popupLocationAboveClick(event, panel));
            popup.moveToFitScreen();
        } else {
            popup.showCenteredInCurrentWindow(project);
        }
    }

    private static @NotNull Point popupLocationAboveClick(
            @NotNull MouseEvent event,
            @NotNull JComponent panel
    ) {
        Dimension popupSize = panel.getPreferredSize();
        int x = event.getXOnScreen() - popupSize.width + POPUP_RIGHT_ALIGN_OFFSET_PX;
        int y = event.getYOnScreen() - popupSize.height - POPUP_GAP_PX;
        return new Point(x, y);
    }

    private void updateUi() {
        if (statusBar != null) {
            statusBar.updateWidget(ID);
        }
    }
}
