package javafx_demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx_demo.utils.SessionContext;

import java.io.File;
import java.util.*;

/**
 * API 服务 — 封装所有后端接口调用
 */
public class ApiService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static String toJson(Map<String, ?> data) throws Exception {
        return MAPPER.writeValueAsString(data);
    }

    // ==================== 登录 ====================

    /**
     * 打手端登录
     * @return JWT Token
     */
    public static String login(String username, String password) throws Exception {
        SessionContext ctx = SessionContext.getInstance();
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("softwareCode", ctx.getSoftwareCode());

        String resp = retryOnKeyExpired(() -> HttpService.post("/user/pal/login", toJson(body)));
        JsonNode json = MAPPER.readTree(resp);
        if (!json.path("success").asBoolean()) {
            String msg = json.has("data") ? json.path("data").asText() : "登录失败";
            throw new RuntimeException(msg);
        }
        return json.path("data").asText(""); // JWT token
    }

    // ==================== 工单 ====================

    /**
     * 查询今日自己的工单
     */
    public static List<Map<String, Object>> getTodayOrders(long userId) throws Exception {
        PageResult pr = queryOrders(userId, true, 0, 100);
        return pr.content;
    }

    /**
     * 查询所有自己的工单 (分页)
     */
    public static PageResult queryOrders(long userId, boolean todayOnly, int page, int size) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pageNumber", page);
        body.put("pageSize", size);
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("userId", String.valueOf(userId));
        if (todayOnly) filters.put("todayOnly", "true");
        body.put("filters", filters);

        String resp = retryOnKeyExpired(() -> HttpService.post("/order/list", toJson(body)));
        return parsePageResult(resp);
    }

    /**
     * 接单开工
     */
    public static void acceptOrder(long palId, String orderId, String picStart) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("palId", String.valueOf(palId));
        body.put("orderId", orderId);
        if (picStart != null && !picStart.isEmpty()) body.put("picStart", picStart);
        String resp = retryOnKeyExpired(() -> HttpService.post("/order/work", toJson(body)));
        checkSuccess(resp);
    }

    /**
     * 续单
     */
    public static void continueOrder(String orderId, double price, double amount,
                                     String unitType, String additionalPic) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("orderId", orderId);
        body.put("price", String.valueOf(price));
        body.put("amount", String.valueOf(amount));
        body.put("unitType", unitType);
        if (additionalPic != null && !additionalPic.isEmpty()) body.put("additionalPic", additionalPic);
        String resp = retryOnKeyExpired(() -> HttpService.post("/order/continue", toJson(body)));
        checkSuccess(resp);
    }

    /**
     * 结束工单
     */
    public static void closeOrder(String orderId, String picString) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("orderId", orderId);
        body.put("picString", picString);
        String resp = retryOnKeyExpired(() -> HttpService.post("/order/close", toJson(body)));
        checkSuccess(resp);
    }

    // ==================== 状态变更 ====================

    /**
     * 改变用户在线状态
     * @param status ACTIVE / HANGING / OFFLINE
     */
    public static void changeStatus(long userId, String status) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("userId", String.valueOf(userId));
        body.put("status", status);
        String resp = retryOnKeyExpired(() -> HttpService.post("/user/status", toJson(body)));
        checkSuccess(resp);
    }

    // ==================== 找单请求 ====================

    /**
     * 提交找单请求
     */
    public static void submitFindingRequest(long palId, boolean man, String description) throws Exception {
        // 构建 FindingRequest JSON
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        body.put("man", man);
        body.put("description", description);
        ObjectNode palObj = JsonNodeFactory.instance.objectNode();
        palObj.put("id", palId);
        body.set("palworld", palObj);

        String resp = retryOnKeyExpired(() -> HttpService.post("/finding/submit", MAPPER.writeValueAsString(body)));
        checkSuccess(resp);
    }

    // ==================== 文件上传 ====================

    /**
     * 上传图片
     * @return 文件 ID 字符串
     */
    public static String uploadImage(File file) throws Exception {
        String resp = HttpService.uploadFile(file);
        JsonNode json = MAPPER.readTree(resp);
        if (!json.path("success").asBoolean()) {
            throw new RuntimeException("上传失败");
        }
        // data 是 FileLog 对象，取 id
        return json.path("data").path("id").asText("");
    }

    // ==================== 统计 ====================

    /**
     * 获取用户统计摘要
     * @return {totalOrders: int, totalIncome: double}
     */
    public static Map<String, Object> getUserSummary(long userId) throws Exception {
        String resp = retryOnKeyExpired(
                () -> HttpService.get("/statistic/user-summary?userId=" + userId));
        JsonNode json = MAPPER.readTree(resp);
        if (!json.path("success").asBoolean()) {
            throw new RuntimeException("获取统计失败");
        }
        JsonNode data = json.path("data");
        if (data.isMissingNode()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOrders", data.path("totalOrders").asInt());
        result.put("totalIncome", data.path("totalIncome").asDouble());
        return result;
    }

    // ==================== 工具方法 ====================

    private static void checkSuccess(String resp) {
        try {
            JsonNode json = MAPPER.readTree(resp);
            if (!json.path("success").asBoolean()) {
                String msg = json.has("data") ? json.path("data").asText() : "操作失败";
                throw new RuntimeException(msg);
            }
        } catch (Exception e) {
            throw new RuntimeException("操作失败: " + e.getMessage());
        }
    }

    /** 密钥过期自动重试一次 */
    private static String retryOnKeyExpired(ApiCall call) throws Exception {
        try {
            return call.execute();
        } catch (RuntimeException e) {
            if ("RETRY".equals(e.getMessage())) {
                return call.execute();
            }
            throw e;
        }
    }

    @FunctionalInterface
    private interface ApiCall {
        String execute() throws Exception;
    }

    // ==================== 分页结果解析 ====================

    public static class PageResult {
        public List<Map<String, Object>> content = new ArrayList<>();
        public int totalElements;
        public int totalPages;
        public int number; // current page (0-based)
        public int size;
    }

    private static PageResult parsePageResult(String resp) {
        try {
            JsonNode json = MAPPER.readTree(resp);
            if (!json.path("success").asBoolean()) {
                throw new RuntimeException("查询失败");
            }
            JsonNode data = json.path("data");
            if (data.isMissingNode()) {
                return new PageResult();
            }
            PageResult pr = new PageResult();
            pr.totalElements = data.path("totalElements").asInt(0);
            pr.totalPages = data.path("totalPages").asInt(0);
            pr.number = data.path("number").asInt(0);
            pr.size = data.path("size").asInt(0);

            JsonNode arr = data.path("content");
            if (arr.isArray()) {
                for (JsonNode el : arr) {
                    pr.content.add(jsonNodeToMap(el));
                }
            }
            return pr;
        } catch (Exception e) {
            throw new RuntimeException("解析分页结果失败: " + e.getMessage());
        }
    }

    private static Map<String, Object> jsonNodeToMap(JsonNode node) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                JsonNode val = entry.getValue();
                String key = entry.getKey();
                if (val.isNull()) {
                    map.put(key, null);
                } else if (val.isObject()) {
                    map.put(key, jsonNodeToMap(val));
                } else if (val.isArray()) {
                    List<Object> list = new ArrayList<>();
                    for (JsonNode item : val) {
                        if (item.isObject()) {
                            list.add(jsonNodeToMap(item));
                        } else if (item.isNull()) {
                            list.add(null);
                        } else if (item.isNumber()) {
                            list.add(item.asDouble());
                        } else if (item.isBoolean()) {
                            list.add(item.asBoolean());
                        } else {
                            list.add(item.asText());
                        }
                    }
                    map.put(key, list);
                } else if (val.isNumber()) {
                    map.put(key, val.asDouble());
                } else if (val.isBoolean()) {
                    map.put(key, val.asBoolean());
                } else {
                    map.put(key, val.asText());
                }
            });
        }
        return map;
    }
}
