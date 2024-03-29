package jp.co.onehr.workflow.util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility for easily retrieving environment variables
 */
public class EnvUtil {

    private static final Logger log = LoggerFactory.getLogger(EnvUtil.class);

    static final Map<String, String> envMap = Maps.newHashMap();

    static {
        try {
            readEnvFile();
        } catch (Exception e) {
            log.error("read env file has error", e);
        }
    }

    protected static void readEnvFile() throws Exception {
        var envFile = new File(".env");
        if (!envFile.exists()) {
            return;
        }
        var lines = FileUtils.readLines(envFile, StandardCharsets.UTF_8);
        for (var line : lines) {
            if (StringUtils.isEmpty(line) || StringUtils.trim(line).startsWith("#")) {
                continue;
            }
            var params = StringUtils.split(StringUtils.trim(line), "=", 2);
            if (params.length == 2) {
                envMap.put(StringUtils.trim(params[0]), StringUtils.trim(params[1]));
            }

        }
    }

    public static String getOrDefault(String envName, String defaultValue) {
        var value = System.getenv(envName);
        return StringUtils.isNotEmpty(value) ? value : envMap.getOrDefault(envName, defaultValue == null ? "" : defaultValue);

    }

    public static String get(String envName) {
        var value = System.getenv(envName);
        if (StringUtils.isEmpty(value)) {
            value = envMap.get(envName);
        }
        return value;
    }

    public static boolean getBooleanOrDefault(String envName, boolean defaultValue) {
        return Boolean.parseBoolean(getOrDefault(envName, String.valueOf(defaultValue)));
    }
}
