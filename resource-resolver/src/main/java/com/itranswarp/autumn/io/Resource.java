package com.itranswarp.autumn.io;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author: suwg
 * @Date: 2023/4/26
 */
@Data
@AllArgsConstructor
public class Resource {

    /**
     * 资源路径.
     */
    private String path;

    /**
     * 资源名称.
     */
    private String name;


}
