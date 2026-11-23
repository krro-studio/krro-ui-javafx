package top.kzre.krro.ui.javafx;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;

import java.io.IOException;
import java.io.InputStream;

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



}
