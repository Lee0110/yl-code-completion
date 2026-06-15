package com.lyl.ylcodecompletion.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public final class YlPasswordSafe {

    private static final String SERVICE = "YlCodeCompletion";
    private static final String KEY = "apiKey";

    private YlPasswordSafe() {
    }

    private static @NotNull CredentialAttributes attrs() {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName(SERVICE, KEY)
        );
    }

    /**
     * 同步读取 API key。会触碰系统 keychain，是慢操作。
     * 禁止在 EDT 调用，请使用 {@link #loadApiKeyAsync()}。
     */
    public static @Nullable String loadApiKey() {
        Credentials c = PasswordSafe.getInstance().get(attrs());
        if (c == null) return null;
        String pwd = c.getPasswordAsString();
        return (pwd == null || pwd.isEmpty()) ? null : pwd;
    }

    public static @NotNull CompletableFuture<@Nullable String> loadApiKeyAsync() {
        CompletableFuture<String> f = new CompletableFuture<>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                f.complete(loadApiKey());
            } catch (Throwable t) {
                f.completeExceptionally(t);
            }
        });
        return f;
    }

    /**
     * 同步写入 API key。会触碰系统 keychain，是慢操作。
     * 禁止在 EDT 调用，请使用 {@link #storeApiKeyAsync(String)}。
     */
    public static void storeApiKey(@Nullable String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            PasswordSafe.getInstance().set(attrs(), null);
        } else {
            PasswordSafe.getInstance().set(attrs(), new Credentials(KEY, apiKey));
        }
    }

    public static @NotNull CompletableFuture<Void> storeApiKeyAsync(@Nullable String apiKey) {
        CompletableFuture<Void> f = new CompletableFuture<>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                storeApiKey(apiKey);
                f.complete(null);
            } catch (Throwable t) {
                f.completeExceptionally(t);
            }
        });
        return f;
    }
}
