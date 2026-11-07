package net.storagenexus.client.compat;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The search box of the recipe viewer (JEI or REI), when one is installed: their plugins connect it here, and the
 * terminals read and write it to keep both searches the same. Safe to call with no viewer installed.
 */
public final class RecipeViewerSearch {
    private static @Nullable Supplier<@Nullable String> getter;
    private static @Nullable Consumer<String> setter;

    public static void connect(Supplier<@Nullable String> get, Consumer<String> set) {
        getter = get;
        setter = set;
    }

    public static void disconnect() {
        getter = null;
        setter = null;
    }

    /** The viewer's search text, or null if there is no viewer (or it isn't ready). */
    public static @Nullable String get() {
        try {
            return getter == null ? null : getter.get();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static void set(String text) {
        try {
            if (setter != null) setter.accept(text);
        } catch (RuntimeException ignored) {
            // the viewer isn't ready (e.g. still reloading); the next change tries again
        }
    }

    private RecipeViewerSearch() {}
}
