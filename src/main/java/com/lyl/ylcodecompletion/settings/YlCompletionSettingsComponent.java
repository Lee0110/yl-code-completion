package com.lyl.ylcodecompletion.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.lyl.ylcodecompletion.YlMessageBundle;
import com.lyl.ylcodecompletion.llm.DeepSeekFimClient;
import com.lyl.ylcodecompletion.llm.LlmException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.concurrent.CompletionException;

public final class YlCompletionSettingsComponent {

    private final JPanel root;

    private final JBCheckBox enabledBox = new JBCheckBox(YlMessageBundle.message("settings.field.enabled"));
    private final JBTextField baseUrlField = new JBTextField();
    private final JBPasswordField apiKeyField = new JBPasswordField();
    private final JBTextField modelField = new JBTextField();
    private final JBIntSpinner maxTokensSpinner = new JBIntSpinner(
            YlCompletionSettingsState.DEFAULT_MAX_TOKENS, 1, 4096);
    private final JSpinner temperatureSpinner =
            new JSpinner(new SpinnerNumberModel(
                    YlCompletionSettingsState.DEFAULT_TEMPERATURE, 0.0, 2.0, 0.05));
    private final JSpinner topPSpinner =
            new JSpinner(new SpinnerNumberModel(
                    YlCompletionSettingsState.DEFAULT_TOP_P, 0.0, 1.0, 0.05));
    private final JBIntSpinner debounceMsSpinner = new JBIntSpinner(
            YlCompletionSettingsState.DEFAULT_DEBOUNCE_MS, 0, 10_000);
    private final JBIntSpinner timeoutMsSpinner = new JBIntSpinner(
            YlCompletionSettingsState.DEFAULT_TIMEOUT_MS, 1000, 60_000);
    private final JBIntSpinner contextMaxCharsSpinner = new JBIntSpinner(
            YlCompletionSettingsState.DEFAULT_CONTEXT_MAX_CHARS, 200, 200_000);
    private final JBIntSpinner triggerMinPrefixSpinner = new JBIntSpinner(
            YlCompletionSettingsState.DEFAULT_TRIGGER_MIN_PREFIX_LENGTH, 0, 100);
    private final JBTextField disabledExtensionsField = new JBTextField();

    private final JButton testConnectionButton = new JButton(
            YlMessageBundle.message("settings.button.testConnection"));
    private final JButton resetButton = new JButton(YlMessageBundle.message("settings.button.reset"));
    private final JLabel testStatusLabel = new JLabel(" ");

    public YlCompletionSettingsComponent() {
        baseUrlField.getEmptyText().setText(YlCompletionSettingsState.DEFAULT_BASE_URL);
        apiKeyField.getEmptyText().setText(YlMessageBundle.message("settings.field.apiKey.emptyPlaceholder"));
        modelField.getEmptyText().setText(YlCompletionSettingsState.DEFAULT_MODEL);
        installTooltips();

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonRow.add(testConnectionButton);
        buttonRow.add(resetButton);
        buttonRow.add(testStatusLabel);

        JPanel form = FormBuilder.createFormBuilder()
                .addComponent(enabledBox)
                .addSeparator()
                .addLabeledComponent(label("settings.field.baseUrl", "settings.tooltip.baseUrl"), baseUrlField)
                .addLabeledComponent(label("settings.field.apiKey", "settings.tooltip.apiKey"), apiKeyField)
                .addLabeledComponent(label("settings.field.model", "settings.tooltip.model"), modelField)
                .addSeparator()
                .addLabeledComponent(label("settings.field.maxTokens", "settings.tooltip.maxTokens"), maxTokensSpinner)
                .addLabeledComponent(label("settings.field.temperature", "settings.tooltip.temperature"), temperatureSpinner)
                .addLabeledComponent(label("settings.field.topP", "settings.tooltip.topP"), topPSpinner)
                .addLabeledComponent(label("settings.field.debounceMs", "settings.tooltip.debounceMs"), debounceMsSpinner)
                .addLabeledComponent(label("settings.field.timeoutMs", "settings.tooltip.timeoutMs"), timeoutMsSpinner)
                .addLabeledComponent(label("settings.field.contextMaxChars", "settings.tooltip.contextMaxChars"), contextMaxCharsSpinner)
                .addLabeledComponent(label("settings.field.triggerMinPrefixLength", "settings.tooltip.triggerMinPrefixLength"), triggerMinPrefixSpinner)
                .addLabeledComponent(label("settings.field.disabledExtensions", "settings.tooltip.disabledExtensions"), disabledExtensionsField)
                .addSeparator()
                .addComponent(buttonRow)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        root = new JPanel(new BorderLayout());
        root.setBorder(JBUI.Borders.empty(12));
        root.add(form, BorderLayout.NORTH);

        testConnectionButton.addActionListener(e -> testConnection());
        resetButton.addActionListener(e -> resetToDefaults());
    }

    private void installTooltips() {
        tooltip(enabledBox, "settings.tooltip.enabled");
        tooltip(baseUrlField, "settings.tooltip.baseUrl");
        tooltip(apiKeyField, "settings.tooltip.apiKey");
        tooltip(modelField, "settings.tooltip.model");
        tooltip(maxTokensSpinner, "settings.tooltip.maxTokens");
        tooltip(temperatureSpinner, "settings.tooltip.temperature");
        tooltip(topPSpinner, "settings.tooltip.topP");
        tooltip(debounceMsSpinner, "settings.tooltip.debounceMs");
        tooltip(timeoutMsSpinner, "settings.tooltip.timeoutMs");
        tooltip(contextMaxCharsSpinner, "settings.tooltip.contextMaxChars");
        tooltip(triggerMinPrefixSpinner, "settings.tooltip.triggerMinPrefixLength");
        tooltip(disabledExtensionsField, "settings.tooltip.disabledExtensions");
    }

    private static void tooltip(@NotNull JComponent component, @NotNull String key) {
        component.setToolTipText(YlMessageBundle.message(key));
    }

    private static @NotNull JLabel label(@NotNull String labelKey, @NotNull String tooltipKey) {
        JLabel label = new JLabel(YlMessageBundle.message(labelKey));
        label.setToolTipText(YlMessageBundle.message(tooltipKey));
        return label;
    }

    public JPanel getPanel() {
        return root;
    }

    public void load(YlCompletionSettingsState state) {
        enabledBox.setSelected(state.enabled);
        baseUrlField.setText(state.baseUrl);
        apiKeyField.setText(state.apiKey);
        modelField.setText(state.model);
        maxTokensSpinner.setValue(state.maxTokens);
        temperatureSpinner.setValue(state.temperature);
        topPSpinner.setValue(state.topP);
        debounceMsSpinner.setValue(state.debounceMs);
        timeoutMsSpinner.setValue(state.timeoutMs);
        contextMaxCharsSpinner.setValue(state.contextMaxChars);
        triggerMinPrefixSpinner.setValue(state.triggerMinPrefixLength);
        disabledExtensionsField.setText(state.disabledExtensions);
        testStatusLabel.setText(" ");
    }

    public void apply(YlCompletionSettingsState state) {
        state.enabled = enabledBox.isSelected();
        state.baseUrl = baseUrlField.getText().trim();
        state.apiKey = getApiKey().trim();
        state.model = modelField.getText().trim();
        state.maxTokens = ((Number) maxTokensSpinner.getValue()).intValue();
        state.temperature = ((Number) temperatureSpinner.getValue()).doubleValue();
        state.topP = ((Number) topPSpinner.getValue()).doubleValue();
        state.debounceMs = ((Number) debounceMsSpinner.getValue()).intValue();
        state.timeoutMs = ((Number) timeoutMsSpinner.getValue()).intValue();
        state.contextMaxChars = ((Number) contextMaxCharsSpinner.getValue()).intValue();
        state.triggerMinPrefixLength = ((Number) triggerMinPrefixSpinner.getValue()).intValue();
        state.disabledExtensions = disabledExtensionsField.getText().trim();
    }

    public String getApiKey() {
        char[] pwd = apiKeyField.getPassword();
        return pwd == null ? "" : new String(pwd);
    }

    public boolean isModified(YlCompletionSettingsState state) {
        if (state.enabled != enabledBox.isSelected()) return true;
        if (!state.baseUrl.equals(baseUrlField.getText().trim())) return true;
        if (!state.apiKey.equals(getApiKey().trim())) return true;
        if (!state.model.equals(modelField.getText().trim())) return true;
        if (state.maxTokens != ((Number) maxTokensSpinner.getValue()).intValue()) return true;
        if (Double.compare(state.temperature, ((Number) temperatureSpinner.getValue()).doubleValue()) != 0) return true;
        if (Double.compare(state.topP, ((Number) topPSpinner.getValue()).doubleValue()) != 0) return true;
        if (state.debounceMs != ((Number) debounceMsSpinner.getValue()).intValue()) return true;
        if (state.timeoutMs != ((Number) timeoutMsSpinner.getValue()).intValue()) return true;
        if (state.contextMaxChars != ((Number) contextMaxCharsSpinner.getValue()).intValue()) return true;
        if (state.triggerMinPrefixLength != ((Number) triggerMinPrefixSpinner.getValue()).intValue()) return true;
        return !state.disabledExtensions.equals(disabledExtensionsField.getText().trim());
    }

    private void resetToDefaults() {
        YlCompletionSettingsState defaults = new YlCompletionSettingsState();
        load(defaults);
    }

    private void testConnection() {
        testConnectionButton.setEnabled(false);
        testStatusLabel.setText(YlMessageBundle.message("settings.test.connecting"));

        String baseUrl = baseUrlField.getText().trim();
        String model = modelField.getText().trim();
        int timeoutMs = ((Number) timeoutMsSpinner.getValue()).intValue();
        String apiKey = getApiKey().trim();
        ModalityState modality = ModalityState.stateForComponent(root);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            DeepSeekFimClient.getInstance()
                    .testConnection(apiKey, baseUrl, model, timeoutMs)
                    .whenComplete((v, err) ->
                            ApplicationManager.getApplication().invokeLater(() -> {
                                testConnectionButton.setEnabled(true);
                                if (err == null) {
                                    testStatusLabel.setText(YlMessageBundle.message("settings.test.success"));
                                    Messages.showInfoMessage(root,
                                            YlMessageBundle.message("settings.test.success"),
                                            YlMessageBundle.message("settings.test.title"));
                                    return;
                                }
                                Throwable cause = err instanceof CompletionException ? err.getCause() : err;
                                String msg;
                                if (cause instanceof LlmException llm && llm.getKind() == LlmException.Kind.AUTH) {
                                    msg = YlMessageBundle.message("settings.test.authFailed");
                                } else {
                                    msg = YlMessageBundle.message("settings.test.failed",
                                            cause == null ? "unknown" : cause.getMessage());
                                }
                                testStatusLabel.setText(msg);
                                Messages.showErrorDialog(root, msg,
                                        YlMessageBundle.message("settings.test.title"));
                            }, modality));
        });
    }
}
