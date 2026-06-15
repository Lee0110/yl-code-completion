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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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

    /**
     * 用户是否动过 API Key 输入框。
     * - false：UI 显示空 + placeholder "&lt;stored&gt;"，apply 时不写 keychain。
     * - true：apply 时把输入框内容写入 keychain（空字符串则清除）。
     * 注意：用户主动清空也算 dirty。
     */
    private boolean apiKeyDirty = false;
    private boolean suppressDirty = false;

    public YlCompletionSettingsComponent() {
        baseUrlField.getEmptyText().setText("https://api.deepseek.com/beta");
        modelField.getEmptyText().setText("deepseek-v4-pro");

        // 监听输入框变化标记 dirty
        apiKeyField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { mark(); }
            @Override public void removeUpdate(DocumentEvent e) { mark(); }
            @Override public void changedUpdate(DocumentEvent e) { mark(); }
            private void mark() { if (!suppressDirty) apiKeyDirty = true; }
        });

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

    public void load(YlCompletionSettingsState state) {
        enabledBox.setSelected(state.enabled);
        baseUrlField.setText(state.baseUrl);
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

        // 重置 API Key 输入框：清空、显示 placeholder、不算 dirty
        suppressDirty = true;
        try {
            apiKeyField.setText("");
            apiKeyField.getEmptyText().setText(state.hasApiKey
                    ? YlMessageBundle.message("settings.field.apiKey.storedPlaceholder")
                    : YlMessageBundle.message("settings.field.apiKey.emptyPlaceholder"));
        } finally {
            suppressDirty = false;
            apiKeyDirty = false;
        }
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

    public boolean isApiKeyDirty() {
        return apiKeyDirty;
    }

    /** 用户输入的新 API key；apply 后调用方负责重置 dirty 标志。 */
    public String getApiKey() {
        char[] pwd = apiKeyField.getPassword();
        return pwd == null ? "" : new String(pwd);
    }

    public void resetApiKeyDirty(boolean hasApiKey) {
        suppressDirty = true;
        try {
            apiKeyField.setText("");
            apiKeyField.getEmptyText().setText(hasApiKey
                    ? YlMessageBundle.message("settings.field.apiKey.storedPlaceholder")
                    : YlMessageBundle.message("settings.field.apiKey.emptyPlaceholder"));
        } finally {
            suppressDirty = false;
            apiKeyDirty = false;
        }
    }

    public boolean isModified(YlCompletionSettingsState state) {
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
        if (!state.disabledExtensions.equals(disabledExtensionsField.getText().trim())) return true;
        return apiKeyDirty;
    }

    private void resetToDefaults() {
        // 重置时把 keychain 里的 key 也清掉（视为用户主动删除）
        YlCompletionSettingsState defaults = new YlCompletionSettingsState();
        load(defaults);
        apiKeyDirty = true;
    }

    private void testConnection() {
        testConnectionButton.setEnabled(false);
        testStatusLabel.setText(YlMessageBundle.message("settings.test.connecting"));

        String baseUrl = baseUrlField.getText().trim();
        String model = modelField.getText().trim();
        int timeoutMs = ((Number) timeoutMsSpinner.getValue()).intValue();
        // 用户在输入框输入了新 key → 用新 key 测试；否则用 keychain 缓存里的 key
        String typedKey = getApiKey();
        ModalityState modality = ModalityState.stateForComponent(root);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String apiKey = typedKey.isEmpty() ? YlPasswordSafe.loadApiKey() : typedKey;
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
