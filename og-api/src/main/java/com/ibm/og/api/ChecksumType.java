/* Copyright (c) IBM Corporation 2025. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.api;

public enum ChecksumType {
   // NONE, MD5, SHA1, SHA256, CRC32, CRC32C, CRC64NVME

    NONE("NONE", "", ""),
    MD5("", "", ""),
    SHA1("SHA1", "<ChecksumSHA1>", "</ChecksumSHA1>"),
    SHA256("SHA256", "<ChecksumSHA256>", "</ChecksumSHA256>"),
    CRC32("CRC32", "<ChecksumCRC32>", "</ChecksumCRC32>"),
    CRC32C("CRC32C", "<ChecksumCRC32C>", "</ChecksumCRC32C>"),
    CRC64NVME("CRC64NVME", "<CRC64NVME>", "</CRC64NVME>");


    private final String name;
    private final String beginHtmlTag;
    private final String endHtmlTag;
    ChecksumType(final String checksumType, final String beginHtmlTag, final String endHtmlTag) {
        this.name = checksumType;
        this.beginHtmlTag = beginHtmlTag;
        this.endHtmlTag = endHtmlTag;
    }

    public final String beginTag() {
        return beginHtmlTag;
    }

    public final String endTag() {
        return endHtmlTag;
    }
}
