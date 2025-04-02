/* Copyright (c) IBM Corporation 2025. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.http;

import com.ibm.og.api.ChecksumType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;


public class CRC64NVMEDigestLoaderTest {

    DigestLoader cache;

    @Before
    public void before() {
        this.cache = new DigestLoader(ChecksumType.CRC64NVME);
    }

    @Test
    public void checkCRC64NVME_1kilobytes() throws Exception {
        //04d1bb8fa63cb488
        byte[] expected = {(byte)0x04, (byte)0xd1, (byte)0xbb, (byte)0x8f, (byte)0xa6, (byte)0x3c, (byte)0xb4, (byte)0x88};
        byte[] crc64Nvme =  this.cache.load(1000L);
        assertArrayEquals("CRC64NVME mismatch", expected, crc64Nvme);

    }

    @Test
    public void checkCRC64NVME_10bytes() throws Exception {
        //ba72d254fd671e7d
        byte[] expected = {(byte)0xba, (byte)0x72, (byte)0xd2, (byte)0x54, (byte)0xfd, (byte)0x67, (byte)0x1e, (byte)0x7d};
        byte[] crc64Nvme =  this.cache.load(10L);
        assertArrayEquals("CRC64NVME mismatch", expected, crc64Nvme);

    }

    @Test
    public void checkCRC64NVME_1megabytes() throws Exception {
        //af27c5f17a697242
        byte[] expected = {(byte)0xaf, (byte)0x27, (byte)0xc5, (byte)0xf1, (byte)0x7a, (byte)0x69, (byte)0x72, (byte)0x42};
        byte[] crc64Nvme =  this.cache.load(1000000L);
        assertArrayEquals("CRC64NVME mismatch", expected, crc64Nvme);

    }

}