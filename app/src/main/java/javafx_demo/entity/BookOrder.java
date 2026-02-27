package javafx_demo.entity;

import java.util.Map;

/**
 * 存单实体 POJO — 与后端 BookOrder 对齐
 */
public class BookOrder {
    private Long id;
    private String customer;      // 客户备注
    private String customerId;    // 客户id 微信号
    private String createTime;    // 创建时间
    private String details;       // 客户描述
    private double amount;        // 存单数量
    private double remaining;     // 剩余时间
    private double price;         // 存单总价
    private String picProvence;   // 图片证明来源(oss文件ID,逗号分隔)
    private Long pid;             // 打手id

    public BookOrder() {}

    // @SuppressWarnings("unchecked")
    public static BookOrder fromMap(Map<String, Object> m) {
        BookOrder o = new BookOrder();
        Object idVal = m.get("id");
        if (idVal instanceof Number n) o.id = n.longValue();
        o.customer = str(m.get("customer"));
        o.customerId = str(m.get("customerId"));
        o.createTime = str(m.get("createTime"));
        o.details = str(m.get("details"));
        o.amount = num(m.get("amount"));
        o.remaining = num(m.get("remaining"));
        o.price = num(m.get("price"));
        o.picProvence = str(m.get("picProvence"));
        Object pidVal = m.get("pid");
        if (pidVal instanceof Number n) o.pid = n.longValue();
        return o;
    }

    private static String str(Object v) { return v != null ? v.toString() : null; }
    private static double num(Object v) { return v instanceof Number n ? n.doubleValue() : 0; }

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getRemaining() { return remaining; }
    public void setRemaining(double remaining) { this.remaining = remaining; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getPicProvence() { return picProvence; }
    public void setPicProvence(String picProvence) { this.picProvence = picProvence; }

    public Long getPid() { return pid; }
    public void setPid(Long pid) { this.pid = pid; }
}
