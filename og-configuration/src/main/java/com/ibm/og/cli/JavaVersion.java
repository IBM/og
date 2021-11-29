/* Copyright (c) IBM Corporation 2019. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.cli;

public class JavaVersion {

  public static void main(final String[] args) {
    String version = System.getProperty("java.version");
    String.format("Java version: %s", version);
    if (version.startsWith("1.6")) {
      System.exit(6);
    }
    if (version.startsWith("1.7")) {
      System.exit(7);
    }
    if (version.startsWith("1.8")) {
      System.exit(8);
    }
    if (version.startsWith("9")) {
      System.exit(9);
    }
    if (version.startsWith("10")) {
      System.exit(10);
    }
    if (version.startsWith("11")) {
      System.exit(11);
    }
    if (version.startsWith("12")) {
      System.exit(12);
    }
    if (version.startsWith("13")) {
      System.exit(13);
    }
    if (version.startsWith("14")) {
      System.exit(14);
    }
    if (version.startsWith("15")) {
      System.exit(15);
    }
    if (version.startsWith("16")) {
      System.exit(16);
    }
    if (version.startsWith("17")) {
      System.exit(17);
    }
    System.exit(-1);
  }
}
