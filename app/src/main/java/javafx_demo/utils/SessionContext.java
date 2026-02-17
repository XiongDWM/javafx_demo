package javafx_demo.utils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 会话上下文 — 单例，保存 ECDH 会话、JWT、用户信息、客户端码
 */
public class SessionContext {

    private static final Path CLIENT_ID_FILE = Paths.get(
            System.getProperty("user.home"), ".future_pal", "client.id");
    private static final SessionContext INSTANCE = new SessionContext();

    private String sessionId;      // ECDH 会话 ID
    private SecretKey sharedKey;    // AES 共享密钥
    private String jwtToken;       // JWT 令牌
    private long userId;           // 当前用户 ID
    private String username;       // 当前用户名
    private String role;           // 用户角色
    private String softwareCode;   // 客户端唯一码

    private SessionContext() {
        this.softwareCode = loadOrCreateSoftwareCode();
    }

    public static SessionContext getInstance() {
        return INSTANCE;
    }

    // ---------- ECDH 会话 ----------

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public SecretKey getSharedKey() { return sharedKey; }
    public void setSharedKey(SecretKey sharedKey) { this.sharedKey = sharedKey; }

    public boolean hasSession() { return sessionId != null && sharedKey != null; }

    // ---------- JWT & 用户 ----------

    public String getJwtToken() { return jwtToken; }
    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
        parseToken(jwtToken);
    }

    public long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }

    public boolean isLoggedIn() { return jwtToken != null; }

    // ---------- 客户端码 ----------

    public String getSoftwareCode() { return softwareCode; }

    // ---------- 清除(退出登录) ----------

    public void clear() {
        this.jwtToken = null;
        this.userId = 0;
        this.username = null;
        this.role = null;
        // 保留 session 和 softwareCode
    }

    public void clearAll() {
        clear();
        this.sessionId = null;
        this.sharedKey = null;
    }

    // ---------- 内部方法 ----------

    /** 解析 JWT 载荷 (base64 JSON) 提取 userId/username/role */
    private void parseToken(String token) {
        if (token == null) return;
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return;
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            // 简单提取字段 (无需 JSON 库)
            this.userId = Long.parseLong(extractJsonField(payload, "sub"));
            this.username = extractJsonField(payload, "username");
            this.role = extractJsonField(payload, "role");
        } catch (Exception e) {
            System.err.println("JWT 解析失败: " + e.getMessage());
        }
    }

    private String extractJsonField(String json, String key) {
        String needle = "\"" + key + "\"";
        int idx = json.indexOf(needle);
        if (idx < 0) return "";
        int colonIdx = json.indexOf(":", idx);
        if (colonIdx < 0) return "";
        int start = colonIdx + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return "";
        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return end > 0 ? json.substring(start + 1, end) : "";
        } else {
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(start, end).trim();
        }
    }

    /** 从本地文件加载或首次生成客户端码 */
    private String loadOrCreateSoftwareCode() {
        try {
            if (Files.exists(CLIENT_ID_FILE)) {
                String code = Files.readString(CLIENT_ID_FILE).trim();
                if (!code.isEmpty()) {
                    System.out.println("客户端码已加载: " + code);
                    return code;
                }
            }
            // 首次生成
            String code = UUID.randomUUID().toString().replace("-", "");
            Files.createDirectories(CLIENT_ID_FILE.getParent());
            Files.writeString(CLIENT_ID_FILE, code);
            System.out.println("客户端码已生成: " + code);
            return code;
        } catch (IOException e) {
            System.err.println("客户端码文件操作失败: " + e.getMessage());
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
