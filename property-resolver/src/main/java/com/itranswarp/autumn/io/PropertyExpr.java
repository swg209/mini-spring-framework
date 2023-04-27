package com.itranswarp.autumn.io;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author: suwg
 * @Date: 2023/4/27
 */
@Data
@AllArgsConstructor
public class PropertyExpr {

    /**
     * 配置型的key.
     */
    private String key;

    /**
     * 默认值.
     */
    private String defaultValue;
}
