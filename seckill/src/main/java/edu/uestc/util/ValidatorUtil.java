package edu.uestc.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 手机号码格式校验工具
 */
public class ValidatorUtil {

    // 手机号正则
    private static final Pattern mobilePattern = Pattern.compile("1\\d{10}");

    public static boolean isMobile(String mobile) {
        if (StringUtils.isEmpty(mobile))
            return false;

        Matcher matcher = mobilePattern.matcher(mobile);
        return matcher.matches();
    }

}
