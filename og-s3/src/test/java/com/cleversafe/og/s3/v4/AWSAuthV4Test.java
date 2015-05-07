/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.s3.v4;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpRequest;
import com.google.common.collect.Maps;

public class AWSAuthV4Test {
  private final URI URI;

  private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";
  private static final String KEY_ID = "AKIDEXAMPLE";

  public AWSAuthV4Test() throws URISyntaxException {
    this.URI = new URI("http://127.0.0.1:8080/container/object");
  }

  @Test
  public void testAuth() throws IOException {
    final int bodySize = 35;
    final AWSAuthV4 auth = new AWSAuthV4("dsnet", "s3");
    final HttpRequest.Builder reqBuilder = new HttpRequest.Builder(Method.PUT, this.URI);
    reqBuilder.withHeader(Headers.X_OG_USERNAME, KEY_ID);
    reqBuilder.withHeader(Headers.X_OG_PASSWORD, SECRET_KEY);
    reqBuilder.withBody(Bodies.zeroes(bodySize));
    reqBuilder.withMessageTime(1430419247000l);
    final Request request = reqBuilder.build();


    final Map<String, String> actualHeaders = auth.getAuthorizationHeaders(request);

    final Map<String, String> expectedHeaders = Maps.newHashMap();
    expectedHeaders.put("x-amz-date", "20150430T184047Z");
    expectedHeaders
        .put(
            "Authorization",
            "AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20150430/dsnet/s3/aws4_request, SignedHeaders=date;host;x-amz-content-sha256;x-amz-date, Signature=763172bdb601c03cf538a1b9bee004c90736cccfd913271cae7e6e9508a73192");
    expectedHeaders.put("Host", "127.0.0.1");
    expectedHeaders.put("Date", "Thu, 30 Apr 2015 13:40:47 CDT");
    expectedHeaders.put("x-amz-content-sha256",
        "0d5535e13cc9708d0ff0289af2fae27e564b6bcbcd9242f5140d96957744a517");

    Assert.assertEquals(expectedHeaders, actualHeaders);
  }

}
