package javafx_demo.utils;

/**
 * API Configuration - Centralized API endpoint management
 */
public class ApiConfig {
    private static final ConfigManager config = ConfigManager.getInstance();
    
    // Base URL
    private static final String BASE_URL = config.getServerBaseUrl();
    
    // Authentication Endpoints
    public static final String LOGIN = config.getProperty("api.login", "/api/auth/login");
    public static final String LOGOUT = config.getProperty("api.logout", "/api/auth/logout");
    public static final String REGISTER = config.getProperty("api.register", "/api/auth/register");
    public static final String REFRESH_TOKEN = config.getProperty("api.refresh-token", "/api/auth/refresh");
    
    // User Endpoints
    public static final String USER_INFO = config.getProperty("api.user-info", "/api/user/info");
    public static final String USER_PROFILE = config.getProperty("api.user-profile", "/api/user/profile");
    public static final String USER_UPDATE = config.getProperty("api.user-update", "/api/user/update");
    public static final String USER_AVATAR = config.getProperty("api.user-avatar", "/api/user/avatar");
    public static final String CHANGE_PASSWORD = config.getProperty("api.change-password", "/api/user/password");
    
    // Data Endpoints
    public static final String DATA_LIST = config.getProperty("api.data-list", "/api/data/list");
    public static final String DATA_DETAIL = config.getProperty("api.data-detail", "/api/data/detail/{id}");
    public static final String DATA_CREATE = config.getProperty("api.data-create", "/api/data/create");
    public static final String DATA_UPDATE = config.getProperty("api.data-update", "/api/data/update/{id}");
    public static final String DATA_DELETE = config.getProperty("api.data-delete", "/api/data/delete/{id}");
    
    // Upload Endpoints
    public static final String UPLOAD_FILE = config.getProperty("api.upload-file", "/api/upload/file");
    public static final String UPLOAD_IMAGE = config.getProperty("api.upload-image", "/api/upload/image");
    
    /**
     * Get full URL for an endpoint
     * @param endpoint API endpoint path
     * @return Full URL
     */
    public static String getFullUrl(String endpoint) {
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint;
        }
        return BASE_URL + endpoint;
    }
    
    /**
     * Get login URL
     */
    public static String getLoginUrl() {
        return getFullUrl(LOGIN);
    }
    
    /**
     * Get logout URL
     */
    public static String getLogoutUrl() {
        return getFullUrl(LOGOUT);
    }
    
    /**
     * Get register URL
     */
    public static String getRegisterUrl() {
        return getFullUrl(REGISTER);
    }
    
    /**
     * Get user info URL
     */
    public static String getUserInfoUrl() {
        return getFullUrl(USER_INFO);
    }
    
    /**
     * Get refresh token URL
     */
    public static String getRefreshTokenUrl() {
        return getFullUrl(REFRESH_TOKEN);
    }
    
    /**
     * Get user profile URL
     */
    public static String getUserProfileUrl() {
        return getFullUrl(USER_PROFILE);
    }
    
    /**
     * Get user update URL
     */
    public static String getUserUpdateUrl() {
        return getFullUrl(USER_UPDATE);
    }
    
    /**
     * Get change password URL
     */
    public static String getChangePasswordUrl() {
        return getFullUrl(CHANGE_PASSWORD);
    }
    
    /**
     * Get data list URL
     */
    public static String getDataListUrl() {
        return getFullUrl(DATA_LIST);
    }
    
    /**
     * Get data detail URL with ID
     */
    public static String getDataDetailUrl(Object id) {
        return buildUrl(DATA_DETAIL, id);
    }
    
    /**
     * Get base URL
     */
    public static String getBaseUrl() {
        return BASE_URL;
    }
    
    /**
     * Build URL with path parameters
     * @param endpoint Base endpoint
     * @param params Path parameters
     * @return Full URL with parameters
     */
    public static String buildUrl(String endpoint, Object... params) {
        String url = getFullUrl(endpoint);
        return String.format(url, params);
    }
}
