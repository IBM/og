/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json;



public class ObjectManagerConfig {
  public String objectFileLocation;
  public String objectFileName;
  public long objectFileMaxSize;
  public long objectFilePersistFrequency;
  public Integer objectFileIndex;

  public ObjectManagerConfig() {
    this.objectFileLocation = "./object";
    this.objectFileName = null;
    this.objectFileMaxSize = 100000000; // 100mb
    this.objectFilePersistFrequency = 1800; // 30 minutes
    this.objectFileIndex = null;
  }
}
