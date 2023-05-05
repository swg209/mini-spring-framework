package com.itranswarp.autumn.Utils;

import com.itranswarp.autumn.io.InputStreamCallback;
import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * @Author: suwg
 * @Date: 2023/4/27
 */
public class ClassPathUtils {


    /**
     * 读入文件输入流.
     *
     * @param path
     * @param inputStreamCallback
     * @param <T>
     * @return
     */
    public static <T> T readInputStream(String path, InputStreamCallback<T> inputStreamCallback) {
        //去掉路径开头的/
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        try (InputStream input = getContextClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new FileNotFoundException("File not found in classpath: " + path);
            }
            return inputStreamCallback.doWithInputStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }


    /**
     * 文件内容转化为字符串返回.
     *
     * @param path
     * @return
     */
    public static String readString(String path) {
        return readInputStream(path, (input) -> {
            byte[] data = IOUtils.toByteArray(input);
            return new String(data, StandardCharsets.UTF_8);
        });
    }


    /**
     * 获取上下文类加载器.
     */
    static ClassLoader getContextClassLoader() {
        ClassLoader cl = null;
        cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ClassPathUtils.class.getClassLoader();
        }
        return cl;
    }


}
