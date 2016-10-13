/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.s3;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Operation;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.Credential;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.util.Context;
import com.cleversafe.og.util.MoreFunctions;
import com.cleversafe.og.util.Pair;
import com.cleversafe.og.supplier.Suppliers;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class MultipartRequestSupplierTest {

  final private String vaultName = "vault";
  final private String hostName = "127.0.0.1";
  final private String objectName = "test.txt";
  final private long partSize = 5242880;
  final private String uriRoot = "s3";
  final private boolean trailingSlash = true;


  @Test
  public void createMultipartRequestSupplierVirtualHostStyleTest() throws URISyntaxException {

    final URI uri =
        new URI("http://" + this.vaultName + "." + this.hostName + ":8080/" + this.objectName + "?uploads");
    final long objectSize = 10;
    final MultipartRequestSupplier request =
        createRequestSupplier(true, this.vaultName, this.hostName, this.objectName, objectSize, null, false);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.POST, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createMultipartRequestSupplierWithObjectURIRootVirtualHostStyleTest()
      throws URISyntaxException {

    final String objectName = "s3/test.txt";
    final long objectSize = 10;
    final URI uri =
        new URI("http://" + this.vaultName + "." + this.hostName + ":8080/" + objectName + "?uploads");
    final MultipartRequestSupplier request =
        createRequestSupplier(true, this.vaultName, this.hostName, objectName, objectSize, null, false);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.POST, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createMultipartRequestSupplierWithoutObjectVirtualHostStyleTest() throws URISyntaxException {

    final String objectName = null;
    final long objectSize = 10;
    final URI uri = new URI("http://" + this.vaultName + "." + this.hostName + ":8080/");
    final MultipartRequestSupplier request = createRequestSupplier(true, this.vaultName, this.hostName,
        objectName, objectSize, null, this.trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.POST, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createMultipartRequestSupplierWithoutObjectAndTrailingSlashVirtualHostStyleTest()
      throws URISyntaxException {

    final String objectName = null;
    final long objectSize = 10;
    final URI uri = new URI("http://" + this.vaultName + "." + this.hostName + ":8080");
    final MultipartRequestSupplier request =
        createRequestSupplier(true, this.vaultName, this.hostName, objectName, objectSize, null, false);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.POST, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createMultipartRequestSupplierWithURIRootVirtualHostStyleTest() throws URISyntaxException {

    final String objectName = null;
    final long objectSize = 10;
    final URI uri = new URI("http://" + this.vaultName + "." + this.hostName + ":8080/");
    final MultipartRequestSupplier request = createRequestSupplier(true, this.vaultName, this.hostName,
        objectName, objectSize, this.uriRoot, this.trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.POST, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createMultipartRequestSupplierPathStyleTest() throws URISyntaxException {

    final URI uri =
        new URI("http://" + this.hostName + ":8080/" + this.vaultName + "/" + this.objectName + "?uploads");
    final long objectSize = 10;
    final MultipartRequestSupplier request =
        createRequestSupplier(false, this.vaultName, this.hostName, this.objectName, objectSize, null, false);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.POST, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createMultipartRequestSupplierWithUriRootPathStyleTest() throws URISyntaxException {

    final URI uri = new URI("http://" + this.hostName + ":8080/" + this.uriRoot + "/"
        + this.vaultName + "/" + this.objectName + "?uploads");
    final long objectSize = 10;
    final MultipartRequestSupplier request = createRequestSupplier(false, this.vaultName, this.hostName,
        this.objectName, objectSize, this.uriRoot, false);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.POST, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createMultipartRequestSupplierWithoutObjectPathStyleTest() throws URISyntaxException {

    final String objectName = null;
    final long objectSize = 10;
    final URI uri =
        new URI("http://" + this.hostName + ":8080/" + this.uriRoot + "/" + this.vaultName + "/");
    final MultipartRequestSupplier request = createRequestSupplier(false, this.vaultName, this.hostName,
        objectName, objectSize, this.uriRoot, this.trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.POST, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  @Test
  public void createMultipartRequestSupplierWithoutObjectAndUriRootPathStyleTest()
      throws URISyntaxException {

    final String objectName = null;
    final long objectSize = 10;
    final String uriRoot = null;
    final URI uri = new URI("http://" + this.hostName + ":8080/" + this.vaultName + "/");
    final MultipartRequestSupplier request = createRequestSupplier(false, this.vaultName, this.hostName,
        objectName, objectSize, uriRoot, this.trailingSlash);

    final Request req = request.get();

    Assert.assertNotNull(req);
    Assert.assertEquals(Method.POST, req.getMethod());
    Assert.assertEquals(uri, req.getUri());

  }

  public static <O, T> Function<O, T> forSupplier(final Supplier<T> supplier) {
    return new SupplierFunction<O, T>(supplier);
  }

  @Test
  public void testMultipartSequenceWithEvenParts()
      throws URISyntaxException{

    final URI uri =
        new URI("http://" + this.vaultName + "." + this.hostName + ":8080/" + this.objectName + "?uploads");
    final long objectSize = 5242880 * 2;
    final MultipartRequestSupplier requestSupplier =
        createRequestSupplier(true, this.vaultName, this.hostName, this.objectName, objectSize, null, false);

    Request req0 = requestSupplier.get();

    Assert.assertNotNull(req0);
    Assert.assertEquals(Method.POST, req0.getMethod());
    Assert.assertEquals(uri, req0.getUri());

    Response respMock0 = mock(Response.class);
    when(respMock0.getStatusCode()).thenReturn(200);
    when(respMock0.getContext()).thenReturn(ImmutableMap.of(Context.X_OG_MULTIPART_UPLOAD_ID, "abcd"));
    when(respMock0.headers()).thenReturn(ImmutableMap.of("ETag", "tag0"));

    Request reqMock0 = mock(Request.class);
    ImmutableMap<String, String> contextMap = new ImmutableMap<String, String>();
    when(reqMock0.getContext()).thenReturn(ImmutableMap.of(Context.X_OG_OBJECT_NAME, this.objectName,
                                                            Context.X_OG_MULTIPART_UPLOAD_ID, "abcd",
        Context.X_OG_MULTIPART_CONTAINER, this.vaultName,
        Context.X_OG_OBJECT_SIZE, String.valueOf(objectSize),
        Context.X_OG_MULTIPART_PART_SIZE, String.valueOf(this.partSize)));

    requestSupplier.update(Pair.of(reqMock0, respMock0));
    Request req1 = requestSupplier.get();
    Map<String, String> reqContext1 = req1.getContext();

    Assert.assertNotNull(req1);
    Assert.assertEquals("PART", reqContext1.get(Context.X_OG_MULTIPART_REQUEST));
    Assert.assertEquals(0, reqContext1.get(Context.X_OG_MULTIPART_PART_NUMBER));


  }

  @Test
  public void testMultipartSequenceWithUnevenParts()
      throws URISyntaxException{

    final URI uri =
        new URI("http://" + this.vaultName + "." + this.hostName + ":8080/" + this.objectName + "?uploads");
    final long objectSize = 5242880 * 2;
    final MultipartRequestSupplier requestSupplier =
        createRequestSupplier(true, this.vaultName, this.hostName, this.objectName, objectSize, null, false);

    Request req0 = requestSupplier.get();

    Assert.assertNotNull(req0);
    Assert.assertEquals(Method.POST, req0.getMethod());
    Assert.assertEquals(uri, req0.getUri());

    Response respMock0 = mock(Response.class);
    when(respMock0.getStatusCode()).thenReturn(200);
    when(respMock0.getContext()).thenReturn(ImmutableMap.of(Context.X_OG_MULTIPART_UPLOAD_ID, "abcd"));
    when(respMock0.headers()).thenReturn(ImmutableMap.of("ETag", "tag0"));

    Request reqMock0 = mock(Request.class);
    when(reqMock0.getContext()).thenReturn(ImmutableMap.of(Context.X_OG_OBJECT_NAME, this.objectName));

    requestSupplier.update(Pair.of(reqMock0, respMock0));
    Request req1 = requestSupplier.get();

    Assert.assertNotNull(req1);



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

  private MultipartRequestSupplier createRequestSupplier(final boolean virtualHost, final String vaultName,
      final String hostName, final String objectName, final long objectSize, final String uriRoot,
      final boolean trailingSlash) {
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
    final Body bod = Bodies.random(objectSize);
    final Credential creds = new Credential("admin", "password", null);

    final Supplier<Body> bodySupplier = Suppliers.of(bod);
    final Supplier<Credential> credentialSupplier = Suppliers.of(creds);
    final Function<Map<String, String>, Body> body = MoreFunctions.forSupplier(bodySupplier);
    final Function<Map<String, String>, Credential> credentials = MoreFunctions.forSupplier(credentialSupplier);
    final Map<String, Function<Map<String, String>, String>> headers = Maps.newHashMap();

    final Long partSize = this.partSize;
    final Supplier<Long> partSizeSupplier = Suppliers.of(partSize);
    final Function<Map<String, String>, Long> partSizes = MoreFunctions.forSupplier(partSizeSupplier);

    final List<Function<Map<String, String>, String>> context = Collections.emptyList();

    return new MultipartRequestSupplier(id, scheme, host, port, uriRoot, container,
        object, partSizes, queryParameters, trailingSlash, headers, context, credentials, body,
        virtualHost);
  }
}
