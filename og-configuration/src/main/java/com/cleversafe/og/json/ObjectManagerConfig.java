/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json;

public class ObjectManagerConfig {
  public String objectFileLocation;
  public String objectFileName;

  public ObjectManagerConfig() {
    this.objectFileLocation = "./object";
    this.objectFileName = null;
  }
}
