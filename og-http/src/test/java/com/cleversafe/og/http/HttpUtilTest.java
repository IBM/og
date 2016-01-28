/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.cleversafe.og.api.Method;
import com.cleversafe.og.util.Operation;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class HttpUtilTest {
  @Test(expected = UnsupportedOperationException.class)
  public void modifyStatusCodes() {
    HttpUtil.SUCCESS_STATUS_CODES.add(500);
  }

  @Test(expected = NullPointerException.class)
  public void nullMethod() {
    HttpUtil.toOperation(null);
  }

  @DataProvider
  public static Object[][] provideToOperation() {
    return new Object[][] {{Method.PUT, Operation.WRITE}, {Method.POST, Operation.WRITE},
        {Method.GET, Operation.READ}, {Method.HEAD, Operation.METADATA},
        {Method.DELETE, Operation.DELETE}};
  }

  @Test
  @UseDataProvider("provideToOperation")
  public void toOperation(final Method method, final Operation operation) {
    assertThat(HttpUtil.toOperation(method), is(operation));
  }
}
