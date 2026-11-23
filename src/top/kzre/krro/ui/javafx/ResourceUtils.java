package top.kzre.krro.ui.javafx;

import java.io.InputStream;

final class ResourceUtils {
    final static String CLASSPATH_SCHEME = "classpath:";
    private final static int CLASSPATH_SCHEME_LENGTH = CLASSPATH_SCHEME.length();

    public static InputStream getClasspathInputStream(String path) {
        // 去掉开头的 /，ClassLoader.getResourceAsStream 不需要前导 /
        if (path.startsWith(CLASSPATH_SCHEME)){
             path = path.substring(CLASSPATH_SCHEME_LENGTH);
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        return Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(normalized);
    }

}
