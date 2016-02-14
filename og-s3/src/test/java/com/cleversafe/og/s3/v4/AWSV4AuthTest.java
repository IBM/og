/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.s3.v4;

import org.junit.Test;

import com.cleversafe.og.api.DataType;

public class AWSV4AuthTest {
  @Test(expected = IllegalArgumentException.class)
  public void negativeCacheSize() {
    new AWSV4Auth(false, -1, DataType.ZEROES);
  }

  @Test(expected = NullPointerException.class)
  public void nullData() {
    new AWSV4Auth(false, 1, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void noneData() {
    new AWSV4Auth(false, 1, DataType.NONE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void positiveCacheSizeNonZeroesData() {
    new AWSV4Auth(false, 1, DataType.RANDOM);
  }
}


