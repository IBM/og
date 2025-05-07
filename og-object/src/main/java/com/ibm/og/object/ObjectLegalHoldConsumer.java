/* Copyright (c) IBM Corporation 2022. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.object;


import com.ibm.og.api.Operation;

import java.util.Set;

public class ObjectLegalHoldConsumer extends AbstractObjectNameConsumer {
    /**
     * Constructs an instance
     *
     * @param objectManager the object manager for this instance to work with
     * @param statusCodes the status codes this instance should work with
     * @throws IllegalArgumentException if any status code in status codes is invalid
     */
    public ObjectLegalHoldConsumer(final ObjectManager objectManager,
                                   final Set<Integer> statusCodes) {
        super(objectManager, Operation.PUT_OBJECT_LOCK_LEGAL_HOLD, statusCodes);
    }

    @Override
    protected void updateObjectManager(final ObjectMetadata objectName) {
        this.objectManager.updateObject(objectName);
    }


    @Override
    public String toString() {
        return "ObjectLegalHoldConsumer []";
    }
}
