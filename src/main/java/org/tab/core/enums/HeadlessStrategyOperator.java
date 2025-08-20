package org.tab.core.enums;

/**
 * Toggle for headless execution strategy (true/false).
 * Kept to preserve structure; extend if you need "NEW/LEGACY" style strategies.
 */
public enum HeadlessStrategyOperator {
    ENABLED,
    DISABLED;

    public static boolean toBool(HeadlessStrategyOperator op) {
        return op == ENABLED;
    }
}
