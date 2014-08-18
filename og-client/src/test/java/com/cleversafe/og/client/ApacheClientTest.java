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
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Client;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.http.BasicAuth;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpRequest;
import com.cleversafe.og.http.ResponseBodyConsumer;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class ApacheClientTest
{
   @ClassRule
   public static final WireMockClassRule WIREMOCK_RULE = new WireMockClassRule(8080);

   @Rule
   public WireMockClassRule wireMockRule = WIREMOCK_RULE;
   private Client client;
   private URI objectUri;
   private URI delayUri;

   @Before()
   public void before() throws URISyntaxException
   {
      this.client = new ApacheClient.Builder().build();
      stubFor(any(urlMatching("/container/.*")).willReturn(
            aResponse().withStatus(200)
            ));

      // read
      stubFor(get(urlMatching("/container/.*")).willReturn(
            aResponse().withStatus(200)
                  .withBody(new byte[1000])
            ));

      // 5 second delay
      stubFor(get(urlEqualTo("/delayed")).willReturn(
            aResponse().withStatus(200).withFixedDelay(2000)
            ));

      stubFor(any(urlEqualTo("/301")).willReturn(
            aResponse().withStatus(301)
                  .withHeader("location", "/container/")
            ));

      stubFor(any(urlEqualTo("/302")).willReturn(
            aResponse().withStatus(302)
                  .withHeader("location", "/container/")
            ));

      stubFor(any(urlEqualTo("/307")).willReturn(
            aResponse().withStatus(307)
                  .withHeader("location", "/container/")
            ));

      this.objectUri = uri("/container/object");
      this.delayUri = uri("/delayed");
   }

   private static URI uri(final String path) throws URISyntaxException
   {
      return new URI("http://127.0.0.1:8080" + path);
   }

   @Test(expected = IllegalArgumentException.class)
   public void negativeConnectTimeout()
   {
      new ApacheClient.Builder().withConnectTimeout(-1).build();
   }

   @Test
   public void zeroConnectTimeout()
   {
      new ApacheClient.Builder().withConnectTimeout(0).build();
   }

   @Test
   public void positiveConnectTimeout()
   {
      new ApacheClient.Builder().withConnectTimeout(1).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void negativeSoTimeout()
   {
      new ApacheClient.Builder().withSoTimeout(-1).build();
   }

   @Test
   public void zeroSoTimeout()
   {
      new ApacheClient.Builder().withSoTimeout(0).build();
   }

   @Test
   public void positiveSoTimeout()
   {
      new ApacheClient.Builder().withSoTimeout(1).build();
   }

   // TODO verify reuse address?
   @Test
   public void soReuseAddress()
   {
      new ApacheClient.Builder().usingSoReuseAddress(true).build();
   }

   @Test
   public void noSoReuseAddress()
   {
      new ApacheClient.Builder().usingSoReuseAddress(false).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void negativeSoLinger()
   {
      new ApacheClient.Builder().withSoLinger(-2).build();
   }

   @Test
   public void negativeSoLinger2()
   {
      new ApacheClient.Builder().withSoLinger(-1).build();
   }

   @Test
   public void zeroSoLinger()
   {
      new ApacheClient.Builder().withSoLinger(0).build();
   }

   @Test
   public void positiveSoLinger()
   {
      new ApacheClient.Builder().withSoLinger(1).build();
   }

   // TODO verify so keepalive?
   @Test
   public void soKeepAlive()
   {
      new ApacheClient.Builder().usingSoKeepAlive(true).build();
   }

   @Test
   public void noSoKeepAlive()
   {
      new ApacheClient.Builder().usingSoKeepAlive(false).build();
   }

   // TODO verify tcp nodelay?
   @Test
   public void tcpNoDelay()
   {
      new ApacheClient.Builder().usingTcpNoDelay(true).build();
   }

   @Test
   public void noTcpNoDelay()
   {
      new ApacheClient.Builder().usingTcpNoDelay(false).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void negativeWaitForContinue()
   {
      new ApacheClient.Builder().withWaitForContinue(-1).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void zeroWaitForContinue()
   {
      new ApacheClient.Builder().withWaitForContinue(0).build();
   }

   @Test
   public void positiveWaitForContinue()
   {
      new ApacheClient.Builder().withWaitForContinue(1).build();
   }

   @DataProvider
   public static Object[][] provideExecute()
   {
      final Body zeroes = Bodies.zeroes(1000);
      final Body none = Bodies.none();
      final String content = new String(new byte[1000]);

      return new Object[][]{
            {Method.PUT, none, "", none},
            {Method.PUT, zeroes, content, none},
            {Method.POST, none, "", none},
            {Method.POST, zeroes, content, none},
            {Method.GET, none, "", zeroes},
            {Method.HEAD, none, "", none},
            {Method.DELETE, none, "", none}
      };
   }

   @Test
   @UseDataProvider("provideExecute")
   public void executoe(
         final Method method,
         final Body requestBody,
         final String requestData,
         final Body responseBody)
         throws InterruptedException, ExecutionException
   {
      final Request request =
            new HttpRequest.Builder(method, this.objectUri).withBody(requestBody).build();
      final Response response = this.client.execute(request).get();

      assertThat(response.getStatusCode(), is(200));
      assertThat(response.getBody().getData(), is(responseBody.getData()));
      assertThat(response.getBody().getSize(), is(responseBody.getSize()));

      verify(requestedFor(method, this.objectUri.getPath())
            .withRequestBody(equalTo(requestData)));
   }

   @Test
   public void requestHeaders() throws InterruptedException, ExecutionException
   {
      final Request request = new HttpRequest.Builder(Method.PUT, this.objectUri)
            .withHeader("key", "value")
            .build();
      this.client.execute(request).get();
      verify(putRequestedFor(urlEqualTo(this.objectUri.getPath()))
            .withHeader("key", equalTo("value")));
   }

   @DataProvider
   public static Object[][] provideEncode()
   {
      final String contentLength = "Content-Length";
      final String transferEncoding = "Transfer-Encoding";
      return new Object[][]{
            {false, contentLength, "2048", transferEncoding},
            {true, transferEncoding, "chunked", contentLength},
      };
   }

   @Test
   @UseDataProvider("provideEncode")
   public void encode(final boolean chunk, final String key, final String value, final String absent)
         throws InterruptedException, ExecutionException
   {
      final Client client = new ApacheClient.Builder().usingChunkedEncoding(chunk).build();
      final Request request = new HttpRequest.Builder(Method.PUT, this.objectUri)
            .withBody(Bodies.zeroes(2048))
            .build();
      client.execute(request).get();
      verify(putRequestedFor(urlEqualTo(this.objectUri.getPath()))
            .withHeader(key, equalTo(value))
            .withoutHeader(absent));
   }

   @Test
   public void expect100Continue() throws InterruptedException, ExecutionException
   {
      final Client client = new ApacheClient.Builder()
            .usingExpectContinue(true)
            .build();
      final Request request = new HttpRequest.Builder(Method.PUT, this.objectUri)
            .withBody(Bodies.zeroes(2048))
            .build();
      client.execute(request).get();
      verify(putRequestedFor(urlEqualTo(this.objectUri.getPath()))
            .withHeader("Expect", equalTo("100-continue")));
   }

   @Test
   public void noExpect100Continue() throws InterruptedException, ExecutionException
   {
      final Client client = new ApacheClient.Builder()
            .usingExpectContinue(false)
            .build();
      final Request request = new HttpRequest.Builder(Method.PUT, this.objectUri)
            .withBody(Bodies.zeroes(2048))
            .build();
      client.execute(request).get();
      verify(putRequestedFor(urlEqualTo(this.objectUri.getPath()))
            .withoutHeader("Expect"));
   }

   @Test
   public void authentication() throws InterruptedException, ExecutionException
   {
      final Client client = new ApacheClient.Builder()
            .withAuthentication(new BasicAuth())
            .build();
      final Request request = new HttpRequest.Builder(Method.GET, this.objectUri)
            .withHeader(Headers.X_OG_USERNAME, "test")
            .withHeader(Headers.X_OG_PASSWORD, "test")
            .build();
      client.execute(request).get();
      verify(getRequestedFor(urlEqualTo(this.objectUri.getPath()))
            .withHeader("Authorization", matching("Basic .*")));
   }

   @Test
   public void noAuthentication() throws InterruptedException, ExecutionException
   {
      final Request request = new HttpRequest.Builder(Method.GET, this.objectUri).build();
      this.client.execute(request).get();
      verify(getRequestedFor(urlEqualTo(this.objectUri.getPath()))
            .withoutHeader("Authorization"));
   }

   @Test
   public void userAgent() throws InterruptedException, ExecutionException
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
   public void noUserAgent() throws InterruptedException, ExecutionException
   {
      final Request request = new HttpRequest.Builder(Method.GET, this.objectUri).build();
      this.client.execute(request).get();
      verify(getRequestedFor(urlEqualTo(this.objectUri.getPath()))
            .withHeader("User-Agent", matching("Apache.*")));
   }

   @Test
   public void soTimeoutExceeded() throws InterruptedException, ExecutionException
   {
      final Client client = new ApacheClient.Builder()
            .withSoTimeout(1)
            .build();
      final Request request = new HttpRequest.Builder(Method.GET, this.delayUri).build();
      final Response response = client.execute(request).get();

      assertThat(response.getStatusCode(), is(499));
   }

   @Test
   public void requestId() throws InterruptedException, ExecutionException
   {
      final Request request = new HttpRequest.Builder(Method.GET, this.objectUri)
            .withHeader(Headers.X_OG_REQUEST_ID, "1")
            .build();
      final Response response = this.client.execute(request).get();
      assertThat(response.headers(), hasEntry(Headers.X_OG_REQUEST_ID, "1"));
   }

   @Test
   public void immediateShutdown() throws InterruptedException, ExecutionException
   {
      final Request request = new HttpRequest.Builder(Method.GET, this.delayUri).build();
      this.client.execute(request);
      final long start = System.nanoTime();
      this.client.shutdown(true).get();
      final long duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
      // immediate shutdown takes less than 60 seconds
      assertThat(duration, lessThan(60L));
   }

   @Test
   public void gracefulShutdown() throws InterruptedException, ExecutionException
   {
      final Request request = new HttpRequest.Builder(Method.GET, this.delayUri).build();
      this.client.execute(request);
      final long start = System.nanoTime();
      this.client.shutdown(false).get();
      final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
      // graceful shutdown takes at least request time
      assertThat(duration, greaterThanOrEqualTo(2000L));
   }

   // TODO determine a way to test PUT redirect with 100-Continue; WireMock always
   // sends a 100-Continue intermediate response code prior to the user configured redirect
   // so it cannot be used to test this

   @DataProvider
   public static Object[][] provideRedirect() throws URISyntaxException
   {
      final URI one = uri("/301");
      final URI two = uri("/302");
      final URI three = uri("/307");
      final Body zeroes = Bodies.zeroes(1000);
      final Body none = Bodies.none();
      final String content = new String(new byte[1000]);

      return new Object[][]{
            {Method.PUT, one, zeroes, content, none, false},
            {Method.PUT, two, zeroes, content, none, false},
            {Method.PUT, three, zeroes, content, none, false},

            {Method.PUT, one, zeroes, content, none, true},
            {Method.PUT, two, zeroes, content, none, true},
            {Method.PUT, three, zeroes, content, none, true},

            {Method.POST, one, zeroes, content, none, false},
            {Method.POST, two, zeroes, content, none, false},
            {Method.POST, three, zeroes, content, none, false},

            {Method.POST, one, zeroes, content, none, true},
            {Method.POST, two, zeroes, content, none, true},
            {Method.POST, three, zeroes, content, none, true},

            {Method.GET, one, none, "", zeroes, false},
            {Method.GET, two, none, "", zeroes, false},
            {Method.GET, three, none, "", zeroes, false},

            {Method.HEAD, one, none, "", none, false},
            {Method.HEAD, two, none, "", none, false},
            {Method.HEAD, three, none, "", none, false},

            {Method.DELETE, one, none, "", none, false},
            {Method.DELETE, two, none, "", none, false},
            {Method.DELETE, three, none, "", none, false},
      };
   }

   @Test
   @UseDataProvider("provideRedirect")
   public void redirect(
         final Method method,
         final URI uri,
         final Body requestBody,
         final String requestData,
         final Body responseBody,
         final boolean chunkedEncoding)
         throws InterruptedException, ExecutionException
   {
      final Client client = new ApacheClient.Builder()
            .usingChunkedEncoding(chunkedEncoding)
            .build();

      final Request request = new HttpRequest.Builder(method, uri).withBody(requestBody).build();
      final Response response = client.execute(request).get();
      assertThat(response.getStatusCode(), is(200));
      assertThat(response.getBody().getData(), is(responseBody.getData()));
      assertThat(response.getBody().getSize(), is(responseBody.getSize()));

      verify(requestedFor(method, uri.getPath())
            .withRequestBody(equalTo(requestData)));

      verify(requestedFor(method, "/container/")
            .withRequestBody(equalTo(requestData)));
   }

   private RequestPatternBuilder requestedFor(final Method method, final String uri)
   {
      return new RequestPatternBuilder(RequestMethod.valueOf(method.toString()), urlEqualTo(uri));
   }

   @Test
   public void writeThroughput() throws InterruptedException, ExecutionException
   {
      final Client client = new ApacheClient.Builder().withWriteThroughput(1000).build();
      final Request request =
            new HttpRequest.Builder(Method.PUT, this.objectUri).withBody(Bodies.zeroes(50)).build();
      final long timestampStart = System.nanoTime();
      client.execute(request).get();
      final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);

      assertThat(duration, greaterThanOrEqualTo(40L));
   }

   @Test
   public void readThroughput() throws InterruptedException, ExecutionException
   {
      final Client client = new ApacheClient.Builder().withReadThroughput(20000).build();
      final Request request = new HttpRequest.Builder(Method.GET, this.objectUri).build();
      final long timestampStart = System.nanoTime();
      client.execute(request).get();
      final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);

      assertThat(duration, greaterThanOrEqualTo(40L));
   }

   @Test(expected = NullPointerException.class)
   public void responseBodyConsumerNullConsumerId()
   {
      new ApacheClient.Builder().withResponseBodyConsumer(null, mock(ResponseBodyConsumer.class)).build();
   }

   @Test(expected = NullPointerException.class)
   public void rResponseBodyConsumerNullConsumer()
   {
      new ApacheClient.Builder().withResponseBodyConsumer("object", null).build();
   }

   @Test
   public void responseBodyConsumer() throws InterruptedException, ExecutionException
   {
      final Request request = new HttpRequest.Builder(Method.GET, this.objectUri)
            .withHeader(Headers.X_OG_RESPONSE_BODY_CONSUMER, "consumer").build();

      final Client client = new ApacheClient.Builder()
            .withResponseBodyConsumer("consumer", new ResponseBodyConsumer()
            {
               @Override
               public Iterator<Entry<String, String>> consume(
                     final int statusCode,
                     final InputStream response)
               {
                  return ImmutableMap.of("key", "value").entrySet().iterator();
               }
            }).build();

      final Response response = client.execute(request).get();
      assertThat(response.headers(), hasEntry("key", "value"));
   }
}
