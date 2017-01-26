/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.s3;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ibm.og.api.Body;
import com.ibm.og.api.Method;
import com.ibm.og.api.Request;
import com.ibm.og.api.Response;
import com.ibm.og.http.Bodies;
import com.ibm.og.http.Credential;
import com.ibm.og.http.Scheme;
import com.ibm.og.util.Context;
import com.ibm.og.util.MoreFunctions;
import com.ibm.og.util.Pair;
import com.ibm.og.supplier.Suppliers;
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
import java.util.HashMap;
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

  public static <O, T> Function<O, T> forSupplier(final Supplier<T> supplier) {
    return new SupplierFunction<O, T>(supplier);
  }

  @Test
  public void testMultipartSequenceWithEvenParts()
      throws URISyntaxException{
    long objectSize = 5242880*2;
    long expectedPart1Size = 5242880;
    long expectedPart2Size = 5242880;
    genericTwoPartMultipartSequence(objectSize, expectedPart1Size, expectedPart2Size);
  }

  @Test
  public void testMultipartSequenceWithUnevenParts()
      throws URISyntaxException{
    long objectSize = (long)(5242880*1.5);
    long expectedPart1Size = 5242880;
    long expectedPart2Size = 5242880/2;
    genericTwoPartMultipartSequence(objectSize, expectedPart1Size, expectedPart2Size);
  }

  @Test
  public void testMultipleMultipartSequencesWithEvenParts() throws URISyntaxException {
    long objectSize = (long)5242880*2;
    long expectedPart1Size = 5242880;
    long expectedPart2Size = 5242880;
    genericTwoPartMultipartSequenceWithTwoSequences(objectSize, expectedPart1Size, expectedPart2Size);
  }

  @Test
  public void testMultipleMultipartSequencesWithUnevenParts() throws URISyntaxException {
    long objectSize = (long)(5242880*1.5);
    long expectedPart1Size = 5242880;
    long expectedPart2Size = 5242880/2;
    genericTwoPartMultipartSequenceWithTwoSequences(objectSize, expectedPart1Size, expectedPart2Size);
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
    final Credential creds = new Credential("admin", "password", null, null);

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
        object, partSizes, 1, queryParameters, trailingSlash, headers, context, credentials, body,
        virtualHost);
  }

  private void genericTwoPartMultipartSequence(long objectSize, long expectedPart1Size, long expectedPart2Size) throws URISyntaxException {
    final URI uri =
        new URI("http://" + this.vaultName + "." + this.hostName + ":8080/" + this.objectName + "?uploads");

    final String uploadId = "abcd";
    final MultipartRequestSupplier requestSupplier =
        createRequestSupplier(true, this.vaultName, this.hostName, this.objectName, objectSize, null, false);

    Request req0 = requestSupplier.get();

    Assert.assertNotNull(req0);
    Assert.assertEquals(Method.POST, req0.getMethod());
    Assert.assertEquals(uri, req0.getUri());
    Assert.assertEquals("INITIATE", req0.getContext().get(Context.X_OG_MULTIPART_REQUEST));

    Response respMock0 = mock(Response.class);
    when(respMock0.getStatusCode()).thenReturn(200);
    when(respMock0.getContext()).thenReturn(ImmutableMap.of(Context.X_OG_MULTIPART_UPLOAD_ID, "abcd"));
    when(respMock0.headers()).thenReturn(ImmutableMap.of("ETag", "tag0"));

    Request reqMock0 = mock(Request.class);
    Map<String, String> contextMap = new HashMap<String, String>();
    contextMap.put(Context.X_OG_MULTIPART_BODY_DATA_TYPE, "ZEROES");
    contextMap.put(Context.X_OG_MULTIPART_CONTAINER, this.vaultName);
    contextMap.put(Context.X_OG_CONTAINER_SUFFIX, String.valueOf(-1));
    contextMap.put(Context.X_OG_OBJECT_NAME, this.objectName);
    contextMap.put(Context.X_OG_OBJECT_SIZE, String.valueOf(objectSize));
    contextMap.put(Context.X_OG_MULTIPART_PART_SIZE, String.valueOf(partSize));
    contextMap.put(Context.X_OG_MULTIPART_UPLOAD_ID, uploadId);
    contextMap.put(Context.X_OG_MULTIPART_PART_NUMBER, String.valueOf(0));
    contextMap.put(Context.X_OG_MULTIPART_REQUEST, "INITIATE");
    when(reqMock0.getContext()).thenReturn(contextMap);

    requestSupplier.update(Pair.of(reqMock0, respMock0));
    Request req1 = requestSupplier.get();
    Map<String, String> reqContext1 = req1.getContext();

    Assert.assertNotNull(req1);
    Assert.assertEquals("PART", reqContext1.get(Context.X_OG_MULTIPART_REQUEST));
    Assert.assertEquals("1", reqContext1.get(Context.X_OG_MULTIPART_PART_NUMBER));
    Assert.assertEquals(expectedPart1Size, req1.getBody().getSize());

    Response respMock1 = mock(Response.class);
    when(respMock1.headers()).thenReturn(ImmutableMap.of("ETag", "tag1"));
    requestSupplier.update(Pair.of(req1, respMock1));

    Request req2 = requestSupplier.get();
    Assert.assertNotNull(req2);
    Assert.assertEquals("PART", req2.getContext().get(Context.X_OG_MULTIPART_REQUEST));
    Assert.assertEquals("2", req2.getContext().get(Context.X_OG_MULTIPART_PART_NUMBER));
    Assert.assertEquals(expectedPart2Size, req2.getBody().getSize());

    Response respMock2 = mock(Response.class);
    when(respMock2.headers()).thenReturn(ImmutableMap.of("ETag", "tag2"));
    requestSupplier.update(Pair.of(req2, respMock2));

    Request req3 = requestSupplier.get();
    Assert.assertNotNull(req3);
    Assert.assertEquals("COMPLETE", req3.getContext().get(Context.X_OG_MULTIPART_REQUEST));
    Assert.assertEquals("<CompleteMultipartUpload><Part><PartNumber>1</PartNumber><ETag>tag1</ETag></Part><Part><PartNumber>2</PartNumber><ETag>tag2</ETag></Part></CompleteMultipartUpload>", req3.getBody().getContent());
  }

  private void genericTwoPartMultipartSequenceWithTwoSequences(long objectSize, long expectedPart1Size, long expectedPart2Size) throws URISyntaxException {
    final URI uri =
        new URI("http://" + this.vaultName + "." + this.hostName + ":8080/" + this.objectName + "?uploads");

    final String uploadIdA = "abc";
    final String uploadIdB = "xyz";
    final MultipartRequestSupplier requestSupplier =
        createRequestSupplier(true, this.vaultName, this.hostName, this.objectName, objectSize, null, false);

    // INITIATE upload A
    Request reqA0 = requestSupplier.get();
    Assert.assertNotNull(reqA0);
    Assert.assertEquals(Method.POST, reqA0.getMethod());
    Assert.assertEquals(uri, reqA0.getUri());
    Assert.assertEquals("INITIATE", reqA0.getContext().get(Context.X_OG_MULTIPART_REQUEST));

    // upload A mock responses
    Response respMock0A = mock(Response.class);
    when(respMock0A.getStatusCode()).thenReturn(200);
    when(respMock0A.getContext()).thenReturn(ImmutableMap.of(Context.X_OG_MULTIPART_UPLOAD_ID, uploadIdA));
    when(respMock0A.headers()).thenReturn(ImmutableMap.of("ETag", "tag0"));

    Request reqMockA0 = mock(Request.class);
    Map<String, String> contextMapA = new HashMap<String, String>();
    contextMapA.put(Context.X_OG_MULTIPART_BODY_DATA_TYPE, "ZEROES");
    contextMapA.put(Context.X_OG_MULTIPART_CONTAINER, this.vaultName);
    contextMapA.put(Context.X_OG_CONTAINER_SUFFIX, String.valueOf(-1));
    contextMapA.put(Context.X_OG_OBJECT_NAME, this.objectName);
    contextMapA.put(Context.X_OG_OBJECT_SIZE, String.valueOf(objectSize));
    contextMapA.put(Context.X_OG_MULTIPART_PART_SIZE, String.valueOf(partSize));
    contextMapA.put(Context.X_OG_MULTIPART_UPLOAD_ID, uploadIdA);
    contextMapA.put(Context.X_OG_MULTIPART_PART_NUMBER, String.valueOf(0));
    contextMapA.put(Context.X_OG_MULTIPART_REQUEST, "INITIATE");
    when(reqMockA0.getContext()).thenReturn(contextMapA);

    // upload B mock responses
    Response respMock0B = mock(Response.class);
    when(respMock0B.getStatusCode()).thenReturn(200);
    when(respMock0B.getContext()).thenReturn(ImmutableMap.of(Context.X_OG_MULTIPART_UPLOAD_ID, uploadIdB));
    when(respMock0B.headers()).thenReturn(ImmutableMap.of("ETag", "tag0"));

    Request reqMockB0 = mock(Request.class);
    Map<String, String> contextMapB = new HashMap<String, String>();
    contextMapB.put(Context.X_OG_MULTIPART_BODY_DATA_TYPE, "ZEROES");
    contextMapB.put(Context.X_OG_MULTIPART_CONTAINER, this.vaultName);
    contextMapB.put(Context.X_OG_CONTAINER_SUFFIX, String.valueOf(-1));
    contextMapB.put(Context.X_OG_OBJECT_NAME, this.objectName);
    contextMapB.put(Context.X_OG_OBJECT_SIZE, String.valueOf(objectSize));
    contextMapB.put(Context.X_OG_MULTIPART_PART_SIZE, String.valueOf(partSize));
    contextMapB.put(Context.X_OG_MULTIPART_UPLOAD_ID, uploadIdB);
    contextMapB.put(Context.X_OG_MULTIPART_PART_NUMBER, String.valueOf(0));
    contextMapB.put(Context.X_OG_MULTIPART_REQUEST, "INITIATE");
    when(reqMockB0.getContext()).thenReturn(contextMapB);

    // INITIATE upload B
    Request reqB0 = requestSupplier.get();
    Map<String, String> reqContextB0 = reqB0.getContext();
    Assert.assertNotNull(reqB0);
    Assert.assertEquals("INITIATE", reqContextB0.get(Context.X_OG_MULTIPART_REQUEST));

    // Response for INITIATE upload A
    requestSupplier.update(Pair.of(reqMockA0, respMock0A));

    // PART 1 of upload A
    Request req2A = requestSupplier.get();
    Assert.assertNotNull(req2A);
    Assert.assertEquals(uploadIdA, req2A.getContext().get(Context.X_OG_MULTIPART_UPLOAD_ID));
    Assert.assertEquals("PART", req2A.getContext().get(Context.X_OG_MULTIPART_REQUEST));
    Assert.assertEquals("1", req2A.getContext().get(Context.X_OG_MULTIPART_PART_NUMBER));
    Assert.assertEquals(expectedPart1Size, req2A.getBody().getSize());

    // Response for Part 1 of upload A
    Response respMock1 = mock(Response.class);
    when(respMock1.headers()).thenReturn(ImmutableMap.of("ETag", "tag1"));
    when(respMock1.getStatusCode()).thenReturn(200);
    requestSupplier.update(Pair.of(req2A, respMock1));

    // Part 2 of upload A
    Request req3A = requestSupplier.get();
    Assert.assertNotNull(req3A);
    Assert.assertEquals(uploadIdA, req3A.getContext().get(Context.X_OG_MULTIPART_UPLOAD_ID));
    Assert.assertEquals("PART", req3A.getContext().get(Context.X_OG_MULTIPART_REQUEST));
    Assert.assertEquals("2", req3A.getContext().get(Context.X_OG_MULTIPART_PART_NUMBER));
    Assert.assertEquals(expectedPart2Size, req3A.getBody().getSize());

    // INITIATE upload C
    Request reqC0 = requestSupplier.get();
    Assert.assertNotNull(reqC0);
    Assert.assertEquals("INITIATE", reqC0.getContext().get(Context.X_OG_MULTIPART_REQUEST));

    // Response for INITIATE upload B
    requestSupplier.update(Pair.of(reqMockB0, respMock0B));

    // Part 1 of upload B
    Request req2B = requestSupplier.get();
    Assert.assertNotNull(req2B);
    Assert.assertEquals(uploadIdB, req2B.getContext().get(Context.X_OG_MULTIPART_UPLOAD_ID));
    Assert.assertEquals("PART", req2B.getContext().get(Context.X_OG_MULTIPART_REQUEST));
    Assert.assertEquals("1", req2B.getContext().get(Context.X_OG_MULTIPART_PART_NUMBER));
    Assert.assertEquals(expectedPart1Size, req2B.getBody().getSize());

    // Response for Part 2 of upload A
    Response respMock2 = mock(Response.class);
    when(respMock2.getStatusCode()).thenReturn(200);
    when(respMock2.headers()).thenReturn(ImmutableMap.of("ETag", "tag2"));
    requestSupplier.update(Pair.of(req3A, respMock2));

    // Response for Part 1 of upload B
    requestSupplier.update(Pair.of(req2B, respMock1));

    // COMPLETE upload A
    Request req4A = requestSupplier.get();
    Assert.assertEquals(uploadIdA, req4A.getContext().get(Context.X_OG_MULTIPART_UPLOAD_ID));
    Assert.assertEquals("COMPLETE", req4A.getContext().get(Context.X_OG_MULTIPART_REQUEST));
    Assert.assertEquals("<CompleteMultipartUpload><Part><PartNumber>1</PartNumber><ETag>tag1</ETag></Part><Part><PartNumber>2</PartNumber><ETag>tag2</ETag></Part></CompleteMultipartUpload>", req4A.getBody().getContent());

    // Part 2 of upload B
    Request req3B = requestSupplier.get();
    Assert.assertNotNull(req3B);
    Assert.assertEquals(uploadIdB, req3B.getContext().get(Context.X_OG_MULTIPART_UPLOAD_ID));
    Assert.assertEquals("PART", req3B.getContext().get(Context.X_OG_MULTIPART_REQUEST));
    Assert.assertEquals("2", req3B.getContext().get(Context.X_OG_MULTIPART_PART_NUMBER));
    Assert.assertEquals(expectedPart2Size, req3B.getBody().getSize());

    // Response for upload A Complete
    requestSupplier.update(Pair.of(req4A, respMock1));

    // INITIATE upload D
    Request req0D = requestSupplier.get();
    Assert.assertNotNull(req0D);
    Assert.assertEquals("INITIATE", req0D.getContext().get(Context.X_OG_MULTIPART_REQUEST));

    // Response for Part 2 of upload B
    requestSupplier.update(Pair.of(req3B, respMock2));

    // COMPLETE upload B
    Request req4B = requestSupplier.get();
    Assert.assertEquals(uploadIdB, req4B.getContext().get(Context.X_OG_MULTIPART_UPLOAD_ID));
    Assert.assertEquals("COMPLETE", req4B.getContext().get(Context.X_OG_MULTIPART_REQUEST));
    Assert.assertEquals("<CompleteMultipartUpload><Part><PartNumber>1</PartNumber><ETag>tag1</ETag></Part><Part><PartNumber>2</PartNumber><ETag>tag2</ETag></Part></CompleteMultipartUpload>", req4B.getBody().getContent());
  }

}
