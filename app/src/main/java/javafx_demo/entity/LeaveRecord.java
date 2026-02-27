package javafx_demo.entity;

import java.util.Map;

public class LeaveRecord {
    private Long id;
    private String applyTime;   // 申请时间
    private String endTime;     // 截止时间
    private String status;      // 状态
    private String type;        // 类型: 事假/休假/病假
    private String reason;      // 事由

    public LeaveRecord() {}

    public static LeaveRecord fromMap(Map<String, Object> m) {
        LeaveRecord r = new LeaveRecord();
        Object idVal = m.get("id");
        if (idVal instanceof Number n) r.id = n.longValue();
        r.applyTime = str(m.get("applyTime"));
        r.endTime = str(m.get("endTime"));
        r.status = str(m.get("status"));
        r.type = str(m.get("type"));
        r.reason = str(m.get("reason"));
        return r;
    }

    private static String str(Object v) { return v != null ? v.toString() : null; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApplyTime() { return applyTime; }
    public void setApplyTime(String applyTime) { this.applyTime = applyTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
