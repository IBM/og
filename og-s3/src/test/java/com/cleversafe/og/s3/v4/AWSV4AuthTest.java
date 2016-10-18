/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.s3.v4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import com.amazonaws.util.BinaryUtils;
import com.cleversafe.og.api.DataType;
import com.cleversafe.og.s3.v4.AWSV4Auth.DigestLoader;

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

  @Test
  public void digestLoaderCacheZeroDigest() throws Exception {
    final DigestLoader loader = new AWSV4Auth.DigestLoader();
    assertThat(BinaryUtils.toHex(loader.load(0L)),
        is("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));
  }
}


