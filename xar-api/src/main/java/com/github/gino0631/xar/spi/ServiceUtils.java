package com.github.gino0631.xar.spi;

import java.util.Iterator;
import java.util.ServiceLoader;

public final class ServiceUtils {
    private ServiceUtils() {
    }

    public static <T> T getImpl(Class<T> cls) {
        ServiceLoader<T> spiLoader = ServiceLoader.load(cls);
        Iterator<T> it = spiLoader.iterator();
        if (!it.hasNext()) {
            throw new IllegalStateException("API implementation not found");
        }

        return it.next();
    }
}
