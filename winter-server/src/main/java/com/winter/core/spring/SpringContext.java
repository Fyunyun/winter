package com.winter.core.spring;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public final class SpringContext {

    private static volatile AnnotationConfigApplicationContext context;

    private SpringContext() {
    }

    public static void init() {
        if (context != null) {
            return;
        }
        synchronized (SpringContext.class) {
            if (context == null) {
                context = new AnnotationConfigApplicationContext(AppConfig.class);
            }
        }
    }

    public static <T> T getBean(Class<T> type) {
        init();
        return context.getBean(type);
    }
}
