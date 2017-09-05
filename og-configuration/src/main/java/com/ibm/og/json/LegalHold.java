/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json;

public class LegalHold {
    public String legalHoldPrefix = "";
    public static final Long MIN_SUFFIX = 1L;
    public static final Long MAX_SUFFIX = 100L;
    public double percentage = 0.00;

    public LegalHold() {
        this.legalHoldPrefix = "";
    }
}
