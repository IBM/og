/* Copyright (c) IBM Corporation 2025. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.http;

import com.ibm.og.api.ChecksumType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;


public class SHA256DigestLoaderTest {

    DigestLoader cache;

    @Before
    public void before() {
        this.cache = new DigestLoader(ChecksumType.SHA256);
    }

    @Test
    public void checkSHA256_1kilobytes() throws Exception {
        //541b3e9daa09b20bf85fa273e5cbd3e80185aa4ec298e765db87742b70138a53
        byte[] expected = {(byte)0x54, (byte)0x1b, (byte)0x3e, (byte)0x9d, (byte)0xaa, (byte)0x09, (byte)0xb2,
                (byte)0x0b, (byte)0xf8, (byte)0x5f, (byte)0xa2, (byte)0x73, (byte)0xe5, (byte)0xcb, (byte)0xd3,
                (byte)0xe8, (byte)0x01, (byte)0x85, (byte)0xaa, (byte)0x4e, (byte)0xc2, (byte)0x98, (byte)0xe7,
                (byte)0x65, (byte)0xdb, (byte)0x87, (byte)0x74, (byte)0x2b, (byte)0x70, (byte)0x13,
                (byte)0x8a, (byte)0x53 };
        byte[] sha256 =  this.cache.load(1000L);
        assertArrayEquals("SHA256 mismatch", expected, sha256);

    }

    @Test
    public void checkSHA256_10bytes() throws Exception {
        //01d448afd928065458cf670b60f5a594d735af0172c8d67f22a81680132681ca
        byte[] expected = {(byte)0x01, (byte)0xd4, (byte)0x48, (byte)0xaf, (byte)0xd9, (byte)0x28, (byte)0x06,
                (byte)0x54, (byte)0x58, (byte)0xcf, (byte)0x67, (byte)0x0b, (byte)0x60, (byte)0xf5, (byte)0xa5,
                (byte)0x94, (byte)0xd7, (byte)0x35, (byte)0xaf, (byte)0x01, (byte)0x72, (byte)0xc8, (byte)0xd6,
                (byte)0x7f, (byte)0x22, (byte)0xa8, (byte)0x16, (byte)0x80, (byte)0x13, (byte)0x26,
                (byte)0x81, (byte)0xca };
        byte[] sha256 =  this.cache.load(10L);
        assertArrayEquals("SHA256 mismatch", expected, sha256);

    }

    @Test
    public void checkSHA256_1megabytes() throws Exception {
        //d29751f2649b32ff572b5e0a9f541ea660a50f94ff0beedfb0b692b924cc8025
        byte[] expected = {(byte)0xd2, (byte)0x97, (byte)0x51, (byte)0xf2, (byte)0x64, (byte)0x9b, (byte)0x32,
                (byte)0xff, (byte)0x57, (byte)0x2b, (byte)0x5e, (byte)0x0a, (byte)0x9f, (byte)0x54, (byte)0x1e,
                (byte)0xa6, (byte)0x60, (byte)0xa5, (byte)0x0f, (byte)0x94, (byte)0xff, (byte)0x0b, (byte)0xee,
                (byte)0xdf, (byte)0xb0, (byte)0xb6, (byte)0x92, (byte)0xb9, (byte)0x24, (byte)0xcc, (byte)0x80,
                (byte)0x25 };
        byte[] sha256 =  this.cache.load(1000000L);
        assertArrayEquals("SHA1 mismatch", expected, sha256);
    }

}