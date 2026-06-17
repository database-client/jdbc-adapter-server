package com.dbclient.jdbc.server.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author cweijan
 * @since 2020/07/15 18:33
 */
public abstract class PatternUtils {

    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    public static boolean match(String origin, String regex) {
        return find(origin, regex, 0) != null;
    }

    public static String find(String origin, String regex) {
        return find(origin, regex, 0);
    }

    public static String find(String origin, String regex, Integer groupIndex) {
        Matcher matcher = PATTERN_CACHE.computeIfAbsent(regex, Pattern::compile).matcher(origin);
        if (matcher.find()) {
            if (matcher.groupCount() >= groupIndex) {
                return matcher.group(groupIndex);
            }
            return null;
        }
        return null;
    }
}
