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

import javax.swing.JButton;
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
    private final JBIntSpinner maxTokensSpinner = new JBIntSpinner(128, 1, 4096);
    private final JSpinner temperatureSpinner =
            new JSpinner(new SpinnerNumberModel(0.2, 0.0, 2.0, 0.1));
    private final JSpinner topPSpinner =
            new JSpinner(new SpinnerNumberModel(0.95, 0.0, 1.0, 0.05));
    private final JBIntSpinner debounceMsSpinner = new JBIntSpinner(300, 0, 10_000);
    private final JBIntSpinner timeoutMsSpinner = new JBIntSpinner(8000, 1000, 60_000);
    private final JBIntSpinner contextMaxCharsSpinner = new JBIntSpinner(4000, 200, 200_000);
    private final JBIntSpinner triggerMinPrefixSpinner = new JBIntSpinner(1, 0, 100);
    private final JBTextField disabledExtensionsField = new JBTextField();

    private final JButton testConnectionButton = new JButton(
            YlMessageBundle.message("settings.button.testConnection"));
    private final JButton resetButton = new JButton(YlMessageBundle.message("settings.button.reset"));
    private final JLabel testStatusLabel = new JLabel(" ");

    public YlCompletionSettingsComponent() {
        baseUrlField.getEmptyText().setText("https://api.deepseek.com/beta");
        modelField.getEmptyText().setText("deepseek-v4-pro");

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonRow.add(testConnectionButton);
        buttonRow.add(resetButton);
        buttonRow.add(testStatusLabel);

        JPanel form = FormBuilder.createFormBuilder()
                .addComponent(enabledBox)
                .addSeparator()
                .addLabeledComponent(new JLabel(YlMessageBundle.message("settings.field.baseUrl")), baseUrlField)
                .addLabeledComponent(new JLabel(YlMessageBundle.message("settings.field.apiKey")), apiKeyField)
                .addLabeledComponent(new JLabel(YlMessageBundle.message("settings.field.model")), modelField)
                .addSeparator()
                .addLabeledComponent(new JLabel(YlMessageBundle.message("settings.field.maxTokens")), maxTokensSpinner)
                .addLabeledComponent(new JLabel(YlMessageBundle.message("settings.field.temperature")), temperatureSpinner)
                .addLabeledComponent(new JLabel(YlMessageBundle.message("settings.field.topP")), topPSpinner)
                .addLabeledComponent(new JLabel(YlMessageBundle.message("settings.field.debounceMs")), debounceMsSpinner)
                .addLabeledComponent(new JLabel(YlMessageBundle.message("settings.field.timeoutMs")), timeoutMsSpinner)
                .addLabeledComponent(new JLabel(YlMessageBundle.message("settings.field.contextMaxChars")), contextMaxCharsSpinner)
                .addLabeledComponent(new JLabel(YlMessageBundle.message("settings.field.triggerMinPrefixLength")), triggerMinPrefixSpinner)
                .addLabeledComponent(new JLabel(YlMessageBundle.message("settings.field.disabledExtensions")), disabledExtensionsField)
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

    public JPanel getPanel() {
        return root;
    }

    public void load(YlCompletionSettingsState state, String apiKey) {
        enabledBox.setSelected(state.enabled);
        baseUrlField.setText(state.baseUrl);
        modelField.setText(state.model);
        apiKeyField.setText(apiKey == null ? "" : apiKey);
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

    public void setApiKey(String apiKey) {
        apiKeyField.setText(apiKey == null ? "" : apiKey);
    }

    public boolean isModifiedExceptApiKey(YlCompletionSettingsState state) {
        if (state.enabled != enabledBox.isSelected()) return true;
        if (!state.baseUrl.equals(baseUrlField.getText().trim())) return true;
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

    public boolean isModified(YlCompletionSettingsState state, String storedApiKey) {
        if (isModifiedExceptApiKey(state)) return true;
        String currentKey = getApiKey();
        String stored = storedApiKey == null ? "" : storedApiKey;
        return !currentKey.equals(stored);
    }

    private void resetToDefaults() {
        YlCompletionSettingsState defaults = new YlCompletionSettingsState();
        load(defaults, "");
    }

    private void testConnection() {
        testConnectionButton.setEnabled(false);
        testStatusLabel.setText(YlMessageBundle.message("settings.test.connecting"));

        String baseUrl = baseUrlField.getText().trim();
        String model = modelField.getText().trim();
        String apiKey = getApiKey();
        int timeoutMs = ((Number) timeoutMsSpinner.getValue()).intValue();

        ModalityState modality = ModalityState.stateForComponent(root);
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
    }
}
