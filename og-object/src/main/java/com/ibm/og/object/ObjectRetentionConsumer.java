/* Copyright (c) IBM Corporation 2022. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.object;


import com.ibm.og.api.Operation;
import com.ibm.og.api.Request;
import com.ibm.og.api.Response;
import com.ibm.og.util.Context;

import java.util.Set;

public class ObjectRetentionConsumer extends AbstractObjectNameConsumer {
    /**
     * Constructs an instance
     *
     * @param objectManager the object manager for this instance to work with
     * @param statusCodes the status codes this instance should work with
     * @throws IllegalArgumentException if any status code in status codes is invalid
     */
    public ObjectRetentionConsumer(final ObjectManager objectManager,
                                        final Set<Integer> statusCodes) {
        super(objectManager, Operation.PUT_OBJECT_LOCK_RETENTION, statusCodes);
    }

    @Override
    protected void updateObjectManager(final ObjectMetadata objectName) {

        this.objectManager.updateObject(objectName);
    }

    protected int getObjectRetention(final Request request, final Response response) {
        final String sRetention = request.getContext().get(Context.X_OG_OBJECT_RETENTION);
        if (response.getStatusCode() == 200) {
            return Integer.parseInt(sRetention);
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "ObjectRetentionConsumer []";
    }
}
