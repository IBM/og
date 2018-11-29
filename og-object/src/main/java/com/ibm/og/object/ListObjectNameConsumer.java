/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;

import java.util.Set;

import com.ibm.og.api.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@code ObjectNameConsumer} implementation which consumes object names for list operations
 *
 * @since 1.0
 */
public class ListObjectNameConsumer extends AbstractObjectNameConsumer {

    private static final Logger _logger = LoggerFactory.getLogger(ListObjectNameConsumer.class);
    /**
     * Constructs an instance
     *
     * @param objectManager the object manager for this instance to work with
     * @param statusCodes the status codes this instance should work with
     * @throws IllegalArgumentException if any status code in status codes is invalid
     */
    public ListObjectNameConsumer(final ObjectManager objectManager, final Set<Integer> statusCodes) {
        super(objectManager, Operation.LIST, statusCodes);
    }

    @Override
    protected void updateObjectManager(final ObjectMetadata objectName) {
        _logger.trace("releasing object {}", objectName.getName());
        this.objectManager.getComplete(objectName);
    }

    @Override
    public String toString() {
        return "ListObjectNameConsumer []";
    }
}
