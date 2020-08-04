/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.supplier;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.ibm.og.http.Credential;
import com.ibm.og.util.Context;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.og.api.Body;
import com.ibm.og.api.Method;
import com.ibm.og.api.Operation;
import com.ibm.og.api.Request;
import com.ibm.og.http.Bodies;
import com.ibm.og.http.Scheme;
import com.ibm.og.util.MoreFunctions;
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
        createRequestSupplier(true, this.vaultName, this.hostName, null, this.objectName, null, false, null);

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
        createRequestSupplier(true, this.vaultName, this.hostName, null, objectName, null, false, null);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithoutObjectVirtualHostStyleTest() throws URISyntaxException {

    final String objectName = null;
    final URI uri = new URI("http://" + this.vaultName + "." + this.hostName + ":8080/");
    final RequestSupplier request = createRequestSupplier(true, this.vaultName, this.hostName, null,
        objectName, null, this.trailingSlash, null);

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
        createRequestSupplier(true, this.vaultName, this.hostName, null, objectName, null, false, null);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithURIRootVirtualHostStyleTest() throws URISyntaxException {

    final String objectName = null;
    final URI uri = new URI("http://" + this.vaultName + "." + this.hostName + ":8080/");
    final RequestSupplier request = createRequestSupplier(true, this.vaultName, this.hostName, null,
        objectName, this.uriRoot, this.trailingSlash, null);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithStaticWebsiteRead() throws URISyntaxException {

    final String objectName = "cafebeef0000";
    final URI uri = new URI("http://" +  this.hostName + ":8080/"+ objectName);
    Function<Map<String, String>, String> staticHostSuffix = new Function<Map<String, String>, String>() {
      @Nullable
      @Override
      public String apply(@Nullable Map<String, String> input) {
        final String suffix = "ibm.com";
        input.put(Context.X_OG_STATIC_WEBSITE_VIRTUAL_HOST_SUFFIX,
                suffix);
        return suffix;
      }
    };
    final RequestSupplier request = createRequestSupplier(false, this.vaultName, this.hostName, null,
            objectName, null, false, staticHostSuffix);

    final Request req = request.get();
    Assert.assertNotNull(req);
    Assert.assertEquals(Method.GET, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierPathStyleTest() throws URISyntaxException {

    final URI uri =
        new URI("http://" + this.hostName + ":8080/" + this.vaultName + "/" + this.objectName);
    final RequestSupplier request =
        createRequestSupplier(false, this.vaultName, this.hostName, null, this.objectName, null, false, null);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithUriRootPathStyleTest() throws URISyntaxException {

    final URI uri = new URI("http://" + this.hostName + ":8080/" + this.uriRoot + "/"
        + this.vaultName + "/" + this.objectName);
    final RequestSupplier request = createRequestSupplier(false, this.vaultName, this.hostName, null,
        this.objectName, this.uriRoot, false, null);

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
    final RequestSupplier request = createRequestSupplier(false, this.vaultName, this.hostName, null,
        objectName, this.uriRoot, this.trailingSlash, null);

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

    final RequestSupplier request = createRequestSupplier(false, this.vaultName, this.hostName, null,
        objectName, uriRoot, this.trailingSlash, null);

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
      final String hostName, final String apiVersion, final String objectName, final String uriRoot,
      final boolean trailingSlash, final Function<Map<String, String>, String> staticWebsiteVirtualHostSuffix) {
    final Method method;
    final Operation operation;
    if (staticWebsiteVirtualHostSuffix != null) {
      method = Method.GET;
      operation = Operation.READ;
    } else {
      method = Method.PUT;
      operation = Operation.WRITE;
    }
    final Scheme scheme = Scheme.HTTP;
    final Supplier<String> hostSupplier = Suppliers.of(hostName);
    final Supplier<String> apiVersionSupplier = Suppliers.of("v1");
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
            input.put(Context.X_OG_CONTAINER_NAME, vaultName);
            return vaultName;
          }
        };
    final Integer port = 8080;
    final Body bod = Bodies.random(10);
    final Credential creds = new Credential("admin", "password", null, null, null);

    final Supplier<Body> bodySupplier = Suppliers.of(bod);
    final Supplier<Credential> credentialSupplier = Suppliers.of(creds);
    final Function<Map<String, String>, Body> body = MoreFunctions.forSupplier(bodySupplier);
    final Function<Map<String, String>, Credential> credentials = MoreFunctions.forSupplier(credentialSupplier);
    final Map<String, Function<Map<String, String>, String>> headers = Maps.newHashMap();

    final List<Function<Map<String, String>, String>> context = Collections.emptyList();

    return new RequestSupplier(operation, id, method, scheme, host, port, uriRoot, container, apiVersion,
            object, queryParameters, trailingSlash, headers, context, null, credentials, body,
        virtualHost, null, null, false, null, staticWebsiteVirtualHostSuffix);
  }
}
