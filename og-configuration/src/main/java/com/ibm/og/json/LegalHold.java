package com.ibm.og.json;

/**
 * Created by nlahmed on 5/9/17.
 */
public class LegalHold {
    public String legalHoldPrefix = "";
    public static final Long MIN_SUFFIX = 1L;
    public static final Long MAX_SUFFIX = 100L;

    public LegalHold() {
        this.legalHoldPrefix = "";
    }
}
