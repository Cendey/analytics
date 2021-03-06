package edu.lab.mit.utils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * <p>Project: KEWILL FORWARD ENTERPRISE</p>
 * <p>File: edu.lab.mit.utils.Utilities</p>
 * <p>Copyright: Copyright � 2015 Kewill Co., Ltd. All Rights Reserved.</p>
 * <p>Company: Kewill Co., Ltd</p>
 *
 * @author <chao.deng@kewill.com>
 * @version 1.0
 * @since 7/30/2015
 */
public class Utilities {

    public static <E> void adjustSize(E node, String propertyName, double delta) {
        Class<?> clazz = node.getClass();
        try {
            Method getWidth = clazz.getMethod("get" + propertyName);
            Method setPrefWidth = clazz.getMethod("setPref" + propertyName, double.class);
            setPrefWidth.invoke(node, Double.class.cast(getWidth.invoke(node)) + delta);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            System.err.println(e.getCause().getMessage());
        }
    }

    private static String getStoragePath() {
        URL url = Thread.currentThread().getContextClassLoader().getResource("./config");
        return url != null ? url.getPath() : new File(System.getProperty("java.io.tmpdir")).getPath();
    }

    public static String finalStorePath(String subDir){
        return getStoragePath() + File.separator + subDir;
    }
}
