package com.lyl.ylcodecompletion.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class YlPasswordSafe {

    private static final String SERVICE = "YlCodeCompletion";
    private static final String KEY = "apiKey";

    /**
     * 进程内缓存：避免反复触发 macOS Keychain 鉴权弹窗。
     * 第一次需要 key 时（通常是首次发起补全请求）触发一次系统鉴权，
     * 之后整个 IDE 生命周期都直接走缓存。
     */
    private static final AtomicReference<String> CACHE = new AtomicReference<>();
    private static volatile boolean cacheLoaded = false;

    private YlPasswordSafe() {
    }

    private static @NotNull CredentialAttributes attrs() {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName(SERVICE, KEY)
        );
    }

    /**
     * 同步读取 API key（带内存缓存）。会触碰系统 keychain，是慢操作。
     * 禁止在 EDT 调用，请使用 {@link #loadApiKeyAsync()}。
     */
    public static @Nullable String loadApiKey() {
        if (cacheLoaded) {
            return CACHE.get();
        }
        Credentials c = PasswordSafe.getInstance().get(attrs());
        String pwd = c == null ? null : c.getPasswordAsString();
        String result = (pwd == null || pwd.isEmpty()) ? null : pwd;
        CACHE.set(result);
        cacheLoaded = true;
        return result;
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
        String normalized = (apiKey == null || apiKey.isEmpty()) ? null : apiKey;
        if (normalized == null) {
            PasswordSafe.getInstance().set(attrs(), null);
        } else {
            PasswordSafe.getInstance().set(attrs(), new Credentials(KEY, normalized));
        }
        // 写入后同步刷新缓存，避免下次 loadApiKey 又去读 keychain
        CACHE.set(normalized);
        cacheLoaded = true;
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
