package io.github.andrew6rant.autoslabs.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class CommonConfig extends MidnightConfig {
    @Entry public static boolean dumpResources = false;
    @Entry public static boolean showEnhancedSlabLines = true;
    @Entry public static ShowCrosshairIcon showCrosshairIcon = ShowCrosshairIcon.ON_CHANGE;
    public enum ShowCrosshairIcon {
        ALWAYS,
        ON_CHANGE,
        NEVER
    }
}
