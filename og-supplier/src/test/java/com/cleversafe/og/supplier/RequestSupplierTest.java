/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.supplier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.Scheme;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps;

public class RequestSupplierTest {

  final private String vaultName = "vault";
  final private String hostName = "127.0.0.1";
  final private String objectName = "test.txt";
  final private String uriRoot = "s3";
  final private boolean trailingSlash = true;


  @Test
  public void createRequestSupplierVirtualHostStyleTest() throws URISyntaxException {

    final URI uri =
        new URI("http://" + this.vaultName + "." + this.hostName + ":8080/" + this.objectName);

    final RequestSupplier request =
        createRequestSupplier(true, this.vaultName, this.hostName, this.objectName, null, false);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithObjectURIRootVirtualHostStyleTest()
      throws URISyntaxException {

    final String objectName = "s3/test.txt";
    final URI uri =
        new URI("http://" + this.vaultName + "." + this.hostName + ":8080/" + objectName);
    final RequestSupplier request =
        createRequestSupplier(true, this.vaultName, this.hostName, objectName, null, false);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithoutObjectVirtualHostStyleTest() throws URISyntaxException {

    final String objectName = null;
    final URI uri = new URI("http://" + this.vaultName + "." + this.hostName + ":8080/");
    final RequestSupplier request = createRequestSupplier(true, this.vaultName, this.hostName,
        objectName, null, this.trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithoutObjectAndTrailingSlashVirtualHostStyleTest()
      throws URISyntaxException {

    final String objectName = null;
    final URI uri = new URI("http://" + this.vaultName + "." + this.hostName + ":8080");
    final RequestSupplier request =
        createRequestSupplier(true, this.vaultName, this.hostName, objectName, null, false);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithURIRootVirtualHostStyleTest() throws URISyntaxException {

    final String objectName = null;
    final URI uri = new URI("http://" + this.vaultName + "." + this.hostName + ":8080/");
    final RequestSupplier request = createRequestSupplier(true, this.vaultName, this.hostName,
        objectName, this.uriRoot, this.trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierPathStyleTest() throws URISyntaxException {

    final URI uri =
        new URI("http://" + this.hostName + ":8080/" + this.vaultName + "/" + this.objectName);
    final RequestSupplier request =
        createRequestSupplier(false, this.vaultName, this.hostName, this.objectName, null, false);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithUriRootPathStyleTest() throws URISyntaxException {

    final URI uri = new URI("http://" + this.hostName + ":8080/" + this.uriRoot + "/"
        + this.vaultName + "/" + this.objectName);
    final RequestSupplier request = createRequestSupplier(false, this.vaultName, this.hostName,
        this.objectName, this.uriRoot, false);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithoutObjectPathStyleTest() throws URISyntaxException {

    final String objectName = null;
    final URI uri =
        new URI("http://" + this.hostName + ":8080/" + this.uriRoot + "/" + this.vaultName + "/");
    final RequestSupplier request = createRequestSupplier(false, this.vaultName, this.hostName,
        objectName, this.uriRoot, this.trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithoutObjectAndUriRootPathStyleTest()
      throws URISyntaxException {

    final String objectName = null;
    final String uriRoot = null;
    final URI uri = new URI("http://" + this.hostName + ":8080/" + this.vaultName + "/");
    final RequestSupplier request = createRequestSupplier(false, this.vaultName, this.hostName,
        objectName, uriRoot, this.trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithoutObjectAndVaultPathStyleTest() throws URISyntaxException {

    final String vaultName = null;
    final String objectName = null;
    final URI uri = new URI("http://" + this.hostName + ":8080/" + this.uriRoot + "/");
    final RequestSupplier request =
        createRequestSupplier(false, vaultName, this.hostName, objectName, this.uriRoot, false);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithoutObjectAndVaultAndUriRootPathStyleTest()
      throws URISyntaxException {

    final String vaultName = null;
    final String objectName = null;
    final URI uri = new URI("http://" + this.hostName + ":8080/");
    final RequestSupplier request =
        createRequestSupplier(false, vaultName, this.hostName, objectName, null, false);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  // ------------------------HELPER METHODS--------------------------//

  private RequestSupplier createRequestSupplier(final boolean virtualHost, final String vaultName,
      final String hostName, final String objectName, final String uriRoot,
      final boolean trailingSlash) {
    final Method method = Method.PUT;
    final Scheme scheme = Scheme.HTTP;
    final Supplier<String> host = Suppliers.of(hostName);
    final Function<Map<String, String>, String> object =
        new Function<Map<String, String>, String>() {

          @Override
          public String apply(final Map<String, String> input) {
            return objectName;
          }
        };
    final Map<String, String> queryParameters = new HashMap<String, String>();
    final Supplier<String> id = Suppliers.of("request.id");
    final Function<Map<String, String>, String> container =
        new Function<Map<String, String>, String>() {

          @Override
          public String apply(final Map<String, String> input) {
            // TODO Auto-generated method stub
            return vaultName;
          }
        };
    final Integer port = 8080;
    final String username = "admin";
    final String password = "password";
    final Body bod = Bodies.random(10);

    final Supplier<Body> body = Suppliers.of(bod);
    final Map<String, Supplier<String>> headers = new HashMap<String, Supplier<String>>();

    return new RequestSupplier(id, method, scheme, host, port, uriRoot, container, object,
        queryParameters, trailingSlash, headers, Maps.<String, String>newHashMap(), username,
        password, null, body, virtualHost);
  }
}
