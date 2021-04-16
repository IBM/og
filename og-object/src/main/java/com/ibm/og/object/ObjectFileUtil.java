/* Copyright (c) IBM Corporation 2017. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;


import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static com.ibm.og.object.RandomObjectPopulator.OBJECT_SIZE_V1;
import static com.ibm.og.object.RandomObjectPopulator.OBJECT_SIZE_V2;

public class ObjectFileUtil {

    public static final int OBJECT_SIZE_V1_0 = 30;
    private static final Logger _logger = LoggerFactory.getLogger(RandomObjectPopulator.class);

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
     * @param is ObjectFile InputStream
     * @return
     * @throws IOException
     */
    public static ObjectFileHeader readObjectFileHeader(InputStream is) throws IOException {
        is.mark(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.OBJECT_FILE_HEADER_BYTES);
        is.skip(ObjectFileVersion.VERSION_HEADER_LENGTH);
        byte[] objectFileHeaderLength = new byte[1];
        is.read(objectFileHeaderLength);
        int headerLength = objectFileHeaderLength[0];
        byte[] headerBytes = new byte[headerLength];
        headerBytes[0] = (byte)headerLength;
        is.read(headerBytes,1, headerLength-1);
        is.reset();
        ObjectFileHeader header = new ObjectFileHeader(headerBytes);
        return header;
    }

    /**
     *  Get objectsize in the object file
     *
     * @param objectFile - object file
     *
     * @return size of the object record in the object file
     *
     */
    public static int getObjectSize(File objectFile) throws IOException{
        InputStream is = new BufferedInputStream(new FileInputStream(objectFile));
        ObjectFileVersion version = readObjectFileVersion(is);

        if (version.getMajorVersion() == 3 && version.getMinorVersion() == 0) {
            ObjectFileHeader header = readObjectFileHeader(is);
            return header.getObjectNameLen() + header.getObjectVersionLen() +
                    header.getObjectSizeLen() + header.getObjectSuffixLen() +
                    header.getObjectLegalHoldsLen() + header.getObjectRetentionLen();
        }

        if (version.getMajorVersion() == 2 && version.getMinorVersion() == 0) {
            return RandomObjectPopulator.OBJECT_SIZE_V2;
        }

        if (version.getMajorVersion() == 1 && version.getMinorVersion() == 0) {
            return RandomObjectPopulator.OBJECT_SIZE_V1;
        }
        throw new IllegalArgumentException("Unsupported Object file version");
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

    /**
     *
     * @param os - output stream to write the Object File Version
     * @throws IOException
     */

    public static void writeObjectFileVersion(OutputStream os, int majorVersion, int minorVersion) throws IOException {
        // format VERSION:1.0
        ObjectFileVersion version = ObjectFileVersion.fromMetadata((byte)majorVersion, (byte)minorVersion);
        os.write(version.getBytes());
    }

    /**
     *
     * @param os - output stream to write the Object File Version
     * @throws IOException
     */

    public static void writeObjectFileHeader(OutputStream os, int objectVersionSize) throws IOException {
        ObjectFileHeader header = ObjectFileHeader.fromMetadata(LegacyObjectMetadata.OBJECT_NAME_SIZE,
                (byte)objectVersionSize, LegacyObjectMetadata.OBJECT_SIZE_SIZE, LegacyObjectMetadata.OBJECT_SUFFIX_SIZE,
                LegacyObjectMetadata.OBJECT_LEGAL_HOLDS_SIZE, LegacyObjectMetadata.OBJECT_RETENTION_SIZE);
        ;
        os.write(header.getBytes());
    }

    public static LegacyObjectMetadata getObjectFromInputBuffer(int majorVersion, int minorVersion,
                                                                byte[] inputBytes, byte[] objectBytes,
                                                                boolean objectVersionPresent) {
        ByteBuffer outputBuffer = ByteBuffer.wrap(objectBytes);
        if (majorVersion == 1 && minorVersion <= 0) {
            outputBuffer.put(inputBytes);
            outputBuffer.put((byte)0); //legalholds
            outputBuffer.putInt(-1); // retention
            LegacyObjectMetadata id = LegacyObjectMetadata.fromBytes(objectBytes, false);
            return id;
        } else if (majorVersion == 2 && minorVersion == 0) {
            outputBuffer.put(inputBytes);
            LegacyObjectMetadata id = LegacyObjectMetadata.fromBytes(objectBytes, false);
            return id;
        }  else if (majorVersion == LegacyObjectMetadata.MAJOR_VERSION &&
                minorVersion == LegacyObjectMetadata.MINOR_VERSION) {
            outputBuffer.put(inputBytes);
            if (!objectVersionPresent) {
                LegacyObjectMetadata id = LegacyObjectMetadata.fromBytes(objectBytes, false);
                return id;
            } else {
                LegacyObjectMetadata id = LegacyObjectMetadata.fromBytes(objectBytes,true);
                return id;
            }
        } else {
            throw new IllegalArgumentException("Unsupported Object File version");
        }
    }

    public static int getVersionHeaderLength(int majorVersion, int minorVersion) throws IOException {

        if (majorVersion == 1 && minorVersion == 0) {
            return 0;
        } else if ((majorVersion > 1 && majorVersion <= 2) &&
                (minorVersion <= LegacyObjectMetadata.MINOR_VERSION)) {
            return ObjectFileVersion.VERSION_HEADER_LENGTH;
        } else if ((majorVersion > 1 && majorVersion <= LegacyObjectMetadata.MAJOR_VERSION) &&
                (minorVersion <= LegacyObjectMetadata.MINOR_VERSION)) {
            return ObjectFileVersion.VERSION_HEADER_LENGTH;
        } else {
            throw new IllegalArgumentException("Unsupported object file version");
        }
    }

    public static byte[] allocateObjectBuffer(int majorVersion, int minorVersion, ObjectFileHeader header)
            throws IllegalArgumentException {
        if (majorVersion == LegacyObjectMetadata.MAJOR_VERSION && minorVersion == LegacyObjectMetadata.MINOR_VERSION) {
            int objectVersionLen = header.getObjectVersionLen();
            return new byte[LegacyObjectMetadata.OBJECT_SIZE + objectVersionLen];
        } else if (majorVersion == 2 && minorVersion == 0) {
            return new byte[LegacyObjectMetadata.OBJECT_SIZE];
        } else if (majorVersion == 1 && minorVersion == 0) {
            return new byte[ObjectFileUtil.OBJECT_SIZE_V1_0];
        } else {
            throw new IllegalArgumentException("Unsupported object file version");
        }
    }


    // adapt Bytestreams.readFully to return a boolean rather than throwing an exception
    public static boolean readFully(final InputStream in, final byte[] b) throws IOException {
        try {
            ByteStreams.readFully(in, b);
        } catch (final EOFException e) {
            // FIXME deal with the case where bytes not divisible by b.size, rather than regular EOF
            return false;
        }
        return true;
    }

    /**
     * Converts the given object file to a version 3.0 format
     *
     * @param inputFile - InputFile that needs to be converted
     * @param needObjectVersion - If the converted file has objectVersion field
     *
     * @throws IOException
     */
    public static void upgrade(final File inputFile,  boolean needObjectVersion) throws IOException {


        String tempFileName = inputFile.getName().concat(".tmp");
        File outputFile = new File(inputFile.getParentFile(), tempFileName);
        outputFile.delete();


        final BufferedInputStream in = new BufferedInputStream(new FileInputStream(inputFile));
        // check the version of the file and calculate skip
        long skip = 0;
        byte[] readBuf = null;
        int actualObjectSize = 0;
        in.mark(ObjectFileVersion.VERSION_HEADER_LENGTH);
        ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(in);
        boolean conversionRequired = false;
        boolean objectVersionPresent = false;
        byte[] objectBuf = new byte[actualObjectSize]; // correct size will be allocated based on the borrowed object
        if (version.getMajorVersion() == 3 && version.getMinorVersion() == 0) {
            _logger.info("converting object file {} of length {} version 3.0 ", inputFile.getName(),
                    inputFile.length());
            // calculate object size by reading file header
            ObjectFileHeader header = ObjectFileUtil.readObjectFileHeader(in);
            int objectSize = LegacyObjectMetadata.getObjectRecordSize(header);

            if (header.getObjectVersionLen() > 0) {
                objectVersionPresent = true;
            }
            if (!objectVersionPresent && needObjectVersion) {
                // conversion is necessary
                _logger.info("{} Version: {} Version Field {}. upgrade it for writing versioned object",
                        inputFile.getName(), "3.0", objectVersionPresent);
                conversionRequired = true;
                skip = ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH;
                readBuf = new byte[objectSize];
                actualObjectSize = objectSize;
                objectBuf = new byte[actualObjectSize];
            }
        } else if (version.getMajorVersion() == 2 && version.getMinorVersion() == 0) {
            _logger.info("upgrade object file {} of length {} version 2.0. object version: {} ", inputFile.getName(),
                    inputFile.length(), needObjectVersion);
            conversionRequired = true;
            skip = ObjectFileVersion.VERSION_HEADER_LENGTH;
            readBuf = new byte[OBJECT_SIZE_V2];
            actualObjectSize = OBJECT_SIZE_V2;
            objectBuf = new byte[actualObjectSize];
        } else if (version.getMajorVersion() == 1 && version.getMinorVersion() == 0) {
            _logger.info("upgrade object file {} of length {} version 1.0. object version {} ", inputFile.getName(),
                    inputFile.length(), needObjectVersion);
            conversionRequired = true;
            if (in.markSupported()) {
                _logger.warn("Missing version in object file [%s].", inputFile.getName());
                readBuf = new byte[OBJECT_SIZE_V1];
                actualObjectSize = OBJECT_SIZE_V1;
                objectBuf = new byte[OBJECT_SIZE_V2];
                // allocate objectBuf size to include the retention, legalholds.
                // ObjectFileUtil.getObjectFromInputBuffer fills in default values for retention and legalholds.
            }
            skip = 0;
        } else {
            in.close();
            throw new IllegalArgumentException("Unsupported Object file version");
        }

        if (conversionRequired) {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile));
            ObjectFileUtil.writeObjectFileVersion(bos);
            if (needObjectVersion) {
                ObjectFileUtil.writeObjectFileHeader(bos, LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
            } else {
                ObjectFileUtil.writeObjectFileHeader(bos, 0);
            }
            in.reset();
            _logger.info("skip object file length [{}]", skip);
            long skippedBytes = 0;
            while(skippedBytes < skip) {
                skippedBytes += in.skip(skip - skippedBytes);
            }
            _logger.info("skippedBytes [{}]", skippedBytes);
            final byte[] buf = new byte[actualObjectSize];
            ObjectMetadata sid;
            while (readFully(in, readBuf)) {
                sid = ObjectFileUtil.getObjectFromInputBuffer(version.getMajorVersion(), version.getMinorVersion(),
                        readBuf, objectBuf, objectVersionPresent);
                // convert the object record with version if it needs object version or
                // if the object version is already present in the file being converted
                bos.write(sid.toBytes(needObjectVersion || objectVersionPresent));
            }
            in.close();
            bos.close();
            Files.move(outputFile, inputFile);
        } else {
            _logger.info("{} Version: {}.{} Version Field: {}. No upgrade needed",
                    inputFile.getName(), version.getMajorVersion(), version.getMinorVersion(),
                    objectVersionPresent);
        }
    }

}
