package com.lyl.ylcodecompletion.status;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 全局补全状态：负责单飞守门 + 状态广播。
 * <p>
 * 单飞策略：当一次请求在飞时，{@link #tryStart()} 直接返回 false，新请求被丢弃。
 */
public final class YlBusyState {

    public enum Status {
        IDLE,
        LOADING,
        ERROR
    }

    public interface Listener {
        void onStatusChanged(@NotNull Status status);
    }

    public static final Topic<Listener> TOPIC =
            Topic.create("YlBusyState", Listener.class);

    private static final Logger LOG = Logger.getInstance(YlBusyState.class);

    private final AtomicBoolean inFlight = new AtomicBoolean(false);
    private final AtomicReference<Status> status = new AtomicReference<>(Status.IDLE);

    public static YlBusyState getInstance() {
        return ApplicationManager.getApplication().getService(YlBusyState.class);
    }

    /**
     * 尝试占座。返回 true 表示拿到坑位，调用方负责在结束后调用 {@link #finishOk()} / {@link #finishError()}。
     * 返回 false 表示已有请求在飞，调用方应直接放弃。
     */
    public boolean tryStart() {
        if (!inFlight.compareAndSet(false, true)) {
            LOG.debug("YlBusyState: request rejected, another in flight");
            return false;
        }
        publish(Status.LOADING);
        return true;
    }

    public void finishOk() {
        if (inFlight.compareAndSet(true, false)) {
            publish(Status.IDLE);
        }
    }

    public void finishError() {
        if (inFlight.compareAndSet(true, false)) {
            publish(Status.ERROR);
        }
    }

    public @NotNull Status currentStatus() {
        return status.get();
    }

    public boolean isInFlight() {
        return inFlight.get();
    }

    private void publish(@NotNull Status next) {
        status.set(next);
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(TOPIC).onStatusChanged(next);
    }
}
