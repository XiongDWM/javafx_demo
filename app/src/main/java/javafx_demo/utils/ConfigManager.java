package javafx_demo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Configuration Manager - Singleton pattern for managing application configuration
 */
public class ConfigManager {
    private static ConfigManager instance;
    private Properties properties;
    private static final String CONFIG_FILE = "application.properties";

    private ConfigManager() {
        properties = new Properties();
        loadProperties();
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.err.println("Unable to find " + CONFIG_FILE);
                return;
            }
            // 使用 UTF-8 编码加载配置文件
            properties.load(new InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8));
            // Resolve property references like ${server.protocol}
            resolvePropertyReferences();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void resolvePropertyReferences() {
        Properties resolved = new Properties();
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            resolved.setProperty(key, resolveValue(value));
        }
        properties = resolved;
    }

    private String resolveValue(String value) {
        if (value == null) return null;
        
        int startIndex = value.indexOf("${");
        if (startIndex == -1) {
            return value;
        }
        
        StringBuilder result = new StringBuilder();
        int currentIndex = 0;
        
        while (startIndex != -1) {
            result.append(value, currentIndex, startIndex);
            int endIndex = value.indexOf("}", startIndex);
            if (endIndex == -1) {
                break;
            }
            
            String propertyName = value.substring(startIndex + 2, endIndex);
            String propertyValue = properties.getProperty(propertyName);
            result.append(propertyValue != null ? propertyValue : "${" + propertyName + "}");
            
            currentIndex = endIndex + 1;
            startIndex = value.indexOf("${", currentIndex);
        }
        
        result.append(value.substring(currentIndex));
        return result.toString();
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long getLongProperty(String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    // Server configuration getters
    public String getServerHost() {
        return getProperty("server.host", "localhost");
    }

    public int getServerPort() {
        return getIntProperty("server.port", 8080);
    }

    public String getServerProtocol() {
        return getProperty("server.protocol", "http");
    }

    public String getServerBaseUrl() {
        return getProperty("server.base-url");
    }

    // Connection settings getters
    public int getConnectionTimeout() {
        return getIntProperty("connection.timeout", 5000);
    }

    public int getRetryCount() {
        return getIntProperty("connection.retry-count", 3);
    }

    public int getReadTimeout() {
        return getIntProperty("connection.read-timeout", 10000);
    }

    // Cache settings getters
    public boolean isCacheEnabled() {
        return getBooleanProperty("cache.enabled", true);
    }

    public int getCacheCapacity() {
        return getIntProperty("cache.capacity", 64);
    }

    public long getCacheExpireTime() {
        return getLongProperty("cache.expire-time", 300000);
    }

    // App settings getters
    public String getAppTitle() {
        return getProperty("app.title", "JavaFX Application");
    }

    public String getAppVersion() {
        return getProperty("app.version", "1.0.0");
    }
}
