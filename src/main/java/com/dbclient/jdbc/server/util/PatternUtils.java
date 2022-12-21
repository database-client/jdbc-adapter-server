package com.dbclient.jdbc.server.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author cweijan
 * @since 2020/07/15 18:33
 */
public abstract class PatternUtils {

    public static boolean match(String origin, String regex) {
        return find(origin, regex, 0) != null;
    }

    public static String find(String origin, String regex) {
        return find(origin, regex, 0);
    }

    public static String find(String origin, String regex, Integer groupIndex) {

        Matcher matcher = Pattern.compile(regex).matcher(origin);
        if (matcher.find()) {
            if (matcher.groupCount() >= groupIndex) {
                return matcher.group(groupIndex);
            }
            return null;
        }
        return null;
    }
}
