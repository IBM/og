/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;

@RunWith(DataProviderRunner.class)
public class HttpUtilTest {
  @Test(expected = UnsupportedOperationException.class)
  public void modifyStatusCodes() {
    HttpUtil.SUCCESS_STATUS_CODES.add(500);
  }

//  @Test(expected = NullPointerException.class)
//  public void nullMethod() {
//    HttpUtil.toOperation(null);
//  }

//  @DataProvider
//  public static Object[][] provideToOperation() {
//    return new Object[][] {{Method.PUT, Operation.WRITE}, {Method.POST, Operation.WRITE},
//        {Method.GET, Operation.READ}, {Method.HEAD, Operation.METADATA},
//        {Method.DELETE, Operation.DELETE}};
//  }

//  @Test
//  @UseDataProvider("provideToOperation")
//  public void toOperation(final Method method, final Operation operation) {
//    assertThat(HttpUtil.toOperation(method), is(operation));
//  }
}
