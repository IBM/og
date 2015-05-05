/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.supplier;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Data;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Scheme;
import com.google.common.base.Function;
import com.google.common.base.Supplier;



@SuppressWarnings("deprecation")
public class RequestSupplierTest {

  @Test
  public void createRequestSupplierTest() {

    final Method method = Method.PUT;
    final Scheme scheme = Scheme.HTTP;
    final Supplier<String> host = Suppliers.of("127.0.0.1");
    final Function<Map<String, String>, String> object =
        new Function<Map<String, String>, String>() {

          @Override
          public String apply(final Map<String, String> input) {
            return "test.txt";
          }
        };
    final Map<String, String> queryParameters = new HashMap<String, String>();
    final Supplier<String> id = Suppliers.of("request.id");
    final Function<Map<String, String>, String> container =
        new Function<Map<String, String>, String>() {

          @Override
          public String apply(final Map<String, String> input) {
            // TODO Auto-generated method stub
            return "vault";
          }
        };
    final String uriRoot = "";
    final Integer port = 8080;
    final String username = "admin";
    final String password = "password";
    final boolean trailingSlash = false;
    final boolean virtualHost = true;
    final Body body = new Body() {

      @Override
      public long getSize() {
        return 10;
      }

      @Override
      public Data getData() {
        return Data.RANDOM;
      }
    };
    final Supplier<Body> bod = Suppliers.of(body);
    final Map<String, Supplier<String>> headers = new HashMap<String, Supplier<String>>();

    final RequestSupplier request =
        new RequestSupplier(id, method, scheme, host, port, uriRoot, container, object,
            queryParameters, trailingSlash, headers, username, password, bod, virtualHost);

    final Request req = request.get();

    Assert.assertNotNull(req);

  }
}
