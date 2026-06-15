package com.lyl.ylcodecompletion;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

public final class YlMessageBundle {
    private static final String BUNDLE = "messages.YlMessageBundle";
    private static final DynamicBundle INSTANCE = new DynamicBundle(YlMessageBundle.class, BUNDLE);

    private YlMessageBundle() {
    }

    public static @Nls String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    public static Supplier<@Nls String> lazyMessage(
            @PropertyKey(resourceBundle = BUNDLE) String key,
            Object... params
    ) {
        return INSTANCE.getLazyMessage(key, params);
    }
}
