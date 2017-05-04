package com.ibm.og.json;

import java.util.concurrent.TimeUnit;

/**
 * Created by nlahmed on 5/9/17.
 */
public class RetentionConfig {
    public final Long expiry;
    public final TimeUnit timeUnit;
    public static final long MAX_RETENTION_EXPIRY = Integer.MAX_VALUE;

    public RetentionConfig() {
        this.expiry = null;
        this.timeUnit = TimeUnit.SECONDS;
    }

    public RetentionConfig(Long expiry, TimeUnit timeUnit) {
        this.expiry = expiry;
        this.timeUnit = timeUnit;
    }
}
