package com.itranswarp.autumn.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * @Author: suwg
 * @Date: 2023/4/27
 */
@FunctionalInterface
public interface InputStreamCallback<T> {
    T doWithInputStream(InputStream stream) throws IOException;
}
