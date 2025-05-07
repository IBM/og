/* Copyright (c) IBM Corporation 2022. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.supplier;

import com.google.common.base.Function;
import com.ibm.og.object.ObjectManager;
import com.ibm.og.object.ObjectMetadata;
import com.ibm.og.util.Context;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.io.BaseEncoding;
/**
 * A function which generates object names for read from a provided {@code ObjectManager}
 *
 * @since 1.14
 */
public class ObjectPutLegalHold implements Function<Map<String, String>, String> {
    private final ObjectManager objectManager;
    private static final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();

    /**
     * Creates an instance
     *
     * @param objectManager the object manager to draw object names from
     * @throws NullPointerException if objectManager is null
     */
    public ObjectPutLegalHold(final ObjectManager objectManager) {
        this.objectManager = checkNotNull(objectManager);
    }

    /**
     * returns an object from object manager. Additionally, inserts the following entries into the
     * context:
     * <ul>
     * <li>Context.X_OG_OBJECT_NAME</li>
     * <li>Context.X_OG_OBJECT_VERSION</li>
     * <li>Context.X_OG_OBJECT_SIZE</li>
     * <li>Context.X_OG_CONTAINER_SUFFIX</li>
     * </li>Context.Context.X_OG_LEGAL_HOLD_SUFFIX</li>
     * <li>Context.X_OG_OBJECT_RETENTION</li>
     * </ul>
     *
     * @param context a request creation context for storing metadata to be used by other functions
     */
    @Override
    public String apply(final Map<String, String> context) {
        final ObjectMetadata objectMetadata = this.objectManager.removeForUpdate();
        context.put(Context.X_OG_OBJECT_NAME, objectMetadata.getName());
        String objectVersion = objectMetadata.getVersion();
        if (objectVersion != null) {
            byte[] buffer = new byte[16];
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            byteBuffer.put(ENCODING.decode(objectVersion), 0, 16);
            UUID uuid = new UUID(byteBuffer.getLong(0), byteBuffer.getLong(8));
            if (uuid.getMostSignificantBits() != 0 && uuid.getLeastSignificantBits() != 0) {
                context.put(Context.X_OG_OBJECT_VERSION, uuid.toString());
            }
        }
        context.put(Context.X_OG_OBJECT_SIZE, String.valueOf(objectMetadata.getSize()));
        context.put(Context.X_OG_CONTAINER_SUFFIX, String.valueOf(objectMetadata.getContainerSuffix()));
        context.put(Context.X_OG_LEGAL_HOLD_SUFFIX, String.valueOf(objectMetadata.getNumberOfLegalHolds()));
        context.put(Context.X_OG_OBJECT_RETENTION, String.valueOf(objectMetadata.getRetention()));

        return objectMetadata.getName();
    }

    @Override
    public String toString() {
        return "ObjectPutRetention []";
    }
}

