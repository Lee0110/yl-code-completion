package com.lyl.ylcodecompletion.completion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 上一次成功补全的快照。用于「前缀对齐复用」：
 * 用户逐字符接受 ghost text 时，无需重复发起 HTTP，直接从上次 raw 文本里
 * substring 出剩余部分作为本次建议。
 * <p>
 * 线程安全：单个 {@link AtomicReference} 持有不可变 {@link Snapshot}，
 * 读写都是整体原子替换；命中判定 {@code get()} 一次后所有字段同源。
 */
public final class YlSuggestionCache {

    private static final int PREFIX_TAIL_LEN = 128;

    public record Snapshot(
            @NotNull String filePath,
            int caret,
            @NotNull String prefixTail,
            @NotNull String suggestion
    ) {
    }

    public record Hit(@NotNull String remaining) {
    }

    private static final AtomicReference<Snapshot> CURRENT = new AtomicReference<>();

    private YlSuggestionCache() {
    }

    public static @Nullable Hit tryHit(
            @NotNull String filePath,
            int caret,
            @NotNull CharSequence documentText
    ) {
        Snapshot snap = CURRENT.get();
        if (snap == null) return null;
        if (!snap.filePath.equals(filePath)) return null;

        int n = caret - snap.caret;
        if (n <= 0 || n > snap.suggestion.length()) return null;

        String accepted = snap.suggestion.substring(0, n);
        String currentTail = tail(documentText, caret, snap.prefixTail.length() + n);
        String expected = snap.prefixTail + accepted;
        if (expected.length() != currentTail.length()) return null;
        if (!expected.contentEquals(currentTail)) return null;

        String remaining = snap.suggestion.substring(n);
        if (remaining.isEmpty()) {
            invalidate();
            return null;
        }
        return new Hit(remaining);
    }

    public static void update(
            @NotNull String filePath,
            int caret,
            @NotNull CharSequence documentText,
            @NotNull String suggestion
    ) {
        String prefixTail = tail(documentText, caret, PREFIX_TAIL_LEN);
        CURRENT.set(new Snapshot(filePath, caret, prefixTail, suggestion));
    }

    public static void invalidate() {
        CURRENT.set(null);
    }

    private static @NotNull String tail(@NotNull CharSequence text, int caret, int maxLen) {
        int safeCaret = Math.max(0, Math.min(caret, text.length()));
        int start = Math.max(0, safeCaret - maxLen);
        if (start > 0 && start < text.length()) {
            char c = text.charAt(start);
            char prev = text.charAt(start - 1);
            if (Character.isLowSurrogate(c) && Character.isHighSurrogate(prev)) {
                start += 1;
            }
        }
        return text.subSequence(start, safeCaret).toString();
    }
}
