package com.itranswarp.autumn.io;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.annotation.sub.AnnoScan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @Author: suwg
 * @Date: 2023/4/26
 */
public class ResourceResolverTest {


    //测试扫描类.
    //返回的类名列表跟预期相符.
    @Test
    public void testScanClass() {

        String pkg = "com.itranswarp.scan";
        ResourceResolver resourceResolver = new ResourceResolver(pkg);
        List<String> classes = resourceResolver.scan(resource -> {
            String name = resource.getName();
            if (name.endsWith(".class")) {
                return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
            }
            return null;
        });
        Collections.sort(classes);
        System.out.println(classes);
        String[] listClasses = new String[]{
                // list of some scan classes:
                "com.itranswarp.scan.convert.ValueConverterBean", //
                "com.itranswarp.scan.destroy.AnnotationDestroyBean", //
                "com.itranswarp.scan.init.SpecifyInitConfiguration", //
                "com.itranswarp.scan.proxy.OriginBean", //
                "com.itranswarp.scan.proxy.FirstProxyBeanPostProcessor", //
                "com.itranswarp.scan.proxy.SecondProxyBeanPostProcessor", //
                "com.itranswarp.scan.nested.OuterBean", //
                "com.itranswarp.scan.nested.OuterBean$NestedBean", //
                "com.itranswarp.scan.sub1.Sub1Bean", //
                "com.itranswarp.scan.sub1.sub2.Sub2Bean", //
                "com.itranswarp.scan.sub1.sub2.sub3.Sub3Bean", //
        };
        for (String clazz : listClasses) {
            assertTrue(classes.contains(clazz));
        }
    }


    //测试扫描Jar包内的文件.
    //返回类名列表符合预期.
    @Test
    public void testScanJar() {
        String pkg = PostConstruct.class.getPackage().getName();
        ResourceResolver resourceResolver = new ResourceResolver(pkg);
        List<String> classes = resourceResolver.scan(resource -> {
            String name = resource.getName();
            if (name.endsWith(".class")) {
                return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
            }
            return null;
        });

        //classes in jar:

        assertTrue(classes.contains(PostConstruct.class.getName()));
        assertTrue(classes.contains(PreDestroy.class.getName()));
        assertTrue(classes.contains(PermitAll.class.getName()));
        assertTrue(classes.contains(DataSourceDefinition.class.getName()));
        // jakarta.annotation.sub.AnnoScan is defined in classes:
        assertTrue(classes.contains(AnnoScan.class.getName()));

    }


    //测试 扫描txt文件.
    @Test
    public void testScanTxt() {
        String pkg = "com.itranswarp.scan";
        ResourceResolver resourceResolver = new ResourceResolver(pkg);
        List<String> classes = resourceResolver.scan(resource -> {
            String name = resource.getName();
            if (name.endsWith(".txt")) {
                return name.replace("\\", "/");
            }
            return null;
        });
        Collections.sort(classes);

        List<String> listClasses = Arrays.asList(
                // txt files:
                "com/itranswarp/scan/sub1/sub1.txt", //
                "com/itranswarp/scan/sub1/sub2/sub2.txt", //
                "com/itranswarp/scan/sub1/sub2/sub3/sub3.txt"); //

        Assertions.assertEquals(listClasses, classes);

    }

}
