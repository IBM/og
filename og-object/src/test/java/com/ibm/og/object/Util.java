package com.ibm.og.object;

import java.util.UUID;

public class Util {

  public static ObjectMetadata generateId() {
    return LegacyObjectMetadata.fromMetadata(UUID.randomUUID().toString().replace("-", "") + "0000",
            0, -1, (byte) 0, -1, null);
  }

  public static ObjectMetadata generateIdV2NoVersion() {
    return LegacyObjectMetadata.fromMetadata(UUID.randomUUID().toString().replace("-", "") + "0000",
            0, -1, (byte) 0, -1, "00000178-4ae8-33c9-91de-1eac9b869048");
  }


  public static ObjectMetadata generateIdV3WithVersion() {
    return LegacyObjectMetadata.fromMetadata(UUID.randomUUID().toString().replace("-", "") + "0000",
            0, -1, (byte) 0, -1,
            UUID.randomUUID().toString().replace("-", ""));
  }

  public static ObjectMetadata generateIdV3WithoutVersion() {
    return LegacyObjectMetadata.fromMetadata(UUID.randomUUID().toString().replace("-", "") + "0000",
            0x100, -1, (byte) 0, -1, null);

  }

}
