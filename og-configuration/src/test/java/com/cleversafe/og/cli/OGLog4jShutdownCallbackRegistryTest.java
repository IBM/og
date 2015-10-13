/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.cleversafe.og.cli.OGLog4jShutdownCallbackRegistry.IncompleteGzCompressAction;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class OGLog4jShutdownCallbackRegistryTest {

  @Test(expected = NullPointerException.class)
  public void nullSetOGShutdownHook() {
    OGLog4jShutdownCallbackRegistry.setOGShutdownHook(null);
  }

  @Test
  public void setOGShutdownHook() {
    OGLog4jShutdownCallbackRegistry.setOGShutdownHook(new Runnable() {
      @Override
      public void run() {}
    });
  }

  @DataProvider
  public static Object[][] provideIncompleteGzCompressActionFilter() {
    return new Object[][] {{"request.log", false}, {"request.log-", false},
        {"request.log-1.gz", false}, {"request.log-10.gz", false}, {"request.log-100.gz", false},
        {"request.log-1", true}, {"request.log-10", true}, {"request.log-100", true}};
  }

  @Test
  @UseDataProvider("provideIncompleteGzCompressActionFilter")
  public void incompleteGzCompressActionFilter(final String name, final boolean acceptResult) {
    assertThat(new IncompleteGzCompressAction().accept(null, name), is(acceptResult));
  }
}
