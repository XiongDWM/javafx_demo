package javafx_demo.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 游戏类型本地存储 — 读写 ~/.future_pal/game_types.txt
 * 每行一个游戏名，默认包含「三角洲」「瓦罗兰特」
 */
public class GameTypeStore {

    private static final Path STORE_FILE = Paths.get(
            System.getProperty("user.home"), ".future_pal", "game_types.txt");

    private static final List<String> DEFAULTS = List.of("三角洲", "瓦罗兰特");

    /** 读取所有游戏类型（保证默认项在前） */
    public static List<String> load() {
        Set<String> set = new LinkedHashSet<>(DEFAULTS);
        try {
            if (Files.exists(STORE_FILE)) {
                for (String line : Files.readAllLines(STORE_FILE)) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) set.add(trimmed);
                }
            }
        } catch (IOException e) {
            System.err.println("读取游戏类型失败: " + e.getMessage());
        }
        return new ArrayList<>(set);
    }

    /** 新增一个游戏类型并持久化 */
    public static void add(String gameType) {
        if (gameType == null || gameType.trim().isEmpty()) return;
        List<String> all = load();
        String trimmed = gameType.trim();
        if (all.contains(trimmed)) return;
        all.add(trimmed);
        save(all);
    }

    private static void save(List<String> types) {
        try {
            Files.createDirectories(STORE_FILE.getParent());
            Files.writeString(STORE_FILE, String.join("\n", types));
        } catch (IOException e) {
            System.err.println("保存游戏类型失败: " + e.getMessage());
        }
    }
}
