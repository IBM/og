/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json;

import java.util.concurrent.TimeUnit;

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
