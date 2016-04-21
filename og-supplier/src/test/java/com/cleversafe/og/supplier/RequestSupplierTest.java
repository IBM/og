/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.supplier;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.cleversafe.og.http.Credential;
import org.junit.Assert;
import org.junit.Test;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Operation;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.util.MoreFunctions;
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

  public static <O, T> Function<O, T> forSupplier(final Supplier<T> supplier) {
    return new SupplierFunction<O, T>(supplier);
  }


  private static class SupplierFunction<O, T> implements Function<O, T> {

    private final Supplier<T> supplier;

    private SupplierFunction(final Supplier<T> supplier) {
      this.supplier = checkNotNull(supplier);
    }

    @Override
    public T apply(@Nullable final O input) {
      return this.supplier.get();
    }
  }

  // ------------------------HELPER METHODS--------------------------//

  private RequestSupplier createRequestSupplier(final boolean virtualHost, final String vaultName,
      final String hostName, final String objectName, final String uriRoot,
      final boolean trailingSlash) {
    final Method method = Method.PUT;
    final Operation operation = Operation.WRITE;
    final Scheme scheme = Scheme.HTTP;
    final Supplier<String> hostSupplier = Suppliers.of(hostName);
    final Function<Map<String, String>, String> host = MoreFunctions.forSupplier(hostSupplier);
    Function<Map<String, String>, String> object = null;
    if (objectName != null) {
      object = new Function<Map<String, String>, String>() {

        @Override
        public String apply(final Map<String, String> input) {
          return objectName;
        }
      };
    }
    final Map<String, Function<Map<String, String>, String>> queryParameters = Maps.newHashMap();
    final Supplier<String> idSupplier = Suppliers.of("request.id");
    final Function<Map<String, String>, String> id = MoreFunctions.forSupplier(idSupplier);
    final Function<Map<String, String>, String> container =
        new Function<Map<String, String>, String>() {

          @Override
          public String apply(final Map<String, String> input) {
            // TODO Auto-generated method stub
            return vaultName;
          }
        };
    final Integer port = 8080;
    final Body bod = Bodies.random(10);
    final Credential creds = new Credential("admin", "password", null);

    final Supplier<Body> bodySupplier = Suppliers.of(bod);
    final Supplier<Credential> credentialSupplier = Suppliers.of(creds);
    final Function<Map<String, String>, Body> body = MoreFunctions.forSupplier(bodySupplier);
    final Function<Map<String, String>, Credential> credentials = MoreFunctions.forSupplier(credentialSupplier);
    final Map<String, Function<Map<String, String>, String>> headers = Maps.newHashMap();

    final List<Function<Map<String, String>, String>> context = Collections.emptyList();

    return new RequestSupplier(operation, id, method, scheme, host, port, uriRoot, container,
        object, queryParameters, trailingSlash, headers, context, credentials, body,
        virtualHost);
  }
}
