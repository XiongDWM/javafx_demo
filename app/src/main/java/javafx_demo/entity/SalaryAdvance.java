package javafx_demo.entity;

import java.util.Map;

public class SalaryAdvance {
    private Long id;
    private String name;        // 姓名
    private double amount;      // 预支金额
    private String status;      // 状态
    private String reason;      // 事由

    public SalaryAdvance() {}

    public static SalaryAdvance fromMap(Map<String, Object> m) {
        SalaryAdvance r = new SalaryAdvance();
        Object idVal = m.get("id");
        if (idVal instanceof Number n) r.id = n.longValue();
        r.name = str(m.get("name"));
        r.amount = num(m.get("amount"));
        r.status = str(m.get("status"));
        r.reason = str(m.get("reason"));
        return r;
    }

    private static String str(Object v) { return v != null ? v.toString() : null; }
    private static double num(Object v) { return v instanceof Number n ? n.doubleValue() : 0; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
