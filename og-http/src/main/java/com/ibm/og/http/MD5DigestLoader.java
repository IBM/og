/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.http;

import com.google.common.cache.CacheLoader;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.ibm.og.util.io.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *  A cache to store content-md5, used in case of zero body
 *
 *  @since 1.3.0
 */
public class MD5DigestLoader extends CacheLoader<Long, byte[]> {
    private static Logger _logger = LoggerFactory.getLogger(MD5DigestLoader.class);

    @Override
    public byte[] load(final Long key) throws Exception {
        checkNotNull(key);
        _logger.debug("Loading md5 digest for size [{}]", key);

        final HashingInputStream hashStream =
                new HashingInputStream(Hashing.md5(), Streams.create(Bodies.zeroes(key)));
        final byte[] buffer = new byte[4096];
        while (hashStream.read(buffer) != -1) {
        }
        // should never throw an exception since the source is from Streams.create
        hashStream.close();
        return hashStream.hash().asBytes();
    }
}