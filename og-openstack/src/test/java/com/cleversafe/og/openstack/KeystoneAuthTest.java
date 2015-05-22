/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.openstack;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.junit.Test;

import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpAuth;
import com.cleversafe.og.http.HttpRequest;
import com.cleversafe.og.util.io.Streams;

public class KeystoneAuthTest {
  private final HttpAuth keystoneAuth;
  private final Request request;

  public KeystoneAuthTest() throws URISyntaxException {
    this.keystoneAuth = new KeystoneAuth();
    final URI uri = new URI("http://127.0.0.1/openstack/container/object");
    this.request =
        new HttpRequest.Builder(Method.PUT, uri).withHeader(Headers.X_OG_KEYSTONE_TOKEN, "token")
            .withBody(Bodies.random(1024)).build();
  }

  @Test
  public void getAuthorizationHeaders() {
    final Map<String, String> authHeaders = this.keystoneAuth.getAuthorizationHeaders(this.request);
    assertThat(authHeaders, hasEntry("X-Auth-Token", "token"));
  }

  @Test
  public void wrapStream() {
    final InputStream in = Streams.create(this.request.getBody());
    assertThat(this.keystoneAuth.wrapStream(this.request, in), is(in));
  }

  @Test
  public void getContentLength() {
    assertThat(this.keystoneAuth.getContentLength(this.request), is(1024L));
  }
}
