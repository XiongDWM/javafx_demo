package javafx_demo.entity;

import java.util.Map;

public class FindingRequest {
    private Long id;
    private Boolean man;          // true=男单, false=女单, null=不限
    private String description;   // 需求描述
    private String requestedAt;   // 请求时间
    private Boolean fulfilled;    // null=撤销, true=已找到, false=寻找中

    public FindingRequest() {}

    public static FindingRequest fromMap(Map<String, Object> m) {
        FindingRequest r = new FindingRequest();
        Object idVal = m.get("id");
        if (idVal instanceof Number n) r.id = n.longValue();
        Object manVal = m.get("man");
        r.man = manVal instanceof Boolean b ? b : null;
        r.description = str(m.get("description"));
        r.requestedAt = str(m.get("requestedAt"));
        Object fulVal = m.get("fulfilled");
        r.fulfilled = fulVal instanceof Boolean b ? b : null;
        return r;
    }

    private static String str(Object v) { return v != null ? v.toString() : null; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Boolean getMan() { return man; }
    public void setMan(Boolean man) { this.man = man; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequestedAt() { return requestedAt; }
    public void setRequestedAt(String requestedAt) { this.requestedAt = requestedAt; }
    public Boolean getFulfilled() { return fulfilled; }
    public void setFulfilled(Boolean fulfilled) { this.fulfilled = fulfilled; }
}
