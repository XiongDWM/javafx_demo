package javafx_demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx_demo.utils.ConfigManager;
import javafx_demo.utils.CryptoUtil;
import javafx_demo.utils.SessionContext;

import javax.crypto.SecretKey;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SSE 客户端 — 监听后端 /events/stream，收到事件后回调到 JavaFX 线程
 * <p>
 * 使用 java.net.http.HttpClient 的同步流式读取实现（后台线程轮询）。
 * 内置自动重连（3 秒间隔）。
 */
public class SseClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String BASE_URL = ConfigManager.getInstance().getServerBaseUrl();
    private static final long RECONNECT_DELAY_MS = 3000;

    /** 事件处理器 */
    @FunctionalInterface
    public interface EventHandler {
        void onEvent(String domain, String action, String resourceId);
    }

    private final Map<String, CopyOnWriteArrayList<EventHandler>> listeners = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Thread workerThread;
    private volatile List<String> domains;

    // ---- 单例 ----
    private static final SseClient INSTANCE = new SseClient();
    public static SseClient getInstance() { return INSTANCE; }
    private SseClient() {}

    /**
     * 启动 SSE 连接
     * @param domains 要监听的域列表，如 ["ORDER", "FINDING_REQUEST"]，null 监听全部
     */
    public void connect(List<String> domains) {
        if (running.get()) {
            disconnect();
        }
        this.domains = domains;
        running.set(true);
        workerThread = new Thread(this::streamLoop, "SSE-Worker");
        workerThread.setDaemon(true);
        workerThread.start();
        System.out.println("[SSE] 连接已启动");
    }

    /** 断开连接 */
    public void disconnect() {
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
        System.out.println("[SSE] 已断开");
    }

    /**
     * 注册事件监听
     * @param domain 域名（如 "ORDER"），或 "*" 监听所有
     */
    public void on(String domain, EventHandler handler) {
        listeners.computeIfAbsent(domain, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    /** 移除某个域的所有监听 */
    public void off(String domain) {
        listeners.remove(domain);
    }

    /** 清除所有监听 */
    public void offAll() {
        listeners.clear();
    }

    // ==================== 内部实现 ====================

    private void streamLoop() {
        while (running.get()) {
            try {
                doStream();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[SSE] 连接异常: " + e.getMessage());
            }
            if (!running.get()) break;
            // 自动重连
            try {
                System.out.println("[SSE] " + RECONNECT_DELAY_MS + "ms 后重连...");
                Thread.sleep(RECONNECT_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void doStream() throws Exception {
        SessionContext ctx = SessionContext.getInstance();
        if (!ctx.hasSession()) {
            HttpService.handshake();
        }

        SecretKey key = ctx.getSharedKey();
        String sessionId = ctx.getSessionId();

        // 构建 URL
        StringBuilder pathBuilder = new StringBuilder("/events/stream");
        if (domains != null && !domains.isEmpty()) {
            StringJoiner joiner = new StringJoiner("&", "?", "");
            for (String d : domains) {
                joiner.add("domain=" + d);
            }
            pathBuilder.append(joiner);
        }
        String path = pathBuilder.toString();

        // 签名
        String timestamp = String.valueOf(System.currentTimeMillis());
        String message = "GET\n" + path + "\n" + timestamp + "\n";
        String signature = CryptoUtil.hmacSign(key, message);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Accept", "text/event-stream")
                .header("X-Session-Id", sessionId)
                .header("X-Timestamp", timestamp)
                .header("X-Signature", signature)
                .GET();

        if (ctx.getJwtToken() != null) {
            reqBuilder.header("Authorization", "Bearer " + ctx.getJwtToken());
        }

        // 使用 InputStream 流式读取 SSE
        HttpResponse<java.io.InputStream> resp = client.send(
                reqBuilder.build(),
                HttpResponse.BodyHandlers.ofInputStream());

        if (resp.statusCode() != 200) {
            throw new RuntimeException("SSE 连接失败: HTTP " + resp.statusCode());
        }
        System.out.println("[SSE] 连接已建立");

        try (var is = resp.body();
             var reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {

            StringBuilder dataBuilder = new StringBuilder();
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    dataBuilder.append(line.substring(5).trim());
                } else if (line.isEmpty() && dataBuilder.length() > 0) {
                    // 一条完整的 SSE 消息
                    String data = dataBuilder.toString();
                    dataBuilder.setLength(0);
                    parseAndDispatch(data);
                }
            }
        }
        System.out.println("[SSE] 流结束");
    }

    private void parseAndDispatch(String data) {
        try {
            JsonNode json = MAPPER.readTree(data);
            String domain = json.path("domain").asText("");
            String action = json.path("action").asText("");
            String resourceId = json.path("resourceId").asText("");

            // 在 JavaFX 线程上回调
            Platform.runLater(() -> dispatch(domain, action, resourceId));
        } catch (Exception e) {
            System.err.println("[SSE] 消息解析失败: " + e.getMessage() + " data=" + data);
        }
    }

    private void dispatch(String domain, String action, String resourceId) {
        // 精确匹配
        CopyOnWriteArrayList<EventHandler> exact = listeners.get(domain);
        if (exact != null) {
            for (EventHandler h : exact) {
                try { h.onEvent(domain, action, resourceId); } catch (Exception e) { e.printStackTrace(); }
            }
        }
        // 通配
        CopyOnWriteArrayList<EventHandler> wildcard = listeners.get("*");
        if (wildcard != null) {
            for (EventHandler h : wildcard) {
                try { h.onEvent(domain, action, resourceId); } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }
}
