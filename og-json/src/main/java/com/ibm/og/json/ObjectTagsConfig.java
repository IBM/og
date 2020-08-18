/* Copyright (c) IBM Corporation 2020. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json;

import com.google.common.base.Strings;

public class ObjectTagsConfig {

  public ObjectTag[] tags;

  public ObjectTagsConfig() {
  }

  public class ObjectTag {
    final String keyChar;
    final int keyLength;
    final String valueChar;
    final int valueLength;

    public ObjectTag(String keyChar, int keyLength, String valueChar, int valueLength) {
      this.keyChar = keyChar;
      this.keyLength = keyLength;
      this.valueChar = valueChar;
      this.valueLength = valueLength;
    }

    public String getKey() {
      return Strings.padEnd("", keyLength, keyChar.charAt(0));
    }

    public String getValue() {
      return Strings.padEnd("", valueLength, valueChar.charAt(0));
    }

  }
}
