/* Copyright (c) IBM Corporation 2025. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.http;

import com.ibm.og.api.ChecksumType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;


public class CRC32DigestLoaderTest {

    DigestLoader cache;

    @Before
    public void before() {
        this.cache = new DigestLoader(ChecksumType.CRC32);
    }

    @Test
    public void checkCRC32() throws Exception {
        byte[] expected = {(byte)0x06, 0x0b, (byte)0x17, (byte)0x80};
        byte[] crc32 =  this.cache.load(1000L);
        assertArrayEquals("CRC32 mismatch", expected, crc32);

    }

    @Test
    public void checkCRC32_10bytes() throws Exception {
        byte[] expected = {(byte)0xe3, (byte)0x8a, (byte)0x68, (byte)0x76};
        byte[] crc32 =  this.cache.load(10L);
        assertArrayEquals("CRC32 mismatch", expected, crc32);

    }

    @Test
    public void checkCRC32_1megabytes() throws Exception {
        byte[] expected = {(byte)0x12, (byte)0x79, (byte)0xcb, (byte)0x9e};
        byte[] crc32 =  this.cache.load(1000000L);
        assertArrayEquals("CRC32 mismatch", expected, crc32);

    }

}
