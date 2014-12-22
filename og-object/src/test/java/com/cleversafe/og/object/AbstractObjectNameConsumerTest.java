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

package com.cleversafe.og.object;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpUtil;
import com.cleversafe.og.util.Pair;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public abstract class AbstractObjectNameConsumerTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  protected ObjectManager objectManager;
  protected List<Integer> statusCodes;
  protected String object;
  protected Request request;
  protected Response response;
  protected Pair<Request, Response> operation;
  protected AbstractObjectNameConsumer objectNameConsumer;

  @Before
  public void before() throws URISyntaxException {
    this.objectManager = mock(ObjectManager.class);
    this.statusCodes = HttpUtil.SUCCESS_STATUS_CODES;
    this.object = "5c18be1057404792923dc487ca40f2370000";

    this.request = mock(Request.class);
    when(this.request.getMethod()).thenReturn(method());
    when(this.request.getUri()).thenReturn(new URI("/container/" + this.object));
    when(this.request.headers()).thenReturn(ImmutableMap.of(Headers.X_OG_OBJECT_NAME, this.object));

    this.response = mock(Response.class);
    when(this.response.getStatusCode()).thenReturn(200);
    when(this.response.headers()).thenReturn(ImmutableMap.of(Headers.X_OG_REQUEST_ID, "1"));

    this.operation = Pair.of(this.request, this.response);
    this.objectNameConsumer = create(this.objectManager, this.statusCodes);
  }

  public abstract AbstractObjectNameConsumer create(ObjectManager objectManager,
      List<Integer> statusCodes);

  public abstract Method method();

  public abstract void doVerify();

  public abstract void doVerifyNever();

  public abstract void doThrowIt();

  @DataProvider
  public static Object[][] provideInvalidObjectNameConsumer() {
    final ObjectManager objectManager = mock(ObjectManager.class);
    final List<Integer> statusCodes = HttpUtil.SUCCESS_STATUS_CODES;
    final List<Integer> nullElement = Lists.newArrayList();
    nullElement.add(null);

    return new Object[][] { {null, statusCodes, NullPointerException.class},
        {objectManager, null, NullPointerException.class},
        {objectManager, ImmutableList.of(), IllegalArgumentException.class},
        {objectManager, nullElement, NullPointerException.class},
        {objectManager, ImmutableList.of(99), IllegalArgumentException.class},
        {objectManager, ImmutableList.of(600), IllegalArgumentException.class}};
  }

  @Test
  @UseDataProvider("provideInvalidObjectNameConsumer")
  public void invalidObjectNameConsumer(final ObjectManager objectManager,
      final List<Integer> statusCodes, final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    create(objectManager, statusCodes);
  }

  @Test(expected = NullPointerException.class)
  public void nullOperation() {
    this.objectNameConsumer.consume(null);
  }

  @Test
  public void successful() {
    this.objectNameConsumer.consume(this.operation);
    doVerify();
  }

  @Test
  public void unsuccessful() {
    when(this.response.getStatusCode()).thenReturn(500);
    this.objectNameConsumer.consume(this.operation);
    doVerifyNever();
  }

  @Test
  public void operationDoesNotMatchMethod() {
    when(this.request.getMethod()).thenReturn(Method.DELETE);
    this.objectNameConsumer.consume(this.operation);
    doVerifyNever();
  }

  @Test
  public void statusCodeModification() {
    final List<Integer> mutable = Lists.newArrayList();
    mutable.add(200);
    final AbstractObjectNameConsumer consumer = create(this.objectManager, mutable);
    mutable.clear();
    consumer.consume(this.operation);
    doVerify();
  }

  @Test(expected = ObjectManagerException.class)
  public void objectManagerException() {
    doThrowIt();
    this.objectNameConsumer.consume(this.operation);
  }

  @Test(expected = IllegalStateException.class)
  public void noObject() {
    when(this.request.headers()).thenReturn(ImmutableMap.<String, String>of());
    this.objectNameConsumer.consume(this.operation);
  }
}
