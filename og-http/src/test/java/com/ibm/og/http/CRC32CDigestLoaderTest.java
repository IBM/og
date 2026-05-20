/* Copyright (c) IBM Corporation 2025. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.http;

import com.ibm.og.api.ChecksumType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;


public class CRC32CDigestLoaderTest {

    DigestLoader cache;

    @Before
    public void before() {
        this.cache = new DigestLoader(ChecksumType.CRC32C);
    }

    @Test
    public void checkCRC32() throws Exception {
        byte[] expected = {(byte)0xd8, 0x4d, (byte)0xda, (byte)0x57};
        byte[] crc32c =  this.cache.load(1000L);
        assertArrayEquals("CRC32C mismatch", expected, crc32c);

    }

    @Test
    public void checkCRC32_10bytes() throws Exception {
        byte[] expected = {(byte)0xe3, (byte)0xdd, (byte)0xf0, (byte)0x6b};
        byte[] crc32c =  this.cache.load(10L);
        assertArrayEquals("CRC32C mismatch", expected, crc32c);

    }

    @Test
    public void checkCRC32_1megabytes() throws Exception {
        byte[] expected = {(byte)0x71, (byte)0xaf, (byte)0x9a, (byte)0x4e};
        byte[] crc32c =  this.cache.load(1000000L);
        assertArrayEquals("CRC32C mismatch", expected, crc32c);

    }

}