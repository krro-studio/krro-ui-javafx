package top.kzre.krro.ui.javafx;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class JavaFXUtils {


    public static Background fillBackground(BackgroundFill fill){
        return new Background(fill);
    }

    /**
     * 给 ImageView 设置图片源。
     * <p>
     * src 支持以下格式：
     * <ul>
     *   <li>{@code "classpath:/path/to/img.png"} — classpath 资源</li>
     *   <li>{@code "file:/path/to/img.png"} 或 {@code "file:///..."} — 本地文件</li>
     *   <li>{@code "http://..."} / {@code "https://..."} — 网络图片</li>
     *   <li>{@code "/path/to/img.png"}（以 / 开头）— 当作 classpath 资源</li>
     *   <li>其他 — 直接交给 Image 构造函数（URL 或相对文件路径）</li>
     * </ul>
     *
     * @param imageView 目标 ImageView，不能为 null
     * @param src       图片源路径
     */
    public static ImageView imageViewSetSrc(ImageView imageView, String src) {
        if (imageView == null) {
            throw new IllegalArgumentException("imageView must not be null");
        }
        if (src == null || src.isEmpty()) {
            imageView.setImage(null);
            return imageView;
        }

        Image image = loadImage(src);
        imageView.setImage(image);

        return imageView;
    }

    /**
     * 加载图片，返回 Image 对象。
     * 与 imageViewSetSrc 共享同一套路径解析逻辑，方便需要 Image 而非 ImageView 的场景。
     */
    public static Image loadImage(String src) {
        if (src == null || src.isEmpty()) {
            return null;
        }

        if (src.startsWith(ResourceUtils.CLASSPATH_SCHEME)) {
            return loadFromClasspath(src);
        }

        if (src.startsWith("file:") || src.startsWith("http://") || src.startsWith("https://")) {
            return new Image(src);
        }

        // 以 / 开头，当作 classpath 资源
        if (src.startsWith("/")) {
            return loadFromClasspath(src);
        }

        // 否则直接交给 Image 构造函数（URL 或相对文件路径）
        return new Image(src);
    }

    private static Image loadFromClasspath(String classpath) {

        try (InputStream is = ResourceUtils.getClasspathInputStream(classpath)) {
            if (is == null) {
                System.err.println("JavaFXUtils: classpath resource not found: " + classpath);
                return null;
            }
            return new Image(is);
        } catch (IOException e) {
            System.err.println("JavaFXUtils: failed to load classpath resource: " + classpath);
            e.printStackTrace();
            return null;
        }
    }



    /**
     * JavaFX 原生支持，但不以 -fx- 开头的样式
     */
    private static final Set<String> FX_STYLES = Set.of(

            // ===== 基础 / 通用 =====
            "-fx-background-color",
            "-fx-background-radius",
            "-fx-background-insets",
            "-fx-border-color",
            "-fx-border-width",
            "-fx-border-radius",
            "-fx-border-style",
            "-fx-padding",
            "-fx-opacity",
            "-fx-cursor",
            "-fx-visible",
            "-fx-focus-traversable",

            // ===== 文本 =====
            "-fx-font",
            "-fx-font-family",
            "-fx-font-size",
            "-fx-font-weight",
            "-fx-font-style",
            "-fx-text-fill",
            "-fx-text-alignment",
            "-fx-alignment",

            // ===== 布局 =====
            "-fx-spacing",
            "-fx-pref-width",
            "-fx-pref-height",
            "-fx-min-width",
            "-fx-min-height",
            "-fx-max-width",
            "-fx-max-height",

            // ===== 控件 =====
            "-fx-control-inner-background",
            "-fx-marker-color",
            "-fx-base",
            "-fx-accent",

            // ===== 其他常用 =====
            "-fx-graphic",
            "-fx-content-display",
            "-fx-rotate",
            "-fx-scale-x",
            "-fx-scale-y",
            "-fx-translate-x",
            "-fx-translate-y",

            // ===== 例外（无 -fx- 前缀，但 JavaFX 认） =====
            "-alignment",
            "-text-alignment",
            "-spacing",
            "-margin",
            "-padding",
            "-opacity",
            "-visible",
            "-cursor",
            "-focus-traversable"
    );

    /**
     * 是否是 JavaFX 原生样式
     * <p>
     * 规则：
     * 1. 以 -fx- 开头 → 一定是
     * 2. 属于 JavaFX 原生例外 → 是
     * 3. 其他 → 否
     */
    public static boolean isFxStyle(String styleKey) {
        if (styleKey == null) {
            return false;
        }

        return FX_STYLES.contains(styleKey);
    }


    /** 确保字符串被一对双引号包围（幂等）。null → null，已带引号则原样返回。 */
    public static String ensureStringQuoted(String string) {
        if (string == null || isQuoted(string)) {
            return string;
        }
        return "\"" + string + "\"";
    }

    /** 是否已被引号包围 */
    public static boolean isQuoted(String string) {
        return string != null
                && string.length() >= 2
                && string.charAt(0) == '"'
                && string.charAt(string.length() - 1) == '"';
    }
}
