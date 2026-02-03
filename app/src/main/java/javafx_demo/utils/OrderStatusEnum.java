package javafx_demo.utils;

/**
 * @process: 客服派单后状态是 confirm -> 打手接单后是 processing 
 *                      -> 打手完成是 completed -> 如果客户续单，打手发起状态是pending -> 返回客服云平台确认（打手自己负责，可以等到后台确认后再打，也可以直接打）
 * 
 * 
 */
public enum OrderStatusEnum {
    PENDING("续单待确认", false), // 通常是续单
    CONFIRMED("已续单", false),
    PROCESSING("进行中", true),
    FAILURE("炸单", true),
    RENT_REQUESTED("租号发起", false),
    RENTED("已租号", false),
    COMPLETED("已完成", true);
    private final String text;
    private final boolean changeable;

    private OrderStatusEnum(String text, boolean changeable) {
        this.text = text;
        this.changeable = changeable;
    }
    
    public String getText() {
        return text;
    }
    public boolean isChangeable() {
        return changeable;
    }

}
