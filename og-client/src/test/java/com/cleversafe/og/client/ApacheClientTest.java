//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: rveitch
//
// Date: Jul 7, 2014
// ---------------------

package com.cleversafe.og.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.cleversafe.og.api.Client;
import com.cleversafe.og.api.Entity;
import com.cleversafe.og.api.EntityType;
import com.cleversafe.og.api.Metadata;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.http.BasicAuth;
import com.cleversafe.og.http.HttpRequest;
import com.cleversafe.og.util.Entities;
import com.cleversafe.og.util.ResponseBodyConsumer;
import com.cleversafe.og.util.SizeUnit;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class ApacheClientTest
{
   @ClassRule
   public static final WireMockClassRule WIREMOCK_RULE = new WireMockClassRule(8080);

   @Rule
   public WireMockClassRule wireMockRule = WIREMOCK_RULE;
   private Client client;
   private URI objectUri;
   private URI delayedUri;
   private URI redirectUri;
   private List<Integer> redirectStatuses;

   @Before()
   public void before() throws URISyntaxException
   {
      this.client = new ApacheClient.Builder().build();

      // write
      stubFor(put(urlMatching("/container/.*")).willReturn(
            aResponse().withStatus(200)
            ));

      // write (post)
      stubFor(post(urlMatching("/container/.*")).willReturn(
            aResponse().withStatus(200)
            ));

      // read
      stubFor(get(urlMatching("/container/.*")).willReturn(
            aResponse().withStatus(200)
                  .withBody(new byte[1024])
            ));

      // read (head)
      stubFor(head(urlMatching("/container/.*")).willReturn(
            aResponse().withStatus(200)
            ));

      // delete
      stubFor(delete(urlMatching("/container/.*")).willReturn(
            aResponse().withStatus(204)
            ));

      // 5 second delay
      stubFor(get(urlEqualTo("/delayed")).willReturn(
            aResponse().withStatus(200).withFixedDelay(5000)
            ));

      this.objectUri = new URI("http://127.0.0.1:8080/container/object");
      this.delayedUri = new URI("http://127.0.0.1:8080/delayed");
      this.redirectUri = new URI("http://127.0.0.1:8080/intermediate");
      this.redirectStatuses = Lists.newArrayList();
      this.redirectStatuses.add(301);
      this.redirectStatuses.add(302);
      this.redirectStatuses.add(307);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeConnectTimeout()
   {
      new ApacheClient.Builder().withConnectTimeout(-1).build();
   }

   @Test
   public void testZeroConnectTimeout()
   {
      new ApacheClient.Builder().withConnectTimeout(0).build();
   }

   @Test
   public void testPositiveConnectTimeout()
   {
      new ApacheClient.Builder().withConnectTimeout(1).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeSoTimeout()
   {
      new ApacheClient.Builder().withSoTimeout(-1).build();
   }

   @Test
   public void testZeroSoTimeout()
   {
      new ApacheClient.Builder().withSoTimeout(0).build();
   }

   @Test
   public void testPositiveSoTimeout()
   {
      new ApacheClient.Builder().withSoTimeout(1).build();
   }

   // TODO verify reuse address?
   @Test
   public void testSoReuseAddress()
   {
      new ApacheClient.Builder().usingSoReuseAddress(true).build();
   }

   @Test
   public void testNoSoReuseAddress()
   {
      new ApacheClient.Builder().usingSoReuseAddress(false).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeSoLinger()
   {
      new ApacheClient.Builder().withSoLinger(-2).build();
   }

   @Test
   public void testNegativeSoLinger2()
   {
      new ApacheClient.Builder().withSoLinger(-1).build();
   }

   @Test
   public void testZeroSoLinger()
   {
      new ApacheClient.Builder().withSoLinger(0).build();
   }

   @Test
   public void testPositiveSoLinger()
   {
      new ApacheClient.Builder().withSoLinger(1).build();
   }

   // TODO verify so keepalive?
   @Test
   public void testSoKeepAlive()
   {
      new ApacheClient.Builder().usingSoKeepAlive(true).build();
   }

   @Test
   public void testNoSoKeepAlive()
   {
      new ApacheClient.Builder().usingSoKeepAlive(false).build();
   }

   // TODO verify tcp nodelay?
   @Test
   public void testTcpNoDelay()
   {
      new ApacheClient.Builder().usingTcpNoDelay(true).build();
   }

   @Test
   public void testNoTcpNoDelay()
   {
      new ApacheClient.Builder().usingTcpNoDelay(false).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeWaitForContinue()
   {
      new ApacheClient.Builder().withWaitForContinue(-1).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testZeroWaitForContinue()
   {
      new ApacheClient.Builder().withWaitForContinue(0).build();
   }

   @Test
   public void testPositiveWaitForContinue()
   {
      new ApacheClient.Builder().withWaitForContinue(1).build();
   }

   @Test
   public void testPutRequestNoneEntity() throws InterruptedException, ExecutionException
   {
      testWriteWithEntity(Method.PUT, null, "");
   }

   // TODO test SOH write at integration test level, using real byteBufferConsumer
   @Test
   public void testPutRequestWithEntity() throws InterruptedException, ExecutionException
   {
      testWriteWithEntity(Method.PUT, Entities.zeroes(1024), new String(new byte[1024]));
   }

   @Test
   public void testPostRequestNoneEntity() throws InterruptedException, ExecutionException
   {
      testWriteWithEntity(Method.POST, null, "");
   }

   @Test
   public void testPostRequestWithEntity() throws InterruptedException, ExecutionException
   {
      testWriteWithEntity(Method.POST, Entities.zeroes(1024), new String(new byte[1024]));
   }

   private void testWriteWithEntity(final Method method, final Entity entity, final String body)
         throws InterruptedException, ExecutionException
   {
      final HttpRequest.Builder b = new HttpRequest.Builder(method, this.objectUri);
      if (entity != null)
         b.withEntity(entity);

      final Request request = b.build();
      final Response response = this.client.execute(request).get();
      Assert.assertEquals(200, response.getStatusCode());
      Assert.assertEquals(EntityType.NONE, response.getEntity().getType());
      Assert.assertEquals(0, response.getEntity().getSize());
      verify(requestedFor(method, this.objectUri.getPath())
            .withRequestBody(equalTo(body)));
   }

   @Test
   public void testGetRequest() throws InterruptedException, ExecutionException
   {
      testReadRequest(Method.GET, 200, EntityType.ZEROES, 1024);
   }

   @Test
   public void testHeadRequest() throws InterruptedException, ExecutionException
   {
      testReadRequest(Method.HEAD, 200, EntityType.NONE, 0);
   }

   @Test
   public void testDeleteRequest() throws InterruptedException, ExecutionException
   {
      testReadRequest(Method.DELETE, 204, EntityType.NONE, 0);
   }

   private void testReadRequest(
         final Method method,
         final int statusCode,
         final EntityType type,
         final long size) throws InterruptedException, ExecutionException
   {
      final Request request = new HttpRequest.Builder(method, this.objectUri).build();
      final Response response = this.client.execute(request).get();
      Assert.assertEquals(statusCode, response.getStatusCode());
      Assert.assertEquals(type, response.getEntity().getType());
      Assert.assertEquals(size, response.getEntity().getSize());
      verify(requestedFor(method, this.objectUri.getPath()));
   }

   @Test
   public void testRequestHeaders() throws InterruptedException, ExecutionException
   {
      final Request request = new HttpRequest.Builder(Method.PUT, this.objectUri)
            .withHeader("key", "value")
            .withHeader("key2", "value2")
            .build();
      this.client.execute(request).get();
      verify(putRequestedFor(urlEqualTo(this.objectUri.getPath()))
            .withHeader("key", equalTo("value"))
            .withHeader("key2", equalTo("value2")));
   }

   @Test
   public void testContentLength() throws InterruptedException, ExecutionException
   {
      final Client client = new ApacheClient.Builder()
            .usingChunkedEncoding(false)
            .build();
      final Request request = new HttpRequest.Builder(Method.PUT, this.objectUri)
            .withEntity(Entities.zeroes(2048))
            .build();
      client.execute(request).get();
      verify(putRequestedFor(urlEqualTo(this.objectUri.getPath()))
            .withHeader("Content-Length", equalTo("2048"))
            .withoutHeader("Transfer-Encoding"));
   }

   @Test
   public void testChunkedEncoding() throws InterruptedException, ExecutionException
   {
      final Client client = new ApacheClient.Builder()
            .usingChunkedEncoding(true)
            .build();
      final Request request = new HttpRequest.Builder(Method.PUT, this.objectUri)
            .withEntity(Entities.zeroes(2048))
            .build();
      client.execute(request).get();
      verify(putRequestedFor(urlEqualTo(this.objectUri.getPath()))
            .withHeader("Transfer-Encoding", equalTo("chunked"))
            .withoutHeader("Content-Length"));
   }

   @Test
   public void testExpect100Continue() throws InterruptedException, ExecutionException
   {
      final Client client = new ApacheClient.Builder()
            .usingExpectContinue(true)
            .build();
      final Request request = new HttpRequest.Builder(Method.PUT, this.objectUri)
            .withEntity(Entities.zeroes(2048))
            .build();
      client.execute(request).get();
      verify(putRequestedFor(urlEqualTo(this.objectUri.getPath()))
            .withHeader("Expect", equalTo("100-continue")));
   }

   @Test
   public void testNoExpect100Continue() throws InterruptedException, ExecutionException
   {
      final Client client = new ApacheClient.Builder()
            .usingExpectContinue(false)
            .build();
      final Request request = new HttpRequest.Builder(Method.PUT, this.objectUri)
            .withEntity(Entities.zeroes(2048))
            .build();
      client.execute(request).get();
      verify(putRequestedFor(urlEqualTo(this.objectUri.getPath()))
            .withoutHeader("Expect"));
   }

   @Test
   public void testAuthentication() throws InterruptedException, ExecutionException
   {
      final Client client = new ApacheClient.Builder()
            .withAuthentication(new BasicAuth())
            .build();
      final Request request = new HttpRequest.Builder(Method.GET, this.objectUri)
            .withMetadata(Metadata.USERNAME, "test")
            .withMetadata(Metadata.PASSWORD, "test")
            .build();
      client.execute(request).get();
      verify(getRequestedFor(urlEqualTo(this.objectUri.getPath()))
            .withHeader("Authorization", matching("Basic .*")));
   }

   @Test
   public void testNoAuthentication() throws InterruptedException, ExecutionException
   {
      final Request request = new HttpRequest.Builder(Method.GET, this.objectUri).build();
      this.client.execute(request).get();
      verify(getRequestedFor(urlEqualTo(this.objectUri.getPath()))
            .withoutHeader("Authorization"));
   }

   @Test
   public void testUserAgent() throws InterruptedException, ExecutionException
   {
      final Client client = new ApacheClient.Builder()
            .withUserAgent("testUserAgent")
            .build();
      final Request request = new HttpRequest.Builder(Method.GET, this.objectUri).build();
      client.execute(request).get();
      verify(getRequestedFor(urlEqualTo(this.objectUri.getPath()))
            .withHeader("User-Agent", equalTo("testUserAgent")));
   }

   @Test
   public void testNoUserAgent() throws InterruptedException, ExecutionException
   {
      final Request request = new HttpRequest.Builder(Method.GET, this.objectUri).build();
      this.client.execute(request).get();
      verify(getRequestedFor(urlEqualTo(this.objectUri.getPath()))
            .withHeader("User-Agent", matching("Apache.*")));
   }

   @Test
   public void testSoTimeoutExceeded() throws InterruptedException, ExecutionException
   {
      final Client client = new ApacheClient.Builder()
            .withSoTimeout(5)
            .build();
      final Request request =
            new HttpRequest.Builder(Method.GET, this.delayedUri).build();
      final Response response = client.execute(request).get();
      Assert.assertEquals(499, response.getStatusCode());
      Assert.assertNotNull(response.getMetadata(Metadata.ABORTED));
   }

   @Test
   public void testRequestIdMetadata() throws InterruptedException, ExecutionException
   {
      final Request request = new HttpRequest.Builder(Method.GET, this.objectUri)
            .withMetadata(Metadata.REQUEST_ID, "objectName")
            .build();
      final Response response = this.client.execute(request).get();
      Assert.assertEquals("objectName", response.getMetadata(Metadata.REQUEST_ID));
   }

   @Test
   public void testImmediateShutdown() throws InterruptedException, ExecutionException
   {
      final Request request = new HttpRequest.Builder(Method.GET, this.delayedUri).build();
      this.client.execute(request);
      final long start = System.nanoTime();
      this.client.shutdown(true).get();
      final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
      // immediate shutdown takes less than 60 seconds
      Assert.assertTrue(duration < 60000);
   }

   @Test
   public void testGracefulShutdown() throws InterruptedException, ExecutionException
   {
      final Request request = new HttpRequest.Builder(Method.GET, this.delayedUri).build();
      this.client.execute(request);
      final long start = System.nanoTime();
      this.client.shutdown(false).get();
      final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
      // graceful shutdown takes at least request time
      Assert.assertTrue(duration >= 5000);
   }

   // TODO determine a way to test PUT redirect with 100-Continue; WireMock always
   // sends a 100-Continue intermediate response code prior to the user configured redirect
   // so it cannot be used to test this
   @Test
   public void testPutRedirectContentLength() throws InterruptedException, ExecutionException
   {
      testWriteRedirect(Method.PUT, false);
   }

   @Test
   public void testPutRedirectChunkedEncoding() throws InterruptedException, ExecutionException
   {
      testWriteRedirect(Method.PUT, true);
   }

   @Test
   public void testPostRedirectContentLength() throws InterruptedException, ExecutionException
   {
      testWriteRedirect(Method.POST, false);
   }

   @Test
   public void testPostRedirectChunkedEncoding() throws InterruptedException, ExecutionException
   {
      testWriteRedirect(Method.POST, true);
   }

   private void testWriteRedirect(final Method method, final boolean chunkedEncoding)
         throws InterruptedException, ExecutionException
   {
      final Client client = new ApacheClient.Builder()
            .usingChunkedEncoding(chunkedEncoding)
            .build();

      stubFinal(200);
      for (final int redirectStatusCode : this.redirectStatuses)
      {
         stubIntermediate(redirectStatusCode);
         final String rsc = String.valueOf(redirectStatusCode);
         final Request request = new HttpRequest.Builder(method, this.redirectUri)
               .withHeader("RedirectStatus", rsc)
               .withEntity(Entities.zeroes(1024))
               .build();
         final Response response = client.execute(request).get();
         Assert.assertEquals(200, response.getStatusCode());

         verify(requestedFor(method, this.redirectUri.getPath())
               .withHeader("RedirectStatus", equalTo(rsc))
               .withRequestBody(equalTo(new String(new byte[1024]))));

         verify(requestedFor(method, "/target")
               .withHeader("RedirectStatus", equalTo(rsc))
               .withRequestBody(equalTo(new String(new byte[1024]))));
      }
   }

   @Test
   public void testGetRedirect() throws InterruptedException, ExecutionException
   {
      testReadRedirect(Method.GET);
   }

   @Test
   public void testHeadRedirect() throws InterruptedException, ExecutionException
   {
      testReadRedirect(Method.HEAD);
   }

   @Test
   public void testDeleteRedirect() throws InterruptedException, ExecutionException
   {
      testReadRedirect(Method.DELETE);
   }

   private void testReadRedirect(final Method method) throws InterruptedException,
         ExecutionException
   {
      stubFinal(200);
      for (final int redirectStatusCode : this.redirectStatuses)
      {
         stubIntermediate(redirectStatusCode);
         final String rsc = String.valueOf(redirectStatusCode);
         final Request request = new HttpRequest.Builder(method, this.redirectUri)
               .withHeader("RedirectStatus", rsc)
               .build();
         final Response response = this.client.execute(request).get();
         Assert.assertEquals(200, response.getStatusCode());

         verify(requestedFor(method, this.redirectUri.getPath())
               .withHeader("RedirectStatus", equalTo(rsc)));

         verify(requestedFor(method, "/target")
               .withHeader("RedirectStatus", equalTo(rsc)));
      }
   }

   private void stubIntermediate(final int statusCode)
   {
      stubFor(any(urlEqualTo("/intermediate")).willReturn(
            aResponse().withStatus(statusCode)
                  .withHeader("location", "/target")
            ));
   }

   private void stubFinal(final int statusCode)
   {
      stubFor(any(urlEqualTo("/target")).willReturn(
            aResponse().withStatus(statusCode)
            ));
   }

   private RequestPatternBuilder requestedFor(final Method method, final String uri)
   {
      return new RequestPatternBuilder(RequestMethod.valueOf(method.toString()), urlEqualTo(uri));
   }

   @Test
   public void testWriteThroughput() throws InterruptedException, ExecutionException
   {
      final long size = SizeUnit.KILOBYTES.toBytes(100);
      final long tput = (long) (size * .9);
      final Client client =
            new ApacheClient.Builder().withWriteThroughput(tput).build();
      final Request request =
            new HttpRequest.Builder(Method.PUT, this.objectUri).withEntity(Entities.zeroes(size)).build();
      final long timestampStart = System.nanoTime();
      client.execute(request).get();
      final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);
      // at least 1 second
      Assert.assertTrue(duration >= 1000);
   }

   @Test
   public void testReadThroughput() throws InterruptedException, ExecutionException
   {
      final long size = SizeUnit.KILOBYTES.toBytes(100);
      final long tput = (long) (size * .9);
      stubFor(get(urlMatching("/container/.*")).willReturn(
            aResponse().withStatus(200)
                  .withBody(new byte[(int) size])
            ));

      final Client client =
            new ApacheClient.Builder().withReadThroughput(tput).build();
      final Request request = new HttpRequest.Builder(Method.GET, this.objectUri).build();
      final long timestampStart = System.nanoTime();
      client.execute(request).get();
      final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);
      // at least 1 second
      Assert.assertTrue(duration >= 1000);
   }

   @Test(expected = NullPointerException.class)
   public void testResponseBodyConsumerNullConsumerId()
   {
      new ApacheClient.Builder().withResponseBodyConsumer(null, mock(ResponseBodyConsumer.class)).build();
   }

   @Test(expected = NullPointerException.class)
   public void testResponseBodyConsumerNullConsumer()
   {
      new ApacheClient.Builder().withResponseBodyConsumer("object", null).build();
   }

   @Test
   public void testResponseBodyConsumer() throws InterruptedException, ExecutionException
   {
      final Request request = new HttpRequest.Builder(Method.GET, this.objectUri)
            .withMetadata(Metadata.RESPONSE_BODY_CONSUMER, "myConsumer").build();

      final Client client = new ApacheClient.Builder()
            .withResponseBodyConsumer("myConsumer", new ResponseBodyConsumer()
            {
               @Override
               public Iterator<Entry<String, String>> consume(
                     final int statusCode,
                     final InputStream response)
               {
                  final Entry<String, String> entry =
                        new AbstractMap.SimpleEntry<String, String>("myKey", "myValue");
                  return ImmutableSet.of(entry).iterator();
               }
            }).build();

      final Response response = client.execute(request).get();
      Assert.assertEquals("myValue", response.getMetadata("myKey"));
   }
}
