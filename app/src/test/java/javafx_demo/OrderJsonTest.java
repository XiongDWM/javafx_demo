package javafx_demo;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.*;
import javafx.beans.property.*;
import javafx_demo.entity.Order;
import javafx_demo.utils.OrderStatusEnum;

public class OrderJsonTest {
    
    public static void main(String[] args) {
        System.out.println("========== 开始测试 JSON 序列化 ==========");
        
        try {
            // 1. 读取 JSON 文件
            System.out.println("\n1. 读取 order_sample.json 文件...");
            InputStream inputStream = OrderJsonTest.class.getResourceAsStream("/order_sample.json");
            
            if (inputStream == null) {
                System.err.println("❌ 无法找到 order_sample.json 文件！");
                return;
            }
            
            System.out.println("✅ 成功打开文件流");
            
            // 2. 读取文件内容
            String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("\n2. JSON 文件内容：");
            System.out.println(jsonContent);
            
            // 3. 简单的 JSON 解析 - 统计工单数量
            System.out.println("\n3. 解析 JSON...");
            int count = countMatches(jsonContent, "\"id\"");
            System.out.println("✅ 找到 " + count + " 个工单");
            
            System.out.println("\n✅ JSON 文件读取成功！");
            System.out.println("✅ 问题定位：JSON 文件已正确加载，现在需要在 OrderFormController 中正确加载和显示");
            
        } catch (Exception e) {
            System.err.println("❌ 发生异常:");
            e.printStackTrace();
        }
    }
    
    private static int countMatches(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}
