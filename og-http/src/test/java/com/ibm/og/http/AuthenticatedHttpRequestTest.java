/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Test;

import com.ibm.og.api.Method;
import com.ibm.og.api.Operation;
import com.ibm.og.api.Request;

public class AuthenticatedHttpRequestTest {
  private final Request request;

  public AuthenticatedHttpRequestTest() throws URISyntaxException {
    this.request =
        new HttpRequest.Builder(Method.PUT, new URI("/container/object"), Operation.WRITE).build();
  }

  @Test(expected = NullPointerException.class)
  public void nullRequest() {
    new AuthenticatedHttpRequest(null);
  }

  @Test(expected = NullPointerException.class)
  public void addQueryParameterNullKey() {
    final AuthenticatedHttpRequest authenticatedRequest =
        new AuthenticatedHttpRequest(this.request);
    authenticatedRequest.addQueryParameter(null, "value");
  }

  @Test
  public void addQueryParameterNullValue() {
    final AuthenticatedHttpRequest authenticatedRequest =
        new AuthenticatedHttpRequest(this.request);
    authenticatedRequest.addQueryParameter("key", null);
  }

  @Test
  public void addOneQueryParameter() {
    final AuthenticatedHttpRequest authenticatedRequest =
        new AuthenticatedHttpRequest(this.request);
    authenticatedRequest.addQueryParameter("key", "value");
    assertThat(authenticatedRequest.getQueryParameters().size(), is(1));
    final List<String> values = authenticatedRequest.getQueryParameters().get("key");
    assertThat(values.size(), is(1));
    assertThat(values.get(0), is("value"));
  }

  @Test
  public void addMultipleQueryParametersSameKey() {
    final AuthenticatedHttpRequest authenticatedRequest =
        new AuthenticatedHttpRequest(this.request);
    authenticatedRequest.addQueryParameter("key", "value1");
    authenticatedRequest.addQueryParameter("key", "value2");
    assertThat(authenticatedRequest.getQueryParameters().size(), is(1));
    final List<String> values = authenticatedRequest.getQueryParameters().get("key");
    assertThat(values.size(), is(2));
    assertThat(values.get(0), is("value1"));
    assertThat(values.get(1), is("value2"));
  }

  @Test
  public void addMultipleQueryParametersDifferentKey() {
    final AuthenticatedHttpRequest authenticatedRequest =
        new AuthenticatedHttpRequest(this.request);
    authenticatedRequest.addQueryParameter("key1", "value1");
    authenticatedRequest.addQueryParameter("key2", "value2");
    assertThat(authenticatedRequest.getQueryParameters().size(), is(2));
    final List<String> values1 = authenticatedRequest.getQueryParameters().get("key1");
    assertThat(values1.size(), is(1));
    assertThat(values1.get(0), is("value1"));
    final List<String> values2 = authenticatedRequest.getQueryParameters().get("key2");
    assertThat(values2.size(), is(1));
    assertThat(values2.get(0), is("value2"));
  }

  @Test(expected = NullPointerException.class)
  public void addHeaderNullKey() {
    final AuthenticatedHttpRequest authenticatedRequest =
        new AuthenticatedHttpRequest(this.request);
    authenticatedRequest.addHeader(null, "value");
  }

  @Test(expected = NullPointerException.class)
  public void addHeaderNullValue() {
    final AuthenticatedHttpRequest authenticatedRequest =
        new AuthenticatedHttpRequest(this.request);
    authenticatedRequest.addHeader("key", null);
  }
}
