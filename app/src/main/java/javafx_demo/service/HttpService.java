package javafx_demo.service;

import javafx_demo.utils.ConfigManager;
import javafx_demo.utils.CryptoUtil;
import javafx_demo.utils.SessionContext;

import javax.crypto.SecretKey;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Duration;
import java.util.UUID;

/**
 * HTTP 通信服务，内置 ECDH 握手 + AES-GCM 加解密 + HMAC-SHA256 签名
 */
public class HttpService {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String BASE_URL = ConfigManager.getInstance().getServerBaseUrl();

    // ---------- ECDH 握手 ----------

    /** 执行 ECDH 密钥协商，成功后 SessionContext 中保存 sessionId 和 sharedKey */
    public static void handshake() throws Exception {
        KeyPair kp = CryptoUtil.generateKeyPair();
        String clientPubKeyBase64 = CryptoUtil.encodePublicKey(kp.getPublic());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/crypto/handshake"))
                .header("X-Client-Key", clientPubKeyBase64)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("握手失败: HTTP " + resp.statusCode());
        }

        String serverKeyBase64 = resp.headers().firstValue("X-Server-Key")
                .orElseThrow(() -> new RuntimeException("响应中缺少 X-Server-Key"));
        String sessionId = resp.headers().firstValue("X-Session-Id")
                .orElseThrow(() -> new RuntimeException("响应中缺少 X-Session-Id"));

        PublicKey serverPub = CryptoUtil.decodePublicKey(serverKeyBase64);
        SecretKey sharedKey = CryptoUtil.deriveSharedSecret(kp.getPrivate(), serverPub);

        SessionContext ctx = SessionContext.getInstance();
        ctx.setSessionId(sessionId);
        ctx.setSharedKey(sharedKey);
        System.out.println("ECDH 握手成功，sessionId=" + sessionId);
    }

    /** 确保有活跃的 ECDH 会话，没有则重新握手 */
    private static void ensureSession() throws Exception {
        if (!SessionContext.getInstance().hasSession()) {
            handshake();
        }
    }

    // ---------- 加密 POST ----------

    /**
     * 发送加密的 POST 请求
     * @param path  API 路径，如 "/user/pal/login"
     * @param json  明文 JSON body
     * @return 解密后的响应 JSON
     */
    public static String post(String path, String json) throws Exception {
        ensureSession();
        SessionContext ctx = SessionContext.getInstance();
        SecretKey key = ctx.getSharedKey();

        // 加密 body
        String encrypted = CryptoUtil.encrypt(key, json);

        // 签名: POST\npath\ntimestamp\nbody
        String timestamp = String.valueOf(System.currentTimeMillis());
        String message = "POST\n" + path + "\n" + timestamp + "\n" + encrypted;
        String signature = CryptoUtil.hmacSign(key, message);

        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "text/plain;charset=UTF-8")
                .header("X-Session-Id", ctx.getSessionId())
                .header("X-Timestamp", timestamp)
                .header("X-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(encrypted));

        if (ctx.getJwtToken() != null) {
            rb.header("Authorization", "Bearer " + ctx.getJwtToken());
        }

        HttpResponse<String> resp = CLIENT.send(rb.build(), HttpResponse.BodyHandlers.ofString());
        return handleEncryptedResponse(resp, key, path);
    }

    // ---------- 加密 GET ----------

    /**
     * 发送加密的 GET 请求
     * @param path  API 路径 (可含 query string)
     * @return 解密后的响应 JSON
     */
    public static String get(String path) throws Exception {
        ensureSession();
        SessionContext ctx = SessionContext.getInstance();
        SecretKey key = ctx.getSharedKey();

        String timestamp = String.valueOf(System.currentTimeMillis());
        String message = "GET\n" + path + "\n" + timestamp + "\n";
        String signature = CryptoUtil.hmacSign(key, message);

        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("X-Session-Id", ctx.getSessionId())
                .header("X-Timestamp", timestamp)
                .header("X-Signature", signature)
                .GET();

        if (ctx.getJwtToken() != null) {
            rb.header("Authorization", "Bearer " + ctx.getJwtToken());
        }

        HttpResponse<String> resp = CLIENT.send(rb.build(), HttpResponse.BodyHandlers.ofString());
        return handleEncryptedResponse(resp, key, path);
    }

    // ---------- 文件上传 (SESSION_ONLY，body 不加密) ----------

    /**
     * 上传文件（multipart/form-data），响应不加密
     * @param file 要上传的文件
     * @return 响应 JSON (明文)
     */
    public static String uploadFile(File file) throws Exception {
        ensureSession();
        SessionContext ctx = SessionContext.getInstance();
        SecretKey key = ctx.getSharedKey();

        String boundary = "----FuturePal" + UUID.randomUUID().toString().replace("-", "");
        String path = "/oss/upload";

        // 签名 (SESSION_ONLY: 无 body)
        String timestamp = String.valueOf(System.currentTimeMillis());
        String message = "POST\n" + path + "\n" + timestamp + "\n";
        String signature = CryptoUtil.hmacSign(key, message);

        // 构建 multipart body
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        String filename = file.getName();
        String mimeType = guessMimeType(filename);

        byte[] header = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[header.length + fileBytes.length + footer.length];
        System.arraycopy(header, 0, body, 0, header.length);
        System.arraycopy(fileBytes, 0, body, header.length, fileBytes.length);
        System.arraycopy(footer, 0, body, header.length + fileBytes.length, footer.length);

        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("X-Session-Id", ctx.getSessionId())
                .header("X-Timestamp", timestamp)
                .header("X-Signature", signature)
                .header("Authorization", "Bearer " + ctx.getJwtToken())
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));

        HttpResponse<String> resp = CLIENT.send(rb.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("上传失败: HTTP " + resp.statusCode());
        }
        // SESSION_ONLY 路径响应不加密，直接返回
        return resp.body();
    }

    // ---------- 响应处理 ----------

    private static String handleEncryptedResponse(HttpResponse<String> resp, SecretKey key, String path) throws Exception {
        String body = resp.body();
        if (resp.statusCode() != 200) {
            throw new RuntimeException("请求失败: HTTP " + resp.statusCode() + " " + body);
        }
        // 响应是 AES-GCM 加密的 Base64 文本
        try {
            String decrypted = CryptoUtil.decrypt(key, body.trim());
            // 检查是否密钥过期 (code=556)
            if (decrypted.contains("\"code\":556")) {
                System.out.println("密钥过期，重新握手...");
                handshake();
                throw new RuntimeException("RETRY");
            }
            return decrypted;
        } catch (RuntimeException re) {
            if ("RETRY".equals(re.getMessage())) throw re;
            // 如果解密失败，可能是明文响应
            return body;
        } catch (Exception e) {
            // 解密失败，尝试当作明文处理
            System.err.println("响应解密失败: " + e.getMessage());
            return body;
        }
    }

    private static String guessMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
