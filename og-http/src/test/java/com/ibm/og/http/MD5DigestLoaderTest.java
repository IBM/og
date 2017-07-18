/* Copyright (c) IBM Corporation 2017. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.http;

import static org.junit.Assert.assertArrayEquals;
import org.junit.Before;
import org.junit.Test;

public class MD5DigestLoaderTest {

    MD5DigestLoader cache;

    @Before
    public void before() {
       this.cache = new MD5DigestLoader();
    }

    @Test
    public void checkMD5() throws Exception {
        byte[] expected = {(byte)0xA6, 0x3C, (byte)0x90, (byte)0xCC, 0x36, (byte)0x84, (byte)0xAD, (byte)0x8B, 0x0A, 0x21,
                (byte)0x76, (byte)0xA6, (byte)0xA8, (byte)0xFE, (byte)0x90, (byte)0x05};
        //byte[] expected = "A6 3C 90 CC 36 84 AD 8B 0A 21 76 A6 A8 FE 90 05".getBytes();
        byte[] md5 =  this.cache.load(10L);
        assertArrayEquals("MD5 mismatch", expected, md5);

    }

    @Test
    public void checkMD5_1000() throws Exception {
        byte[] expected = {(byte)0x6B, (byte)0xF9, (byte)0x5A, (byte)0x48, (byte)0xF3, (byte)0x66, (byte)0xBD, (byte)0xF8, (byte)0XAF, 0x3A,
                (byte)0x19, (byte)0x8C, (byte)0x7B, (byte)0x72, (byte)0x3C, (byte)0x77};
        byte[] md5 =  this.cache.load(5000L);
        assertArrayEquals("MD5 mismatch", expected, md5);
    }

}
