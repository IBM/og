//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: rveitch
//
// Date: Jun 29, 2014
// ---------------------

package com.cleversafe.og.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.net.URI;
import java.net.URISyntaxException;

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
    return new Object[][] { {Method.PUT, Operation.WRITE}, {Method.POST, Operation.WRITE},
        {Method.GET, Operation.READ}, {Method.HEAD, Operation.READ},
        {Method.DELETE, Operation.DELETE}};
  }

  @Test
  @UseDataProvider("provideToOperation")
  public void toOperation(final Method method, final Operation operation) {
    assertThat(HttpUtil.toOperation(method), is(operation));
  }

  @Test(expected = NullPointerException.class)
  public void nullUri() {
    HttpUtil.getObjectName(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullScheme() throws URISyntaxException {
    HttpUtil.getObjectName(new URI("192.168.8.1/container"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidScheme() throws URISyntaxException {
    HttpUtil.getObjectName(new URI("ftp://192.168.8.1/container"));
  }

  @Test
  public void rootUriNoObject() throws URISyntaxException {
    assertThat(HttpUtil.getObjectName(new URI("http://192.168.8.1/container")), nullValue());
  }

  @Test
  public void noObject() throws URISyntaxException {
    assertThat(HttpUtil.getObjectName(new URI("http://192.168.8.1/soh/container")), nullValue());
  }

  @DataProvider
  public static Object[][] provideGetObjectName() {
    return new Object[][] { {"https://192.168.8.1/container/object"},
        {"http://192.168.8.1/container/object/"}, {"https://192.168.8.1/soh/container/object"},
        {"http://192.168.8.1/soh/container/object/"}};
  }

  @Test
  @UseDataProvider("provideGetObjectName")
  public void getObjectName(final String uri) throws URISyntaxException {
    assertThat(HttpUtil.getObjectName(new URI(uri)), is("object"));
  }
}
