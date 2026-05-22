/* Copyright (c) IBM Corporation 2024. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.http;

import com.google.common.cache.CacheLoader;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.ibm.og.api.ChecksumType;
import com.ibm.og.util.io.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.zip.CheckedInputStream;

import software.amazon.awssdk.crt.checksums.CRC32;
import software.amazon.awssdk.crt.checksums.CRC32C;
import software.amazon.awssdk.crt.checksums.CRC64NVME;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *  A cache to store checksum used in case of zero body
 *
 *  @since 1.16.0
 */
public class DigestLoader extends CacheLoader<Long, byte[]> {
    private static Logger _logger = LoggerFactory.getLogger(DigestLoader.class);
    private final ChecksumType hash;
    public DigestLoader(ChecksumType hashFunction) {
        this.hash = hashFunction;
    }

    @Override
    public byte[] load(final Long key) throws Exception {
        checkNotNull(key);
        _logger.debug("Loading md5 digest for size [{}]", key);
        final byte[] buffer = new byte[4096];
        HashingInputStream hashStream;
        CheckedInputStream cis;
        if (this.hash == ChecksumType.MD5) {
            hashStream = new HashingInputStream(Hashing.md5(), Streams.create(Bodies.zeroes(key)));
            while (hashStream.read(buffer) != -1) {
            }
            return hashStream.hash().asBytes();
        }
        else if (this.hash == ChecksumType.SHA1) {
            hashStream = new HashingInputStream(Hashing.sha1(), Streams.create(Bodies.zeroes(key)));
            while (hashStream.read(buffer) != -1) {
            }
            hashStream.close();
            return hashStream.hash().asBytes();
        }
        else if (this.hash == ChecksumType.SHA256) {
            hashStream = new HashingInputStream(Hashing.sha256(), Streams.create(Bodies.zeroes(key)));
            while (hashStream.read(buffer) != -1) {
            }
            hashStream.close();
            return hashStream.hash().asBytes();
        }
        else if (this.hash == ChecksumType.CRC32) {
            cis = new CheckedInputStream(Streams.create(Bodies.zeroes(key)), new CRC32());
            while (cis.read(buffer) != -1) {
            }
            cis.close();
            long checksumValue = cis.getChecksum().getValue();
            ByteBuffer byteBuffer = ByteBuffer.allocate(4); // 32 bits checksum
            byteBuffer.putInt((int)checksumValue);
            return byteBuffer.array();

        }
        else if (this.hash == ChecksumType.CRC32C) {
            cis = new CheckedInputStream(Streams.create(Bodies.zeroes(key)), new CRC32C());
            while (cis.read(buffer) != -1) {
            }
            cis.close();
            long checksumValue = cis.getChecksum().getValue();
            ByteBuffer byteBuffer = ByteBuffer.allocate(4); // 32 bits
            byteBuffer.putInt((int)checksumValue);
            return byteBuffer.array();
        }
        else if (this.hash == ChecksumType.CRC64NVME) {
            cis = new CheckedInputStream(Streams.create(Bodies.zeroes(key)), new CRC64NVME());
            while (cis.read(buffer) != -1) {
            }
            cis.close();
            long checksumValue = cis.getChecksum().getValue();
            ByteBuffer byteBuffer = ByteBuffer.allocate(Long.SIZE/8);
            byteBuffer.putLong(checksumValue);
            return byteBuffer.array();
        } else {
            throw new IllegalArgumentException(String.format("Unsupported Digest Algorithm: %s", this.hash));
        }
    }
}