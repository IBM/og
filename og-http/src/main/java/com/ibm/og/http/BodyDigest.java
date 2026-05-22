/* Copyright (c) IBM Corporation 2025. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.http;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.ibm.og.api.ChecksumType;
import com.ibm.og.util.io.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.crt.checksums.CRC32;
import software.amazon.awssdk.crt.checksums.CRC32C;
import software.amazon.awssdk.crt.checksums.CRC64NVME;

import java.nio.ByteBuffer;
import java.util.zip.CheckedInputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *  Checksum for a custom body
 *
 *  @since 1.16.0
 */
public class BodyDigest {
    private static Logger _logger = LoggerFactory.getLogger(BodyDigest.class);

    public static byte[] get(final ChecksumType hash, final String body) throws Exception {
        checkNotNull(body);
        _logger.debug("Loading md5 digest for size [{}]", body);
        final byte[] buffer = new byte[4096];
        HashingInputStream hashStream;
        CheckedInputStream cis;
        if (hash == ChecksumType.MD5) {
            hashStream = new HashingInputStream(Hashing.md5(), Streams.create(Bodies.custom(body.length(), body)));
            while (hashStream.read(buffer) != -1) {
            }
            return hashStream.hash().asBytes();
        }
        else if (hash == ChecksumType.SHA1) {
            hashStream = new HashingInputStream(Hashing.sha1(), Streams.create(Bodies.custom(body.length(), body)));
            while (hashStream.read(buffer) != -1) {
            }
            hashStream.close();
            return hashStream.hash().asBytes();
        }
        else if (hash == ChecksumType.SHA256) {
            hashStream = new HashingInputStream(Hashing.sha256(), Streams.create(Bodies.custom(body.length(), body)));
            while (hashStream.read(buffer) != -1) {
            }
            hashStream.close();
            return hashStream.hash().asBytes();
        }
        else if (hash == ChecksumType.CRC32) {
            cis = new CheckedInputStream(Streams.create(Bodies.custom(body.length(), body)), new CRC32());
            while (cis.read(buffer) != -1) {
            }
            cis.close();
            long checksumValue = cis.getChecksum().getValue();
            ByteBuffer byteBuffer = ByteBuffer.allocate(4); // 32 bits checksum
            byteBuffer.putInt((int)checksumValue);
            return byteBuffer.array();

        }
        else if (hash == ChecksumType.CRC32C) {
            cis = new CheckedInputStream(Streams.create(Bodies.custom(body.length(), body)), new CRC32C());
            while (cis.read(buffer) != -1) {
            }
            cis.close();
            long checksumValue = cis.getChecksum().getValue();
            ByteBuffer byteBuffer = ByteBuffer.allocate(4); // 32 bits
            byteBuffer.putInt((int)checksumValue);
            return byteBuffer.array();
        }
        else if (hash == ChecksumType.CRC64NVME) {
            cis = new CheckedInputStream(Streams.create(Bodies.custom(body.length(), body)), new CRC64NVME());
            while (cis.read(buffer) != -1) {
            }
            cis.close();
            long checksumValue = cis.getChecksum().getValue();
            ByteBuffer byteBuffer = ByteBuffer.allocate(Long.SIZE/8);
            byteBuffer.putLong(checksumValue);
            return byteBuffer.array();
        } else {
            throw new IllegalArgumentException(String.format("Unsupported Digest Algorithm: %s", hash));
        }
    }
}