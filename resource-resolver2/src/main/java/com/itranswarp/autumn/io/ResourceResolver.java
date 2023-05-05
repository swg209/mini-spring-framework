package com.itranswarp.autumn.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple classpath scan works both in directory and jar: 扫描包内的资源信息.
 * https://stackoverflow.com/questions/520328/can-you-find-all-classes-in-a-package-using-reflection#58773038
 * 在编写IoC容器之前，我们首先要实现@ComponentScan，即解决“在指定包下扫描所有Class”的问题。
 *
 * @Author: suwg
 * @Date: 2023/4/26
 */
public class ResourceResolver {

    private static final Logger logger = Logger.getLogger(ResourceResolver.class.getName());

    String basePackage;

    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * scan方法，扫描包内文件.
     *
     * @param mapper
     * @param <R>
     * @return
     */
    public <R> List<R> scan(Function<Resource, R> mapper) {

        String basePackagePath = this.basePackage.replace(".", "/");
        String path = basePackagePath;
        try {
            List<R> collector = new ArrayList<>();
            scan0(basePackagePath, path, collector, mapper);
            return collector;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * @param basePackagePath
     * @param path
     * @param collector
     * @param mapper
     * @param <R>
     * @throws IOException
     * @throws URISyntaxException
     */
    <R> void scan0(String basePackagePath, String path, List<R> collector, Function<Resource, R> mapper)
            throws IOException, URISyntaxException {
        logger.log(Level.INFO, String.format("scan path: %s", path));

        // 通过ClassLoader获取URL列表:
        Enumeration<URL> en = getContextClassLoader().getResources(path);
        while (en.hasMoreElements()) {
            URL url = en.nextElement();
            URI uri = url.toURI();
            String uriStr = removeTrailingSlash(uri.toString());
            String uriBaseStr = uriStr.substring(0, uriStr.length() - basePackagePath.length());
            if (uriBaseStr.startsWith("file:")) {
                uriBaseStr = uriBaseStr.substring(5);
            }
            if (uriStr.startsWith("jar:")) {
                scanFile(true, uriBaseStr, jarUriToPath(basePackagePath, uri), collector, mapper);
            } else {
                scanFile(false, uriBaseStr, Paths.get(uri), collector, mapper);
            }
        }

    }


    Path jarUriToPath(String basePackagePath, URI jarUri) throws IOException {
        Map<String, Object> map = new HashMap<>();
        return FileSystems.newFileSystem(jarUri, map).getPath(basePackagePath);
    }

    /**
     * 扫描文件.
     *
     * @param isJar
     * @param base
     * @param root
     * @param collector
     * @param mapper
     * @param <R>
     * @throws IOException
     */
    <R> void scanFile(boolean isJar, String base, Path root, List<R> collector, Function<Resource, R> mapper)
            throws IOException {
        String baseDir = removeTrailingSlash(base);
        Files.walk(root).filter(Files::isRegularFile).forEach(file -> {
            Resource res = null;
            if (isJar) {
                res = new Resource(baseDir, removeLeadingSlash(file.toString()));
            } else {
                String path = file.toString();
                String name = removeLeadingSlash(path.substring(baseDir.length()));
                res = new Resource("file:" + path, name);
            }
            logger.log(Level.INFO, String.format("found resource: %s", res));
            R r = mapper.apply(res);
            if (r != null) {
                collector.add(r);
            }

        });
    }


    /**
     * 去除字符串开头的斜杠（/).
     *
     * @param s
     * @return
     */

    String removeLeadingSlash(String s) {
        if (s.startsWith("/") || s.startsWith("\\")) {
            s = s.substring(1);
        }
        return s;
    }

    /**
     * 去除字符串末尾的斜杠(/).
     *
     * @param s
     * @return
     */
    String removeTrailingSlash(String s) {
        if (s.endsWith("/") || s.endsWith("\\")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    ClassLoader getContextClassLoader() {
        ClassLoader cl = null;
        cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = getClass().getClassLoader();
        }
        return cl;
    }

}
