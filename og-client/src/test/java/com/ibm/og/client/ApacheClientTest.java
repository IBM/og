/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.client;

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
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.ibm.og.http.BasicAuth;
import com.ibm.og.http.Bodies;
import com.ibm.og.http.ResponseBodyConsumer;
import com.ibm.og.util.Context;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.og.api.Body;
import com.ibm.og.api.Client;
import com.ibm.og.api.Method;
import com.ibm.og.api.Operation;
import com.ibm.og.api.Request;
import com.ibm.og.api.Response;
import com.ibm.og.http.HttpRequest;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class ApacheClientTest {
  @ClassRule
  public static final WireMockClassRule WIREMOCK_RULE = new WireMockClassRule(8080);

  @Rule
  public WireMockClassRule wireMockRule = WIREMOCK_RULE;
  private Client client;
  private URI objectUri;
  private URI delayUri;
  private Operation operation;

  @Before()
  public void before() throws URISyntaxException {
    this.client = new ApacheClient.Builder().build();
    stubFor(any(urlMatching("/container/.*")).willReturn(aResponse().withStatus(200)));

    // read
    stubFor(get(urlMatching("/container/.*"))
        .willReturn(aResponse().withStatus(200).withBody(new byte[1000])));

    // 5 second delay
    stubFor(
        get(urlEqualTo("/delayed")).willReturn(aResponse().withStatus(200).withFixedDelay(1000)));

    stubFor(any(urlEqualTo("/301"))
        .willReturn(aResponse().withStatus(301).withHeader("location", "/container/")));

    stubFor(any(urlEqualTo("/302"))
        .willReturn(aResponse().withStatus(302).withHeader("location", "/container/")));

    stubFor(any(urlEqualTo("/307"))
        .willReturn(aResponse().withStatus(307).withHeader("location", "/container/")));

    this.objectUri = uri("/container/object");
    this.delayUri = uri("/delayed");
    this.operation = Operation.WRITE;
  }

  private static URI uri(final String path) throws URISyntaxException {
    return new URI("http://127.0.0.1:8080" + path);
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeConnectTimeout() {
    new ApacheClient.Builder().withConnectTimeout(-1).build();
  }

  @Test
  public void zeroConnectTimeout() {
    new ApacheClient.Builder().withConnectTimeout(0).build();
  }

  @Test
  public void positiveConnectTimeout() {
    new ApacheClient.Builder().withConnectTimeout(1).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeSoTimeout() {
    new ApacheClient.Builder().withSoTimeout(-1).build();
  }

  @Test
  public void zeroSoTimeout() {
    new ApacheClient.Builder().withSoTimeout(0).build();
  }

  @Test
  public void positiveSoTimeout() {
    new ApacheClient.Builder().withSoTimeout(1).build();
  }

  // TODO verify reuse address?
  @Test
  public void soReuseAddress() {
    new ApacheClient.Builder().usingSoReuseAddress(true).build();
  }

  @Test
  public void noSoReuseAddress() {
    new ApacheClient.Builder().usingSoReuseAddress(false).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeSoLinger() {
    new ApacheClient.Builder().withSoLinger(-2).build();
  }

  @Test
  public void negativeSoLinger2() {
    new ApacheClient.Builder().withSoLinger(-1).build();
  }

  @Test
  public void zeroSoLinger() {
    new ApacheClient.Builder().withSoLinger(0).build();
  }

  @Test
  public void positiveSoLinger() {
    new ApacheClient.Builder().withSoLinger(1).build();
  }

  // TODO verify so keepalive?
  @Test
  public void soKeepAlive() {
    new ApacheClient.Builder().usingSoKeepAlive(true).build();
  }

  @Test
  public void noSoKeepAlive() {
    new ApacheClient.Builder().usingSoKeepAlive(false).build();
  }

  // TODO verify tcp nodelay?
  @Test
  public void tcpNoDelay() {
    new ApacheClient.Builder().usingTcpNoDelay(true).build();
  }

  @Test
  public void noTcpNoDelay() {
    new ApacheClient.Builder().usingTcpNoDelay(false).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeSoSndBuf() {
    new ApacheClient.Builder().withSoSndBuf(-1).build();
  }

  @Test
  public void zeroSoSndBuf() {
    new ApacheClient.Builder().withSoSndBuf(0).build();
  }

  @Test
  public void positiveSoSndBuf() {
    new ApacheClient.Builder().withSoSndBuf(1).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeSoRcvBuf() {
    new ApacheClient.Builder().withSoRcvBuf(-1).build();
  }

  @Test
  public void zeroSoRcvBuf() {
    new ApacheClient.Builder().withSoRcvBuf(0).build();
  }

  @Test
  public void positiveSoRcvBuf() {
    new ApacheClient.Builder().withSoRcvBuf(1).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeValidateAfterInactivity() {
    new ApacheClient.Builder().withValidateAfterInactivity(-1).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void zeroValidateAfterInactivity() {
    new ApacheClient.Builder().withValidateAfterInactivity(0).build();
  }

  @Test
  public void positiveValidateAfterInactivity() {
    new ApacheClient.Builder().withValidateAfterInactivity(1).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeMaxIdleTime() {
    new ApacheClient.Builder().withMaxIdleTime(-1).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void zeroMaxIdleTime() {
    new ApacheClient.Builder().withMaxIdleTime(0).build();
  }

  @Test
  public void positiveMaxIdleTime() {
    // using 100 rather than 1ms and also shutting down client immediately to kill background
    // eviction thread that is created when using max idle time
    new ApacheClient.Builder().withMaxIdleTime(100).build().shutdown(true, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeWaitForContinue() {
    new ApacheClient.Builder().withWaitForContinue(-1).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void zeroWaitForContinue() {
    new ApacheClient.Builder().withWaitForContinue(0).build();
  }

  @Test
  public void positiveWaitForContinue() {
    new ApacheClient.Builder().withWaitForContinue(1).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeRetryCount() {
    new ApacheClient.Builder().withRetryCount(-1).build();
  }

  @Test
  public void zeroRetryCount() {
    new ApacheClient.Builder().withRetryCount(0).build();
  }

  @Test
  public void positiveRetryCount() {
    new ApacheClient.Builder().withRetryCount(1).build();
  }

  @Test(expected = NullPointerException.class)
  public void nullAuthentication() {
    new ApacheClient.Builder().withAuthentication(null).build();
  }

  @DataProvider
  public static Object[][] provideExecute() {
    final Body zeroes = Bodies.zeroes(1000);
    final Body none = Bodies.none();
    final String content = new String(new byte[1000]);

    return new Object[][] {{Method.PUT, none, "", none}, {Method.PUT, zeroes, content, none},
        {Method.POST, none, "", none}, {Method.POST, zeroes, content, none},
        {Method.GET, none, "", zeroes}, {Method.HEAD, none, "", none},
        {Method.DELETE, none, "", none}};
  }

  @Test
  @UseDataProvider("provideExecute")
  public void execute(final Method method, final Body requestBody, final String requestData,
      final Body responseBody) throws InterruptedException, ExecutionException {
    final Request request = new HttpRequest.Builder(method, this.objectUri, this.operation)
        .withBody(requestBody).build();
    final Response response = this.client.execute(request).get();

    assertThat(response.getStatusCode(), is(200));
    assertThat(response.getBody().getDataType(), is(responseBody.getDataType()));
    assertThat(response.getBody().getSize(), is(responseBody.getSize()));

    verify(requestedFor(method, this.objectUri.getPath()).withRequestBody(equalTo(requestData)));
  }

  @Test
  public void requestHeaders() throws InterruptedException, ExecutionException {
    final Request request = new HttpRequest.Builder(Method.PUT, this.objectUri, this.operation)
        .withHeader("key", "value").build();
    this.client.execute(request).get();
    verify(
        putRequestedFor(urlEqualTo(this.objectUri.getPath())).withHeader("key", equalTo("value")));
  }

  @DataProvider
  public static Object[][] provideEncode() {
    final String contentLength = "Content-Length";
    final String transferEncoding = "Transfer-Encoding";
    return new Object[][] {{false, contentLength, "2048", transferEncoding},
        {true, transferEncoding, "chunked", contentLength},};
  }

  @Test
  @UseDataProvider("provideEncode")
  public void encode(final boolean chunk, final String key, final String value, final String absent)
      throws InterruptedException, ExecutionException {
    final Client client = new ApacheClient.Builder().usingChunkedEncoding(chunk).build();
    final Request request = new HttpRequest.Builder(Method.PUT, this.objectUri, this.operation)
        .withBody(Bodies.zeroes(2048)).build();
    client.execute(request).get();
    verify(putRequestedFor(urlEqualTo(this.objectUri.getPath())).withHeader(key, equalTo(value))
        .withoutHeader(absent));
  }

  @Test
  public void expect100Continue() throws InterruptedException, ExecutionException {
    final Client client = new ApacheClient.Builder().usingExpectContinue(true).build();
    final Request request = new HttpRequest.Builder(Method.PUT, this.objectUri, this.operation)
        .withBody(Bodies.zeroes(2048)).build();
    client.execute(request).get();
    verify(putRequestedFor(urlEqualTo(this.objectUri.getPath())).withHeader("Expect",
        equalTo("100-continue")));
  }

  @Test
  public void noExpect100Continue() throws InterruptedException, ExecutionException {
    final Client client = new ApacheClient.Builder().usingExpectContinue(false).build();
    final Request request = new HttpRequest.Builder(Method.PUT, this.objectUri, this.operation)
        .withBody(Bodies.zeroes(2048)).build();
    client.execute(request).get();
    verify(putRequestedFor(urlEqualTo(this.objectUri.getPath())).withoutHeader("Expect"));
  }

  @Test
  public void authentication() throws InterruptedException, ExecutionException {
    final Client client = new ApacheClient.Builder().withAuthentication(new BasicAuth()).build();
    final Request request = new HttpRequest.Builder(Method.GET, this.objectUri, this.operation)
        .withContext(Context.X_OG_USERNAME, "test").withContext(Context.X_OG_PASSWORD, "test")
        .build();
    client.execute(request).get();
    verify(getRequestedFor(urlEqualTo(this.objectUri.getPath())).withHeader("Authorization",
        matching("Basic .*")));
  }

  @Test
  public void noAuthentication() throws InterruptedException, ExecutionException {
    final Request request =
        new HttpRequest.Builder(Method.GET, this.objectUri, this.operation).build();
    this.client.execute(request).get();
    verify(getRequestedFor(urlEqualTo(this.objectUri.getPath())).withoutHeader("Authorization"));
  }

  @Test
  public void userAgent() throws InterruptedException, ExecutionException {
    final Client client = new ApacheClient.Builder().withUserAgent("testUserAgent").build();
    final Request request =
        new HttpRequest.Builder(Method.GET, this.objectUri, this.operation).build();
    client.execute(request).get();
    verify(getRequestedFor(urlEqualTo(this.objectUri.getPath())).withHeader("User-Agent",
        equalTo("testUserAgent")));
  }

  @Test
  public void noUserAgent() throws InterruptedException, ExecutionException {
    final Request request =
        new HttpRequest.Builder(Method.GET, this.objectUri, this.operation).build();
    this.client.execute(request).get();
    verify(getRequestedFor(urlEqualTo(this.objectUri.getPath())).withHeader("User-Agent",
        matching("Apache.*")));
  }

  @Test
  public void soTimeoutExceeded() throws InterruptedException, ExecutionException {
    final Client client = new ApacheClient.Builder().withSoTimeout(1).build();
    final Request request =
        new HttpRequest.Builder(Method.GET, this.delayUri, this.operation).build();
    final Response response = client.execute(request).get();

    assertThat(response.getStatusCode(), is(599));
  }

  @Test
  public void requestId() throws InterruptedException, ExecutionException {
    final Request request = new HttpRequest.Builder(Method.GET, this.objectUri, this.operation)
        .withContext(Context.X_OG_REQUEST_ID, "1").build();
    final Response response = this.client.execute(request).get();
    assertThat(response.getContext(), hasEntry(Context.X_OG_REQUEST_ID, "1"));
  }

  @Test
  public void immediateShutdown() throws InterruptedException, ExecutionException {
    final Request request =
        new HttpRequest.Builder(Method.GET, this.delayUri, this.operation).build();
    this.client.execute(request);
    final long start = System.nanoTime();
    this.client.shutdown(true, 0).get();
    final long duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
    // immediate shutdown takes less than 10 seconds
    assertThat(duration, lessThan(10L));
  }

  @Test
  public void gracefulShutdown() throws InterruptedException, ExecutionException {
    final Request request =
        new HttpRequest.Builder(Method.GET, this.delayUri, this.operation).build();
    this.client.execute(request);
    final long start = System.nanoTime();
    this.client.shutdown(false, 60).get();
    final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    // graceful shutdown takes at least request time
    assertThat(duration, greaterThanOrEqualTo(1000L));
  }

  // TODO determine a way to test PUT redirect with 100-Continue; WireMock always
  // sends a 100-Continue intermediate response code prior to the user configured redirect
  // so it cannot be used to test this

  @DataProvider
  public static Object[][] provideRedirect() throws URISyntaxException {
    final URI one = uri("/301");
    final URI two = uri("/302");
    final URI three = uri("/307");
    final Body zeroes = Bodies.zeroes(1000);
    final Body none = Bodies.none();
    final String content = new String(new byte[1000]);
    final Operation write = Operation.WRITE;
    final Operation read = Operation.READ;
    final Operation metadata = Operation.METADATA;
    final Operation overwrite = Operation.OVERWRITE;
    final Operation delete = Operation.DELETE;

    return new Object[][] {{Method.PUT, one, write, zeroes, content, none, false},
        {Method.PUT, two, write, zeroes, content, none, false},
        {Method.PUT, three, write, zeroes, content, none, false},

        {Method.PUT, one, overwrite, zeroes, content, none, true},
        {Method.PUT, two, overwrite, zeroes, content, none, true},
        {Method.PUT, three, overwrite, zeroes, content, none, true},

        {Method.POST, one, write, zeroes, content, none, false},
        {Method.POST, two, write, zeroes, content, none, false},
        {Method.POST, three, write, zeroes, content, none, false},

        {Method.POST, one, write, zeroes, content, none, true},
        {Method.POST, two, write, zeroes, content, none, true},
        {Method.POST, three, write, zeroes, content, none, true},

        {Method.GET, one, read, none, "", zeroes, false},
        {Method.GET, two, read, none, "", zeroes, false},
        {Method.GET, three, read, none, "", zeroes, false},

        {Method.HEAD, one, metadata, none, "", none, false},
        {Method.HEAD, two, metadata, none, "", none, false},
        {Method.HEAD, three, metadata, none, "", none, false},

        {Method.DELETE, one, delete, none, "", none, false},
        {Method.DELETE, two, delete, none, "", none, false},
        {Method.DELETE, three, delete, none, "", none, false},};
  }

  @Test
  @UseDataProvider("provideRedirect")
  public void redirect(final Method method, final URI uri, final Operation operation,
      final Body requestBody, final String requestData, final Body responseBody,
      final boolean chunkedEncoding) throws InterruptedException, ExecutionException {
    final Client client = new ApacheClient.Builder().usingChunkedEncoding(chunkedEncoding).build();

    final HttpRequest.Builder builder =
        new HttpRequest.Builder(method, uri, operation);
    builder.withBody(requestBody);
    builder.withContext(Context.X_OG_OBJECT_SIZE, String.valueOf(requestBody.getSize()));
    final Request request = builder.build();

    final Response response = client.execute(request).get();
    assertThat(response.getStatusCode(), is(200));
    assertThat(response.getBody().getDataType(), is(responseBody.getDataType()));
    assertThat(response.getBody().getSize(), is(responseBody.getSize()));

    verify(requestedFor(method, uri.getPath()).withRequestBody(equalTo(requestData)));

    verify(requestedFor(method, "/container/").withRequestBody(equalTo(requestData)));
  }

  private RequestPatternBuilder requestedFor(final Method method, final String uri) {
    return new RequestPatternBuilder(RequestMethod.fromString(method.toString()), urlEqualTo(uri));
  }

  @Test
  public void writeThroughput() throws InterruptedException, ExecutionException {
    final Client client = new ApacheClient.Builder().withWriteThroughput(1000).build();
    final Request request = new HttpRequest.Builder(Method.PUT, this.objectUri, this.operation)
        .withBody(Bodies.zeroes(50)).build();
    final long timestampStart = System.nanoTime();
    client.execute(request).get();
    final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);

    assertThat(duration, greaterThanOrEqualTo(40L));
  }

  @Test
  public void readThroughput() throws InterruptedException, ExecutionException {
    final Client client = new ApacheClient.Builder().withReadThroughput(20000).build();
    final Request request =
        new HttpRequest.Builder(Method.GET, this.objectUri, this.operation).build();
    final long timestampStart = System.nanoTime();
    client.execute(request).get();
    final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);

    assertThat(duration, greaterThanOrEqualTo(40L));
  }

  @Test(expected = NullPointerException.class)
  public void responseBodyConsumerNullConsumerId() {
    new ApacheClient.Builder().withResponseBodyConsumer(null, mock(ResponseBodyConsumer.class))
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void responseBodyConsumerNullConsumer() {
    new ApacheClient.Builder().withResponseBodyConsumer("object", null).build();
  }

  @Test
  public void responseBodyConsumer() throws InterruptedException, ExecutionException {
    final Request request = new HttpRequest.Builder(Method.GET, this.objectUri, this.operation)
        .withContext(Context.X_OG_RESPONSE_BODY_CONSUMER, "consumer").build();

    final Client client =
        new ApacheClient.Builder().withResponseBodyConsumer("consumer", new ResponseBodyConsumer() {
          @Override
          public Map<String, String> consume(final int statusCode, final InputStream response) {
            return ImmutableMap.of("key", "value");
          }
        }).build();

    final Response response = client.execute(request).get();
    assertThat(response.getContext(), hasEntry("key", "value"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullTrustStoreWithTrustStorePassword() {
    new ApacheClient.Builder().withTrustStorePassword("password").build();
  }
}
