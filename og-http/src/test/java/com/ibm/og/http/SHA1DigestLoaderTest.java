/* Copyright (c) IBM Corporation 2025. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.http;

import com.ibm.og.api.ChecksumType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;


public class SHA1DigestLoaderTest {

    DigestLoader cache;

    @Before
    public void before() {
        this.cache = new DigestLoader(ChecksumType.SHA1);
    }

    @Test
    public void checkSHA1_1kilobytes() throws Exception {
        //c577f7a3 76570532 75f3e3ec c06ec22e 6b909366
        byte[] expected = {(byte)0xc5, (byte)0x77, (byte)0xf7, (byte)0xa3, (byte)0x76, (byte)0x57, (byte)0x05, (byte)0x32,
                (byte)0x75, (byte)0xf3, (byte)0xe3, (byte)0xec, (byte)0xc0, (byte)0x6e, (byte)0xc2, (byte)0x2e,
                (byte)0x6b, (byte)0x90, (byte)0x93, (byte)0x66};
        byte[] sha1 =  this.cache.load(1000L);
        assertArrayEquals("SHA1 mismatch", expected, sha1);

    }

    @Test
    public void checkSHA1_10bytes() throws Exception {
        //9694c4eb d673a5e2 fd26e4b2 e64f92e9 14ebd95f
        byte[] expected = {(byte)0x96, (byte)0x94, (byte)0xc4, (byte)0xeb, (byte)0xd6, (byte)0x73, (byte)0xa5, (byte)0xe2,
                (byte)0xfd, (byte)0x26, (byte)0xe4, (byte)0xb2, (byte)0xe6, (byte)0x4f, (byte)0x92, (byte)0xe9,
                (byte)0x14, (byte)0xeb, (byte)0xd9, (byte)0x5f};
        byte[] sha1 =  this.cache.load(10L);
        assertArrayEquals("SHA1 mismatch", expected, sha1);

    }

    @Test
    public void checkSHA1_1megabytes() throws Exception {
        //bef35952 66a65a2f f36b700a 75e8ed95 c68210b6
        byte[] expected = {(byte)0xbe, (byte)0xf3, (byte)0x59, (byte)0x52, (byte)0x66, (byte)0xa6, (byte)0x5a, (byte)0x2f,
                (byte)0xf3, (byte)0x6b, (byte)0x70, (byte)0x0a, (byte)0x75, (byte)0xe8, (byte)0xed, (byte)0x95,
                (byte)0xc6, (byte)0x82, (byte)0x10, (byte)0xb6};
        byte[] sha1 =  this.cache.load(1000000L);
        assertArrayEquals("SHA1 mismatch", expected, sha1);

    }

}