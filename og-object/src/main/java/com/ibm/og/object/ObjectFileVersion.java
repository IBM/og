/* Copyright (c) IBM Corporation 2017. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;


import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ObjectFileVersion {
    public static final String VERSION_HEADER_PREFIX = "VERSION:";
    public static final byte[] VERSION_HEADER_PREFIX_BYTES = VERSION_HEADER_PREFIX.getBytes();
    public static final int VERSION_HEADER_PREFIX_LENGTH = VERSION_HEADER_PREFIX.getBytes().length;

    public static final int SIZE_MAJOR_VERSION = 1; // size in bytes
    public static final int SIZE_MINOR_VERSION = 1; // size in bytes
    public static final int VERSION_HEADER_LENGTH = VERSION_HEADER_PREFIX_LENGTH + SIZE_MAJOR_VERSION +
            SIZE_MINOR_VERSION;
    public static final int MAJOR_VERSION_OFFSET = VERSION_HEADER_PREFIX_LENGTH;
    public static final int MINOR_VERSION_OFFSET = VERSION_HEADER_PREFIX_LENGTH + SIZE_MAJOR_VERSION;

    private final ByteBuffer versionBuffer;
    private byte majorVersion;
    private byte minorVersion;

    public ObjectFileVersion(final ByteBuffer versionBuffer) {
        this.versionBuffer = versionBuffer;
    }

    public static ObjectFileVersion fromBytes(final byte[] versionBytes) {
        checkNotNull(versionBytes);
        checkArgument(versionBytes.length == VERSION_HEADER_LENGTH,
                String.format("version header length must be == %s", VERSION_HEADER_LENGTH) + " [%s]",
                versionBytes.length);
        ByteBuffer byteBuffer = ByteBuffer.wrap(versionBytes);
        int bytesToSkip = VERSION_HEADER_PREFIX.getBytes().length;
        byte[] v1 = new byte[bytesToSkip];
        byteBuffer.get(v1, 0, bytesToSkip);

        if (!Arrays.equals(VERSION_HEADER_PREFIX_BYTES, v1)) {
            // header is missing. assume pre-worm version i.e 1.0
            ByteBuffer versionBytesBuffer = ByteBuffer.allocate(VERSION_HEADER_LENGTH);
            versionBytesBuffer.put(VERSION_HEADER_PREFIX_BYTES);
            versionBytesBuffer.put((byte)1); //major version
            versionBytesBuffer.put((byte)0); //minor version
            ObjectFileVersion objectFileVersion = new ObjectFileVersion(versionBytesBuffer);
            return objectFileVersion;

        } else {
            ByteBuffer versionBytesBuffer = ByteBuffer.allocate(versionBytes.length).put(versionBytes);
            ObjectFileVersion objectFileVersion = new ObjectFileVersion(versionBytesBuffer);
            objectFileVersion.setMajorVersion(versionBytesBuffer.get(MAJOR_VERSION_OFFSET));
            objectFileVersion.setMinorVersion(versionBytesBuffer.get(MINOR_VERSION_OFFSET));
            return objectFileVersion;
        }
    }

    public static ObjectFileVersion fromMetadata(byte majorVersion, byte minorVersion) {
        ByteBuffer b = ByteBuffer.allocate(VERSION_HEADER_LENGTH);
        b.put(VERSION_HEADER_PREFIX_BYTES);
        b.put(majorVersion);
        b.put(minorVersion);
        return new ObjectFileVersion(b);
    }

    public static ObjectFileVersion fromCharString(String line) {
        if (!line.startsWith(ObjectFileVersion.VERSION_HEADER_PREFIX)) {
            // pre-worm. VERSION:1.0
            ByteBuffer versionBytesBuffer = ByteBuffer.allocate(VERSION_HEADER_LENGTH);
            versionBytesBuffer.put(VERSION_HEADER_PREFIX_BYTES);
            versionBytesBuffer.put((byte)1); //major version
            versionBytesBuffer.put((byte)0); //minor version
            ObjectFileVersion objectFileVersion = new ObjectFileVersion(versionBytesBuffer);
            return objectFileVersion;
        } else {
            ByteBuffer b = ByteBuffer.allocate(line.getBytes().length);
            String[] components = line.split(":");
            checkArgument(components.length == 2, "Invalid verson header - %s", line);
            components = components[1].split("\\.");
            int majorVersion = Integer.parseInt(components[0]);
            int minorVersion = Integer.parseInt(components[1]);
            ByteBuffer versionBytesBuffer = ByteBuffer.allocate(VERSION_HEADER_LENGTH);
            versionBytesBuffer.put(VERSION_HEADER_PREFIX_BYTES);
            versionBytesBuffer.put((byte)majorVersion); //major version
            versionBytesBuffer.put((byte)minorVersion); //minor version
            ObjectFileVersion objectFileVersion = new ObjectFileVersion(versionBytesBuffer);
            return objectFileVersion;
        }
    }

    public static int getPlaintextVersionHeaderLength() {
        String v = VERSION_HEADER_PREFIX + LegacyObjectMetadata.MAJOR_VERSION + "." +
                LegacyObjectMetadata.MAJOR_VERSION + "\n";
        return v.getBytes().length;
    }

    private void setMinorVersion(byte minorVersion) {
        this.minorVersion = minorVersion;
    }

    private void setMajorVersion(byte majorVersion) {
        this.majorVersion = majorVersion;
    }

    public byte getMajorVersion() {
        return versionBuffer.get(MAJOR_VERSION_OFFSET);
    }

    public byte getMinorVersion() {
        return versionBuffer.get(MINOR_VERSION_OFFSET);
    }

    public byte[] getBytes() {
        return this.versionBuffer.array();
    }

}
