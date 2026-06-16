package com.lyl.ylcodecompletion.status;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.lyl.ylcodecompletion.YlMessageBundle;
import com.lyl.ylcodecompletion.settings.YlCompletionSettingsConfigurable;
import com.lyl.ylcodecompletion.settings.YlCompletionSettingsState;
import com.lyl.ylcodecompletion.usage.YlUsageStatsState;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

final class YlStatusPopupPanel {

    private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("'CNY '0.00");

    private final Project project;
    private final Runnable onSettingsChanged;

    YlStatusPopupPanel(@NotNull Project project, @NotNull Runnable onSettingsChanged) {
        this.project = project;
        this.onSettingsChanged = onSettingsChanged;
    }

    @NotNull JPanel createPanel() {
        YlCompletionSettingsState settings = YlCompletionSettingsState.getInstance();
        YlUsageStatsState.UsageSnapshot usage = YlUsageStatsState.getInstance().snapshot();

        JBCheckBox enabledBox = new JBCheckBox(YlMessageBundle.message("statusbar.popup.enabled"));
        enabledBox.setSelected(settings.enabled);
        enabledBox.addActionListener(e -> {
            settings.enabled = enabledBox.isSelected();
            onSettingsChanged.run();
        });

        FormBuilder builder = FormBuilder.createFormBuilder()
                .addComponent(enabledBox)
                .addSeparator()
                .addLabeledComponent(
                        new JBLabel(YlMessageBundle.message("statusbar.popup.month")),
                        new JBLabel(usage.month())
                )
                .addLabeledComponent(
                        new JBLabel(YlMessageBundle.message("statusbar.popup.requests")),
                        new JBLabel(INTEGER_FORMAT.format(usage.requestCount()))
                )
                .addLabeledComponent(
                        new JBLabel(YlMessageBundle.message("statusbar.popup.tokens")),
                        new JBLabel(INTEGER_FORMAT.format(usage.totalTokens()))
                )
                .addLabeledComponent(
                        new JBLabel(YlMessageBundle.message("statusbar.popup.cost")),
                        new JBLabel(MONEY_FORMAT.format(usage.estimatedCostCny()))
                );

        if (!usage.models().isEmpty()) {
            builder.addSeparator();
            for (YlUsageStatsState.ModelSnapshot model : usage.models()) {
                builder.addLabeledComponent(new JBLabel(model.model()), new JBLabel(modelSummary(model)));
            }
        }

        JButton settingsButton = new JButton(YlMessageBundle.message("statusbar.popup.openSettings"));
        settingsButton.addActionListener(e -> ShowSettingsUtil.getInstance().showSettingsDialog(
                project, YlCompletionSettingsConfigurable.class));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonRow.add(settingsButton);

        JPanel content = builder
                .addSeparator()
                .addComponent(buttonRow)
                .getPanel();

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(JBUI.Borders.empty(12));
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private static @NotNull String modelSummary(@NotNull YlUsageStatsState.ModelSnapshot model) {
        return YlMessageBundle.message(
                "statusbar.popup.modelSummary",
                INTEGER_FORMAT.format(model.requestCount()),
                INTEGER_FORMAT.format(model.promptTokens()),
                INTEGER_FORMAT.format(model.completionTokens()),
                MONEY_FORMAT.format(model.estimatedCostCny())
        );
    }
}
