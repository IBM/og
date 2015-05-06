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
import com.cleversafe.og.api.Data;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Scheme;
import com.google.common.base.Function;
import com.google.common.base.Supplier;

public class RequestSupplierTest {

  @Test
  public void createRequestSupplierVirtualHostStyleTest() throws URISyntaxException {

    final String vaultName = "vault";
    final String hostName = "127.0.0.1";
    final String objectName = "test.txt";
    final URI uri = new URI("http://" + vaultName + "." + hostName + ":8080/" + objectName);
    final String uriRoot = null;
    final boolean trailingSlash = false;
    final RequestSupplier request =
        createRequestSupplier(true, vaultName, hostName, objectName, uriRoot, trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithObjectURIRootVirtualHostStyleTest()
      throws URISyntaxException {

    final String vaultName = "vault";
    final String hostName = "127.0.0.1";
    final String objectName = "s3/test.txt";
    final URI uri = new URI("http://" + vaultName + "." + hostName + ":8080/" + objectName);
    final String uriRoot = null;
    final boolean trailingSlash = false;
    final RequestSupplier request =
        createRequestSupplier(true, vaultName, hostName, objectName, uriRoot, trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithoutObjectVirtualHostStyleTest() throws URISyntaxException {

    final String vaultName = "vault";
    final String hostName = "127.0.0.1";
    final String objectName = null;
    final URI uri = new URI("http://" + vaultName + "." + hostName + ":8080/");
    final String uriRoot = null;
    final boolean trailingSlash = true;
    final RequestSupplier request =
        createRequestSupplier(true, vaultName, hostName, objectName, uriRoot, trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithoutObjectAndTrailingSlashVirtualHostStyleTest()
      throws URISyntaxException {

    final String vaultName = "vault";
    final String hostName = "127.0.0.1";
    final String objectName = null;
    final URI uri = new URI("http://" + vaultName + "." + hostName + ":8080");
    final String uriRoot = null;
    final boolean trailingSlash = false;
    final RequestSupplier request =
        createRequestSupplier(true, vaultName, hostName, objectName, uriRoot, trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithURIRootVirtualHostStyleTest() throws URISyntaxException {

    final String vaultName = "vault";
    final String hostName = "127.0.0.1";
    final String objectName = null;
    final String uriRoot = "s3";
    final URI uri = new URI("http://" + vaultName + "." + hostName + ":8080/");
    final boolean trailingSlash = true;
    final RequestSupplier request =
        createRequestSupplier(true, vaultName, hostName, objectName, uriRoot, trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierPathStyleTest() throws URISyntaxException {

    final String vaultName = "vault";
    final String hostName = "127.0.0.1";
    final String objectName = "test.txt";
    final URI uri = new URI("http://" + hostName + ":8080/" + vaultName + "/" + objectName);
    final String uriRoot = null;
    final boolean trailingSlash = false;
    final RequestSupplier request =
        createRequestSupplier(false, vaultName, hostName, objectName, uriRoot, trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithUriRootPathStyleTest() throws URISyntaxException {

    final String vaultName = "vault";
    final String hostName = "127.0.0.1";
    final String objectName = "test.txt";
    final String uriRoot = "s3";
    final URI uri =
        new URI("http://" + hostName + ":8080/" + uriRoot + "/" + vaultName + "/" + objectName);
    final boolean trailingSlash = false;
    final RequestSupplier request =
        createRequestSupplier(false, vaultName, hostName, objectName, uriRoot, trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithoutObjectPathStyleTest() throws URISyntaxException {

    final String vaultName = "vault";
    final String hostName = "127.0.0.1";
    final String objectName = null;
    final String uriRoot = "s3";
    final URI uri = new URI("http://" + hostName + ":8080/" + uriRoot + "/" + vaultName + "/");
    final boolean trailingSlash = true;
    final RequestSupplier request =
        createRequestSupplier(false, vaultName, hostName, objectName, uriRoot, trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithoutObjectAndUriRootPathStyleTest() throws URISyntaxException {

    final String vaultName = "vault";
    final String hostName = "127.0.0.1";
    final String objectName = null;
    final String uriRoot = null;
    final URI uri = new URI("http://" + hostName + ":8080/" + vaultName + "/");
    final boolean trailingSlash = true;
    final RequestSupplier request =
        createRequestSupplier(false, vaultName, hostName, objectName, uriRoot, trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithoutObjectAndVaultPathStyleTest() throws URISyntaxException {

    final String vaultName = null;
    final String hostName = "127.0.0.1";
    final String objectName = null;
    final String uriRoot = "s3";
    final URI uri = new URI("http://" + hostName + ":8080/" + uriRoot + "/");
    final boolean trailingSlash = false;
    final RequestSupplier request =
        createRequestSupplier(false, vaultName, hostName, objectName, uriRoot, trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createRequestSupplierWithoutObjectAndVaultAndUriRootPathStyleTest()
      throws URISyntaxException {

    final String vaultName = null;
    final String hostName = "127.0.0.1";
    final String objectName = null;
    final String uriRoot = null;
    final URI uri = new URI("http://" + hostName + ":8080/");
    final boolean trailingSlash = false;
    final RequestSupplier request =
        createRequestSupplier(false, vaultName, hostName, objectName, uriRoot, trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.PUT, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  // ------------------------HELPER METHODS--------------------------//

  private RequestSupplier createRequestSupplier(final boolean virtualHostFlag,
      final String vaultName, final String hostName, final String objectName,
      final String uriRootStr, final boolean trailingSlashVal) {
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
    final String uriRoot = uriRootStr;
    final Integer port = 8080;
    final String username = "admin";
    final String password = "password";
    final boolean trailingSlash = trailingSlashVal;
    final boolean virtualHost = virtualHostFlag;
    final Body bod = new Body() {

      @Override
      public long getSize() {
        return 10;
      }

      @Override
      public Data getData() {
        return Data.RANDOM;
      }
    };
    final Supplier<Body> body = Suppliers.of(bod);
    final Map<String, Supplier<String>> headers = new HashMap<String, Supplier<String>>();

    return createRequestSupplierHelper(id, method, scheme, host, port, uriRoot, container, object,
        queryParameters, trailingSlash, headers, body, username, password, virtualHost);

  }

  private RequestSupplier createRequestSupplierHelper(final Supplier<String> id,
      final Method method, final Scheme scheme, final Supplier<String> host, final Integer port,
      final String uriRoot, final Function<Map<String, String>, String> container,
      final Function<Map<String, String>, String> object,
      final Map<String, String> queryParameters, final boolean trailingSlash,
      final Map<String, Supplier<String>> headers, final Supplier<Body> body,
      final String username, final String password, final boolean virtualHost) {

    return new RequestSupplier(id, method, scheme, host, port, uriRoot, container, object,
        queryParameters, trailingSlash, headers, username, password, body, virtualHost);
  }
}
