package javafx_demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx_demo.utils.ConfigManager;
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
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("palId", palId);
        body.put("orderId", orderId);
        if (picStart != null && !picStart.isEmpty()) body.put("picStart", picStart);
        String resp = retryOnKeyExpired(() -> HttpService.post("/order/work", toJson(body)));
        checkSuccess(resp);
    }

    /**
     * 续单
     */
    public static void continueOrder(String orderId, double price, double amount,
                                     String unitType, String additionalPic, String continuePic) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("orderId", orderId);
        body.put("price", String.valueOf(price));
        body.put("amount", String.valueOf(amount));
        body.put("unitType", unitType);
        if (additionalPic != null && !additionalPic.isEmpty()) body.put("additionalPic", additionalPic);
        if (continuePic != null && !continuePic.isEmpty()) body.put("continuePic", continuePic);
        String resp = retryOnKeyExpired(() -> HttpService.post("/order/continue", toJson(body)));
        checkSuccess(resp);
    }

    /**
     * 结束工单
     * @param picEnd oss 返回的文件 ID
     */
    public static void closeOrder(String orderId, String picEnd) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("orderId", orderId);
        body.put("picString", picEnd);
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
    public static void submitFindingRequest(long palId, Boolean man,
                                              String gameType, String rank) throws Exception {
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        if (man != null) body.put("man", man);
        // body.put("description", description); --- IGNORE ---
        if (gameType != null && !gameType.isEmpty()) body.put("gameType", gameType);
        if (rank != null && !rank.isEmpty()) body.put("rank", rank);
        ObjectNode palObj = JsonNodeFactory.instance.objectNode();
        palObj.put("id", palId);
        body.set("palworld", palObj);

        String resp = retryOnKeyExpired(() -> HttpService.post("/finding/submit", MAPPER.writeValueAsString(body)));
        checkSuccess(resp);
    }

    /**
     * 撤销找单请求
     */
    public static void cancelFindingRequest(long requestId) throws Exception {
        String resp = retryOnKeyExpired(
                () -> HttpService.post("/finding/cancel", String.valueOf(requestId)));
        checkSuccess(resp);
    }

    /**
     * 查询找单请求列表（不分页，全量返回）
     */
    public static List<Map<String, Object>> queryFindingList() throws Exception {
        String resp = retryOnKeyExpired(() -> HttpService.get("/finding/list"));
        JsonNode json = MAPPER.readTree(resp);
        if (!json.path("success").asBoolean()) {
            throw new RuntimeException("查询找单列表失败");
        }
        JsonNode data = json.path("data");
        List<Map<String, Object>> result = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode el : data) {
                result.add(jsonNodeToMap(el));
            }
        }
        return result;
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

    // ==================== 二手单状态更新 ====================

    /**
     * 更新二手单状态
     * @param orderId 工单ID
     * @param picStart 上传的图片 ID（放在 additionalPic 字段）
     */
    public static void updateSecondHandStatus(String orderId, String picStart) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("orderId", orderId);
        picStart = (picStart != null && !picStart.isEmpty()) ? picStart : "";

        body.put("additionalPic", picStart); // 二手单状态更新时，前端上传的图片放在 additionalPic 字段
        String resp = retryOnKeyExpired(() -> HttpService.post("/order/secondHandStatus", toJson(body))); // 这里使用continue 
        checkSuccess(resp);
    }

    // ==================== 存单 ====================

    /**
     * 分页查询存单列表
     */
    public static PageResult queryBookOrders(int page, int size, Map<String, String> filters) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pageNumber", page);
        body.put("pageSize", size);
        if (filters != null && !filters.isEmpty()) {
            body.put("filters", filters);
        }
        String resp = retryOnKeyExpired(() -> HttpService.post("/bookOrder/list", toJson(body)));
        return parsePageResult(resp);
    }

    /**
     * 从存单创建 order（开始）
     */
    public static void startBookOrder(long orderId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderId", orderId);
        String resp = retryOnKeyExpired(
                () -> HttpService.post("/bookOrder/starting", toJson(body)));
        checkSuccess(resp);
    }

    /**
     * 新增存单
     */
    public static void createBookOrder(String customer, String customerId, String details,
                                       String picProvence, double amount, double price) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customer", customer);
        body.put("customerId", customerId);
        if (details != null && !details.isEmpty()) body.put("details", details);
        if (picProvence != null && !picProvence.isEmpty()) body.put("picProvence", picProvence);
        body.put("amount", amount);
        body.put("price", price);
        String resp = retryOnKeyExpired(
                () -> HttpService.post("/bookOrder/create", toJson(body)));
        checkSuccess(resp);
    }

    // TODO: 关闭存单接口 — /bookOrder/close
    // TODO: 上传图片到存单接口 — /bookOrder/uploadPic
    // TODO: 续存接口 — /bookOrder/renew

    // ==================== 请假记录 ====================

    /**
     * 分页查询请假记录
     */
    public static PageResult queryLeaveRecords(int page, int size) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pageNumber", page);
        body.put("pageSize", size);
        String resp = retryOnKeyExpired(() -> HttpService.post("/leave/list", toJson(body)));
        return parsePageResult(resp);
    }

    /**
     * 新增请假
     */
    public static void createLeaveRecord(String type, String reason, String startDate, String endDate) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("reason", reason);
        body.put("startDate", startDate);
        body.put("endDate", endDate);
        String resp = retryOnKeyExpired(() -> HttpService.post("/leave/create", toJson(body)));
        checkSuccess(resp);
    }

    // ==================== 工资预支 ====================

    /**
     * 分页查询工资预支记录
     */
    public static PageResult querySalaryAdvances(int page, int size) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pageNumber", page);
        body.put("pageSize", size);
        String resp = retryOnKeyExpired(() -> HttpService.post("/salaryAdvance/list", toJson(body)));
        return parsePageResult(resp);
    }

    /**
     * 新增工资预支
     */
    public static void createSalaryAdvance(String reason, double amount) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reason", reason);
        body.put("amount", amount);
        String resp = retryOnKeyExpired(() -> HttpService.post("/salaryAdvance/create", toJson(body)));
        checkSuccess(resp);
    }

    // ==================== 维护记录 ====================

    /**
     * 分页查询维护记录
     */
    public static PageResult queryMaintenanceRecords(int page, int size) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pageNumber", page);
        body.put("pageSize", size);
        String resp = retryOnKeyExpired(() -> HttpService.post("/maintenance/list", toJson(body)));
        return parsePageResult(resp);
    }

    /**
     * 追加维护记录内容
     */
    public static void appendMaintenanceLog(long recordId, String content) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", recordId);
        body.put("content", content);
        String resp = retryOnKeyExpired(() -> HttpService.post("/maintenance/appendLog", toJson(body)));
        checkSuccess(resp);
    }

    // ==================== 图片预览 ====================

    /**
     * 获取图片预览 URL（不带认证，仅备用）
     */
    public static String getImagePreviewUrl(String fileId) {
        return ConfigManager.getInstance().getServerBaseUrl() + "/oss/preview/" + fileId;
    }

    /**
     * 通过认证通道下载图片字节
     */
    public static byte[] downloadImage(String fileId) throws Exception {
        return HttpService.getBytes("/oss/preview/" + fileId);
    }

    // ==================== 统计 ====================

    /**
     * 获取用户当期收入统计（POST /statistic/user-summary）
     * 返回字段：totalIncome, totalCount, from, to
     *   + firstIncome/firstAuth/firstRej/firstPend/firstCount
     *   + renewalIncome/renewalAuth/renewalRej/renewalPend/renewalCount
     *   + otherIncome/otherAuth/otherRej/otherPend/otherCount
     */
    public static Map<String, Object> getUserIncomeStatistic(long userId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", userId);
        String resp = retryOnKeyExpired(() -> HttpService.post("/statistic/user-summary", toJson(body)));
        JsonNode json = MAPPER.readTree(resp);
        if (!json.path("success").asBoolean()) {
            throw new RuntimeException("获取统计失败");
        }
        JsonNode data = json.path("data");
        if (data.isMissingNode()) return new LinkedHashMap<>();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalIncome", data.path("totalIncome").asDouble());
        result.put("totalCount",  data.path("totalCount").asDouble());
        result.put("from", data.path("from").asText(""));
        result.put("to",   data.path("to").asText(""));
        String[][] categories = {
            {"first",   "unrepeatedIncome"},
            {"renewal", "repeatedIncome"},
            {"other",   "othersIncome"}
        };
        for (String[] cat : categories) {
            String prefix = cat[0];
            JsonNode n = data.path(cat[1]);
            result.put(prefix + "Income", n.path("totalIncome").asDouble());
            result.put(prefix + "Auth",   n.path("authorizedIncome").asDouble());
            result.put(prefix + "Rej",    n.path("unauthorizedIncome").asDouble());
            result.put(prefix + "Pend",   n.path("pendingIncome").asDouble());
            result.put(prefix + "Count",  n.path("count").asDouble());
        }
        return result;
    }

    /**
     * 获取所有打手当期排行（GET /statistic/ranking）
     */
    public static List<Map<String, Object>> getRanking() throws Exception {
        String resp = retryOnKeyExpired(() -> HttpService.get("/statistic/ranking"));
        JsonNode json = MAPPER.readTree(resp);
        if (!json.path("success").asBoolean()) throw new RuntimeException("获取排行失败");
        JsonNode data = json.path("data");
        List<Map<String, Object>> result = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode item : data) result.add(jsonNodeToMap(item));
        }
        return result;
    }

    /**
     * 获取指定打手近7天收入趋势（GET /statistic/weekly-trend）
     */
    public static List<Map<String, Object>> getWeeklyTrend(long userId) throws Exception {
        String resp = retryOnKeyExpired(() -> HttpService.get("/statistic/weekly-trend?userId=" + userId));
        JsonNode json = MAPPER.readTree(resp);
        if (!json.path("success").asBoolean()) throw new RuntimeException("获取趋势失败");
        JsonNode days = json.path("data").path("days");
        List<Map<String, Object>> result = new ArrayList<>();
        if (days.isArray()) {
            for (JsonNode day : days) result.add(jsonNodeToMap(day));
        }
        return result;
    }

    // ==================== 心跳 ====================

    /**
     * 心跳保活 — 触发后端 JWT 滑动续期 + UserActivityTracker.touch()
     * 即使后端没有 /user/heartbeat 端点（返回 404），JWT filter 仍会处理 token 续期
     */
    public static void heartbeat() throws Exception {
        retryOnKeyExpired(() -> HttpService.get("/user/heartbeat"));
    }

    // ==================== 用户状态 ====================

    /**
     * 获取当前用户在后端的真实状态
     * @return 状态枚举名称，如 "ONLINE", "BUSY", "ACTIVE" 等
     */
    public static String getUserStatus() throws Exception {
        String resp = retryOnKeyExpired(() -> HttpService.get("/user/me"));
        JsonNode json = MAPPER.readTree(resp);
        if (!json.path("success").asBoolean()) {
            throw new RuntimeException("获取用户状态失败");
        }
        return json.path("data").asText("ONLINE");
    }

    // ==================== 登出 ====================

    /**
     * 通知后端登出（设置用户状态 OFFLINE）
     */
    public static void logout() throws Exception {
        retryOnKeyExpired(() -> HttpService.post("/user/logout", "{}"));
    }

    // ==================== 单条订单查询 ======================================

    /**
     * 通过 /order/list 按 orderId 过滤获取单条订单（不含 sections）
     */
    public static Map<String, Object> getOrderDetail(String orderId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pageNumber", 0);
        body.put("pageSize", 1);
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("orderId", orderId);
        body.put("filters", filters);

        String resp = retryOnKeyExpired(() -> HttpService.post("/order/list", toJson(body)));
        PageResult pr = parsePageResult(resp);
        return pr.content.isEmpty() ? null : pr.content.get(0);
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
