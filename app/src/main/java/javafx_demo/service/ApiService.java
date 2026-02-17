package javafx_demo.service;

import org.json.JSONArray;
import org.json.JSONObject;
import javafx_demo.utils.SessionContext;

import java.io.File;
import java.util.*;

/**
 * API 服务 — 封装所有后端接口调用
 */
public class ApiService {

    private static String toJson(Map<String, ?> data) {
        return new JSONObject(data).toString();
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
        JSONObject json = new JSONObject(resp);
        if (!json.optBoolean("success")) {
            String msg = json.has("data") ? String.valueOf(json.opt("data")) : "登录失败";
            throw new RuntimeException(msg);
        }
        return json.optString("data", ""); // JWT token
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
        JSONObject body = new JSONObject();
        body.put("man", man);
        body.put("description", description);
        JSONObject palObj = new JSONObject();
        palObj.put("id", palId);
        body.put("palworld", palObj);

        String resp = retryOnKeyExpired(() -> HttpService.post("/finding/submit", body.toString()));
        checkSuccess(resp);
    }

    // ==================== 文件上传 ====================

    /**
     * 上传图片
     * @return 文件 ID 字符串
     */
    public static String uploadImage(File file) throws Exception {
        String resp = HttpService.uploadFile(file);
        JSONObject json = new JSONObject(resp);
        if (!json.optBoolean("success")) {
            throw new RuntimeException("上传失败");
        }
        // data 是 FileLog 对象，取 id
        JSONObject data = json.optJSONObject("data");
        return data == null ? "" : data.optString("id", "");
    }

    // ==================== 统计 ====================

    /**
     * 获取用户统计摘要
     * @return {totalOrders: int, totalIncome: double}
     */
    public static Map<String, Object> getUserSummary(long userId) throws Exception {
        String resp = retryOnKeyExpired(
                () -> HttpService.get("/statistic/user-summary?userId=" + userId));
        JSONObject json = new JSONObject(resp);
        if (!json.optBoolean("success")) {
            throw new RuntimeException("获取统计失败");
        }
        JSONObject data = json.optJSONObject("data");
        if (data == null) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOrders", data.optInt("totalOrders"));
        result.put("totalIncome", data.optDouble("totalIncome"));
        return result;
    }

    // ==================== 工具方法 ====================

    private static void checkSuccess(String resp) {
        JSONObject json = new JSONObject(resp);
        if (!json.optBoolean("success")) {
            String msg = json.has("data") ? String.valueOf(json.opt("data")) : "操作失败";
            throw new RuntimeException(msg);
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
        JSONObject json = new JSONObject(resp);
        if (!json.optBoolean("success")) {
            throw new RuntimeException("查询失败");
        }
        JSONObject data = json.optJSONObject("data");
        if (data == null) {
            return new PageResult();
        }
        PageResult pr = new PageResult();
        pr.totalElements = data.optInt("totalElements", 0);
        pr.totalPages = data.optInt("totalPages", 0);
        pr.number = data.optInt("number", 0);
        pr.size = data.optInt("size", 0);

        JSONArray arr = data.optJSONArray("content");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) {
                    pr.content.add(jsonObjectToMap(obj));
                }
            }
        }
        return pr;
    }

    private static Map<String, Object> jsonObjectToMap(JSONObject obj) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : obj.keySet()) {
            Object val = obj.get(key);
            if (val == JSONObject.NULL) {
                map.put(key, null);
            } else if (val instanceof JSONObject) {
                map.put(key, jsonObjectToMap((JSONObject) val));
            } else if (val instanceof JSONArray) {
                JSONArray arr = (JSONArray) val;
                List<Object> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    Object item = arr.get(i);
                    if (item instanceof JSONObject) {
                        list.add(jsonObjectToMap((JSONObject) item));
                    } else if (item != JSONObject.NULL) {
                        list.add(item);
                    } else {
                        list.add(null);
                    }
                }
                map.put(key, list);
            } else {
                map.put(key, val);
            }
        }
        return map;
    }
}
