package javafx_demo.entity;

import java.util.Map;

public class MaintenanceRecord {
    private Long id;
    private String bossName;      // 老板名称
    private String wechatId;      // 微信号
    private String addTime;       // 添加时间
    private String maintainLog;   // 维护记录内容

    public MaintenanceRecord() {}

    public static MaintenanceRecord fromMap(Map<String, Object> m) {
        MaintenanceRecord r = new MaintenanceRecord();
        Object idVal = m.get("id");
        if (idVal instanceof Number n) r.id = n.longValue();
        r.bossName = str(m.get("bossName"));
        r.wechatId = str(m.get("wechatId"));
        r.addTime = str(m.get("addTime"));
        r.maintainLog = str(m.get("maintainLog"));
        return r;
    }

    private static String str(Object v) { return v != null ? v.toString() : null; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBossName() { return bossName; }
    public void setBossName(String bossName) { this.bossName = bossName; }
    public String getWechatId() { return wechatId; }
    public void setWechatId(String wechatId) { this.wechatId = wechatId; }
    public String getAddTime() { return addTime; }
    public void setAddTime(String addTime) { this.addTime = addTime; }
    public String getMaintainLog() { return maintainLog; }
    public void setMaintainLog(String maintainLog) { this.maintainLog = maintainLog; }
}
