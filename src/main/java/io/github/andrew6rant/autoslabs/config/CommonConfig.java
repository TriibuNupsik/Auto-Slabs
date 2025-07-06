package io.github.andrew6rant.autoslabs.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class CommonConfig extends MidnightConfig {
    @Entry public static boolean suppressStatementAPILogger = true;
    @Entry public static boolean dumpResources = false;
    @Entry public static boolean showEnhancedSlabLines = true;
}
