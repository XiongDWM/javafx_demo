package javafx_demo.entity;

import java.util.Map;

/**
 * 工单实体 POJO — 与后端 Order 对齐
 */
public class Order {
    private String orderId;
    private String issueDate;     // yyyy-MM-dd HH:mm:ss
    private String endAt;
    private String customer;
    private String customerEvaluate;
    private String resource;
    private String type;          // SELF_B, SELF_G, BOOKED, SECOND_HAND, THIRD_PARTY, ...
    private String status;        // PENDING, IN_PROGRESS, COMPLETED, ...
    private double lowIncome;
    private double amount;
    private String unitType;      // HOUR, BATTLE, DAY
    private Long userId;
    private String toWhom;
    private String picStart;
    private String picEnd;
    private String additionalPic;
    private String secondHandStatus;

    // palworld (User 对象, @JsonManagedReference → 会在 JSON 中)
    private Map<String, Object> palworld;

    public Order() {}

    @SuppressWarnings("unchecked")
    public static Order fromMap(Map<String, Object> m) {
        Order o = new Order();
        o.orderId = str(m.get("orderId"));
        o.issueDate = str(m.get("issueDate"));
        o.endAt = str(m.get("endAt"));
        o.customer = str(m.get("customer"));
        o.customerEvaluate = str(m.get("customerEvaluate"));
        o.resource = str(m.get("resource"));
        o.type = str(m.get("type"));
        o.status = str(m.get("status"));
        o.lowIncome = num(m.get("lowIncome"));
        o.amount = num(m.get("amount"));
        o.unitType = str(m.get("unitType"));
        o.toWhom = str(m.get("toWhom"));
        o.picStart = str(m.get("picStart"));
        o.picEnd = str(m.get("picEnd"));
        o.additionalPic = str(m.get("additionalPic"));
        o.secondHandStatus = str(m.get("secondHandStatus"));
        Object uid = m.get("userId");
        if (uid instanceof Number n) o.userId = n.longValue();
        if (m.get("palworld") instanceof Map<?,?> p) o.palworld = (Map<String, Object>) p;
        return o;
    }

    private static String str(Object v) { return v != null ? v.toString() : null; }
    private static double num(Object v) { return v instanceof Number n ? n.doubleValue() : 0; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }

    public String getEndAt() { return endAt; }
    public void setEndAt(String endAt) { this.endAt = endAt; }

    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }

    public String getCustomerEvaluate() { return customerEvaluate; }
    public void setCustomerEvaluate(String customerEvaluate) { this.customerEvaluate = customerEvaluate; }

    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getLowIncome() { return lowIncome; }
    public void setLowIncome(double lowIncome) { this.lowIncome = lowIncome; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getToWhom() { return toWhom; }
    public void setToWhom(String toWhom) { this.toWhom = toWhom; }

    public String getPicStart() { return picStart; }
    public void setPicStart(String picStart) { this.picStart = picStart; }

    public String getPicEnd() { return picEnd; }
    public void setPicEnd(String picEnd) { this.picEnd = picEnd; }

    public String getAdditionalPic() { return additionalPic; }
    public void setAdditionalPic(String additionalPic) { this.additionalPic = additionalPic; }

    public String getSecondHandStatus() { return secondHandStatus; }
    public void setSecondHandStatus(String secondHandStatus) { this.secondHandStatus = secondHandStatus; }

    public Map<String, Object> getPalworld() { return palworld; }
    public void setPalworld(Map<String, Object> palworld) { this.palworld = palworld; }


    public String getTypeText() {
        if (type == null) return "";
        return switch (type) {
            case "SELF_B" -> "自接男单";
            case "SELF_G" -> "自接女单/Ai";
            case "BOOKED" -> "预约单";
            case "COMPELETING" -> "预约单补单";
            case "SECOND_HAND" -> "二手单";
            case "THIRD_PARTY" -> "甩单";
            case "LONG_TERM" -> "长时间订单";
            default -> type;
        };
    }

    public String getStatusText() {
        if (status == null) return "";
        return switch (status) {
            case "PENDING" -> "待接单";
            case "IN_PROGRESS" -> "进行中";
            case "THIRD_PARTY_WAITING" -> "甩单待接单";
            case "THIRD_PARTY_TAKEN" -> "甩单被接单";
            case "THIRD_PARTY_TAKEN_PROCESS_DONE" -> "二手单完成";
            case "THIRD_PARTY_SETTLEMENT_PULL" -> "二手单等待结算";
            case "THIRD_PARTY_SETTLED" -> "二手单已结算";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    public String getUnitTypeText() {
        if (unitType == null) return "";
        return switch (unitType) {
            case "HOUR" -> "小时";
            case "BATTLE" -> "局";
            case "DAY" -> "天";
            default -> unitType;
        };
    }

    public boolean isSecondHand() {
        return "SECOND_HAND".equals(type) || "THIRD_PARTY".equals(type);
    }
}
