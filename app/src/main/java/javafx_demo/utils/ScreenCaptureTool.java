package javafx_demo.utils;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 屏幕截图工具 — 支持区域选择、预览确认
 * <p>
 * 用法: ScreenCaptureTool.capture(window, file -> { ... });
 */
public class ScreenCaptureTool {

    /** 当前的浮窗 Stage（全局唯一） */
    private static Stage floatingStage;

    /**
     * 启动屏幕截图。先隐藏所有应用窗口，延时后截取屏幕（可截到游戏画面），
     * 完成/取消后恢复窗口。
     *
     * @param ownerWindow 触发截图的窗口（可为 null）
     * @param onDone      截图完成回调，参数为截图 PNG 文件（取消时为 null）
     */
    public static void capture(Window ownerWindow, Consumer<File> onDone) {
        // 收集所有可见 Stage 并隐藏，避免出现在截图中
        List<Stage> toRestore = new ArrayList<>();
        for (Window w : new ArrayList<>(Window.getWindows())) {
            if (w instanceof Stage s && s.isShowing() && !s.isIconified()) {
                s.setIconified(true);
                toRestore.add(s);
            }
        }

        // 等待窗口完全隐藏后再截图
        PauseTransition wait = new PauseTransition(Duration.millis(500));
        wait.setOnFinished(evt -> doCapture(toRestore, onDone));
        wait.play();
    }

    /**
     * 在屏幕右侧中间显示一个置顶小浮窗按钮（📷），用户在游戏中点击即可触发截图。
     * 截图完成后浮窗保留，直到调用 {@link #hideFloatingTrigger()} 关闭。
     *
     * @param onDone 截图完成回调
     */
    public static void showFloatingTrigger(Consumer<File> onDone) {
        if (floatingStage != null && floatingStage.isShowing()) return;

        floatingStage = new Stage(StageStyle.TRANSPARENT);
        floatingStage.setAlwaysOnTop(true);

        Button btn = new Button("📷");
        btn.setStyle("-fx-background-color: rgba(25,179,61,0.85); -fx-text-fill: white; "
                + "-fx-font-size: 20; -fx-cursor: hand; -fx-padding: 8 12; "
                + "-fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 6, 0, 0, 2);");
        btn.setOnAction(e -> {
            // 隐藏浮窗自身再截图
            floatingStage.hide();
            capture(null, file -> {
                Platform.runLater(() -> {
                    floatingStage.show();
                    onDone.accept(file);
                });
            });
        });

        StackPane root = new StackPane(btn);
        root.setStyle("-fx-background-color: transparent;");
        root.setPadding(new Insets(4));

        Scene scene = new Scene(root, -1, -1, Color.TRANSPARENT);
        floatingStage.setScene(scene);

        // 放在屏幕右侧中间
        Rectangle2D sb = Screen.getPrimary().getVisualBounds();
        floatingStage.setX(sb.getMaxX() - 70);
        floatingStage.setY(sb.getMinY() + sb.getHeight() / 2 - 25);

        // 允许拖动浮窗
        final double[] dragDelta = new double[2];
        root.setOnMousePressed(me -> {
            dragDelta[0] = floatingStage.getX() - me.getScreenX();
            dragDelta[1] = floatingStage.getY() - me.getScreenY();
        });
        root.setOnMouseDragged(me -> {
            floatingStage.setX(me.getScreenX() + dragDelta[0]);
            floatingStage.setY(me.getScreenY() + dragDelta[1]);
        });

        floatingStage.show();
    }

    /** 关闭浮窗截图按钮 */
    public static void hideFloatingTrigger() {
        if (floatingStage != null) {
            floatingStage.close();
            floatingStage = null;
        }
    }

    // ==================== 截图核心 ====================

    private static void doCapture(List<Stage> toRestore, Consumer<File> onDone) {
        BufferedImage screenshot;
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        try {
            Robot robot = new Robot();
            screenshot = robot.createScreenCapture(new Rectangle(
                    (int) screenBounds.getMinX(), (int) screenBounds.getMinY(),
                    (int) screenBounds.getWidth(), (int) screenBounds.getHeight()));
        } catch (Exception e) {
            restore(toRestore);
            onDone.accept(null);
            return;
        }

        // BufferedImage → JavaFX Image（内存转换，无需写临时文件）
        Image bgImage = toFxImage(screenshot);
        showOverlay(bgImage, screenshot, screenBounds, toRestore, onDone);
    }

    /** BufferedImage → JavaFX WritableImage（避免依赖 javafx.swing） */
    private static Image toFxImage(BufferedImage buf) {
        int w = buf.getWidth(), h = buf.getHeight();
        WritableImage img = new WritableImage(w, h);
        var pw = img.getPixelWriter();
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            buf.getRGB(0, y, w, 1, row, 0, w);
            for (int i = 0; i < w; i++) row[i] |= 0xFF000000; // 确保 alpha=255
            pw.setPixels(0, y, w, 1, PixelFormat.getIntArgbInstance(), row, 0, w);
        }
        return img;
    }

    // ==================== 全屏选区遮罩 ====================

    private static void showOverlay(Image bgImage, BufferedImage rawShot,
                                    Rectangle2D screenBounds,
                                    List<Stage> toRestore, Consumer<File> onDone) {
        double W = screenBounds.getWidth();
        double H = screenBounds.getHeight();

        Stage overlay = new Stage(StageStyle.TRANSPARENT);
        overlay.setAlwaysOnTop(true);

        Canvas canvas = new Canvas(W, H);
        Pane root = new Pane(canvas);
        root.setStyle("-fx-background-color: transparent;");

        HBox toolbar = buildToolbar();
        toolbar.setVisible(false);
        root.getChildren().add(toolbar);

        Scene scene = new Scene(root, W, H, Color.TRANSPARENT);
        scene.setCursor(Cursor.CROSSHAIR);
        overlay.setScene(scene);
        overlay.setX(screenBounds.getMinX());
        overlay.setY(screenBounds.getMinY());

        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawDimmed(gc, bgImage, W, H);

        // -- 选区状态 --
        double[] sx = {0}, sy = {0}, ex = {0}, ey = {0};
        boolean[] dragging = {false}, locked = {false};

        canvas.setOnMousePressed(e -> {
            if (locked[0]) return;
            sx[0] = e.getX(); sy[0] = e.getY();
            ex[0] = e.getX(); ey[0] = e.getY();
            dragging[0] = true;
            toolbar.setVisible(false);
        });

        canvas.setOnMouseDragged(e -> {
            if (!dragging[0]) return;
            ex[0] = clamp(e.getX(), 0, W);
            ey[0] = clamp(e.getY(), 0, H);
            drawSelection(gc, bgImage, W, H, sx[0], sy[0], ex[0], ey[0]);
        });

        canvas.setOnMouseReleased(e -> {
            if (!dragging[0]) return;
            dragging[0] = false;
            ex[0] = clamp(e.getX(), 0, W);
            ey[0] = clamp(e.getY(), 0, H);
            if (Math.abs(ex[0] - sx[0]) < 5 || Math.abs(ey[0] - sy[0]) < 5) return;
            locked[0] = true;
            drawSelection(gc, bgImage, W, H, sx[0], sy[0], ex[0], ey[0]);
            positionToolbar(toolbar, sx[0], sy[0], ex[0], ey[0], W, H);
        });

        // -- 工具栏按钮回调 --
        Button confirmBtn = (Button) toolbar.getChildren().get(0);
        Button redoBtn    = (Button) toolbar.getChildren().get(1);
        Button cancelBtn  = (Button) toolbar.getChildren().get(2);

        confirmBtn.setOnAction(e -> {
            File result = cropAndSave(rawShot, W, H, sx[0], sy[0], ex[0], ey[0]);
            overlay.close();
            restore(toRestore);
            onDone.accept(result);
        });

        redoBtn.setOnAction(e -> {
            locked[0] = false;
            toolbar.setVisible(false);
            drawDimmed(gc, bgImage, W, H);
            scene.setCursor(Cursor.CROSSHAIR);
        });

        cancelBtn.setOnAction(e -> {
            overlay.close();
            restore(toRestore);
            onDone.accept(null);
        });

        // 键盘快捷键: ESC=取消, Enter=确认
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                overlay.close();
                restore(toRestore);
                onDone.accept(null);
            } else if (e.getCode() == KeyCode.ENTER && locked[0]) {
                confirmBtn.fire();
            }
        });

        overlay.show();
        overlay.requestFocus();
    }

    // ==================== 绘制 ====================

    /** 绘制全屏截图 + 半透明蒙版 */
    private static void drawDimmed(GraphicsContext gc, Image bg, double w, double h) {
        gc.clearRect(0, 0, w, h);
        gc.drawImage(bg, 0, 0, w, h);
        gc.setFill(Color.rgb(0, 0, 0, 0.35));
        gc.fillRect(0, 0, w, h);
    }

    /** 绘制选区高亮（选中区域清晰显示 + 边框 + 尺寸标签） */
    private static void drawSelection(GraphicsContext gc, Image bg, double W, double H,
                                      double x1, double y1, double x2, double y2) {
        drawDimmed(gc, bg, W, H);

        double rx = Math.min(x1, x2), ry = Math.min(y1, y2);
        double rw = Math.abs(x2 - x1), rh = Math.abs(y2 - y1);

        // 选区内绘制清晰原图（处理截图可能的 Retina 2x 分辨率）
        double imgScaleX = bg.getWidth() / W;
        double imgScaleY = bg.getHeight() / H;
        gc.drawImage(bg,
                rx * imgScaleX, ry * imgScaleY, rw * imgScaleX, rh * imgScaleY,
                rx, ry, rw, rh);

        // 选区边框
        gc.setStroke(Color.rgb(0, 174, 255));
        gc.setLineWidth(2);
        gc.strokeRect(rx, ry, rw, rh);

        // 绘制 8 个调整手柄小方块
        gc.setFill(Color.rgb(0, 174, 255));
        double hs = 4; // 手柄半径
        double[][] handles = {
                {rx, ry}, {rx + rw / 2, ry}, {rx + rw, ry},
                {rx, ry + rh / 2}, {rx + rw, ry + rh / 2},
                {rx, ry + rh}, {rx + rw / 2, ry + rh}, {rx + rw, ry + rh}
        };
        for (double[] h : handles) {
            gc.fillRect(h[0] - hs, h[1] - hs, hs * 2, hs * 2);
        }

        // 尺寸标签
        String sizeText = (int) rw + " × " + (int) rh;
        double labelY = ry > 25 ? ry - 8 : ry + rh + 18;
        gc.setFill(Color.rgb(0, 0, 0, 0.65));
        gc.fillRoundRect(rx, labelY - 16, sizeText.length() * 7.5 + 14, 22, 6, 6);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(12));
        gc.fillText(sizeText, rx + 7, labelY);
    }

    // ==================== 裁剪 & 保存 ====================

    private static File cropAndSave(BufferedImage raw, double canvasW, double canvasH,
                                    double x1, double y1, double x2, double y2) {
        double scaleX = raw.getWidth() / canvasW;
        double scaleY = raw.getHeight() / canvasH;
        int cx = (int) (Math.min(x1, x2) * scaleX);
        int cy = (int) (Math.min(y1, y2) * scaleY);
        int cw = (int) (Math.abs(x2 - x1) * scaleX);
        int ch = (int) (Math.abs(y2 - y1) * scaleY);
        cx = Math.max(0, cx);
        cy = Math.max(0, cy);
        cw = Math.min(cw, raw.getWidth() - cx);
        ch = Math.min(ch, raw.getHeight() - cy);
        if (cw <= 0 || ch <= 0) return null;

        try {
            BufferedImage cropped = raw.getSubimage(cx, cy, cw, ch);
            File out = File.createTempFile("screenshot_", ".png");
            out.deleteOnExit();
            ImageIO.write(cropped, "png", out);
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 工具栏 ====================

    private static HBox buildToolbar() {
        Button confirm = styledBtn("✓ 确认", "#27ae60");
        Button redo    = styledBtn("↻ 重新截取", "#3498db");
        Button cancel  = styledBtn("✕ 取消", "#e74c3c");

        HBox box = new HBox(6, confirm, redo, cancel);
        box.setPadding(new Insets(5, 8, 5, 8));
        box.setStyle("-fx-background-color: rgba(0,0,0,0.75); -fx-background-radius: 5;");
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private static Button styledBtn(String text, String bg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:white; -fx-cursor:hand; "
                + "-fx-padding:5 12; -fx-background-radius:3; -fx-font-size:12;");
        return b;
    }

    private static void positionToolbar(HBox toolbar, double x1, double y1,
                                        double x2, double y2, double W, double H) {
        toolbar.setVisible(true);
        Platform.runLater(() -> {
            toolbar.applyCss();
            toolbar.layout();
            double tw = toolbar.getBoundsInLocal().getWidth();
            double th = toolbar.getBoundsInLocal().getHeight();
            if (tw == 0) { tw = 230; th = 36; }

            double rx = Math.min(x1, x2), rw = Math.abs(x2 - x1);
            double ry = Math.min(y1, y2), rh = Math.abs(y2 - y1);

            double tx = rx + rw - tw;
            if (tx < 0) tx = 0;
            double ty = ry + rh + 6;
            if (ty + th > H) ty = ry - th - 6;
            if (ty < 0) ty = 0;

            toolbar.setLayoutX(tx);
            toolbar.setLayoutY(ty);
        });
    }

    // ==================== 工具方法 ====================

    private static void restore(List<Stage> stages) {
        Platform.runLater(() -> {
            for (Stage s : stages) s.setIconified(false);
            // 按窗口大小降序排列 toFront，最小的窗口（对话框）最后调用，确保在最前面
            stages.stream()
                    .sorted((a, b) -> Double.compare(b.getWidth() * b.getHeight(), a.getWidth() * a.getHeight()))
                    .forEach(Stage::toFront);
        });
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
