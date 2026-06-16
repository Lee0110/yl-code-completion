package com.lyl.ylcodecompletion.status;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 全局补全状态：状态广播 + 在飞计数。
 * <p>
 * 不做单飞守门 —— 由 IntelliJ inline completion 框架在新事件到来时
 * 自动 cancel 旧的 {@code getSuggestion} 协程；调用方仅需配对 {@link #start()}
 * 与 {@link #finishOk()} / {@link #finishError()} / {@link #finishCancelled()}。
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

    private final AtomicInteger inFlight = new AtomicInteger(0);
    private final AtomicReference<Status> status = new AtomicReference<>(Status.IDLE);
    private final AtomicBoolean errorPending = new AtomicBoolean(false);

    public static YlBusyState getInstance() {
        return ApplicationManager.getApplication().getService(YlBusyState.class);
    }

    /**
     * 标记一次请求开始。每次 {@code start()} 必须配对一次 finish*。
     * 进入新一轮 try 时清掉 pending error，避免被旧错误绑架。
     */
    public void start() {
        inFlight.incrementAndGet();
        errorPending.set(false);
        publish(Status.LOADING);
    }

    public void finishOk() {
        finish(Status.IDLE);
    }

    public void finishError() {
        errorPending.set(true);
        finish(Status.ERROR);
    }

    /**
     * 协程被 cancel（用户继续输入触发新请求）时调用。不进入 ERROR。
     */
    public void finishCancelled() {
        finish(Status.IDLE);
    }

    private void finish(@NotNull Status onLastStatus) {
        int now = inFlight.decrementAndGet();
        if (now < 0) {
            inFlight.set(0);
            return;
        }
        if (now == 0) {
            // pending error 优先：只要本批次内任一请求失败过，最终一定 publish ERROR
            Status finalStatus = errorPending.compareAndSet(true, false) ? Status.ERROR : onLastStatus;
            publish(finalStatus);
        }
    }

    public @NotNull Status currentStatus() {
        return status.get();
    }

    public boolean isInFlight() {
        return inFlight.get() > 0;
    }

    private void publish(@NotNull Status next) {
        status.set(next);
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(TOPIC).onStatusChanged(next);
    }
}
