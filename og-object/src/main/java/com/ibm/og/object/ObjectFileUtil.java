/* Copyright (c) IBM Corporation 2017. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ObjectFileUtil {

    public static final int OBJECT_SIZE_V1_0 = 30;

    /**
     *
     * @param is ObjectFile InputStream
     * @return
     * @throws IOException
     */
    public static ObjectFileVersion readObjectFileVersion(InputStream is) throws IOException {

        // Format VERSION:V1.0
        is.mark(ObjectFileVersion.VERSION_HEADER_LENGTH);
        byte[] versionBuffer = new byte[ObjectFileVersion.VERSION_HEADER_LENGTH];
        is.read(versionBuffer);
        is.reset();
        ObjectFileVersion objectFileVersion = ObjectFileVersion.fromBytes(versionBuffer);
        return objectFileVersion;
    }

    /**
     *
     * @param os - output stream to write the Object File Version
     * @throws IOException
     */

    public static void writeObjectFileVersion(OutputStream os) throws IOException {
        // format VERSION:1.0
        ObjectFileVersion version = ObjectFileVersion.fromMetadata(LegacyObjectMetadata.MAJOR_VERSION,
                LegacyObjectMetadata.MINOR_VERSION);
        os.write(version.getBytes());
    }

    public static LegacyObjectMetadata getObjectFromInputBuffer(int majorVersion, int minorVersion,
                                                                byte[] inputBytes, byte[] objectBytes) {
        ByteBuffer outputBuffer = ByteBuffer.wrap(objectBytes);
        if (majorVersion == 1 && minorVersion <= 0) {
            outputBuffer.put(inputBytes);
            outputBuffer.put((byte)0); //legalholds
            outputBuffer.putInt(-1); // retention
            LegacyObjectMetadata id = LegacyObjectMetadata.fromBytes(objectBytes);
            return id;
        } else if (majorVersion == LegacyObjectMetadata.MAJOR_VERSION &&
                minorVersion == LegacyObjectMetadata.MINOR_VERSION) {
            outputBuffer.put(inputBytes);
            LegacyObjectMetadata id = LegacyObjectMetadata.fromBytes(objectBytes);
            return id;
        } else {
            throw new IllegalArgumentException("Unsupported Object File version");
        }
    }

    public static int getVersionHeaderLength(int majorVersion, int minorVersion) throws IOException {
        if (majorVersion == LegacyObjectMetadata.MAJOR_VERSION && minorVersion == LegacyObjectMetadata.MINOR_VERSION) {
            return ObjectFileVersion.VERSION_HEADER_LENGTH;
        }
        else if (majorVersion == 1 && minorVersion == 0) {
            return 0;
        } else {
            throw new IllegalArgumentException("Unsupported object file version");
        }
    }

    public static byte[] allocateObjectBuffer(int majorVersion, int minorVersion, InputStream in)
            throws IllegalArgumentException {
        if (majorVersion == LegacyObjectMetadata.MAJOR_VERSION && minorVersion == LegacyObjectMetadata.MINOR_VERSION) {
            return new byte[LegacyObjectMetadata.OBJECT_SIZE];
        } else if (majorVersion == 1 && minorVersion == 0) {
            return new byte[ObjectFileUtil.OBJECT_SIZE_V1_0];
        } else {
            throw new IllegalArgumentException("Unsupported object file version");
        }
    }



}
