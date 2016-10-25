/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.client;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Security;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.util.PublicSuffixMatcher;
import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.api.AuthenticatedRequest;
import com.cleversafe.og.api.Client;
import com.cleversafe.og.api.DataType;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.client.RequestLogEntry.RequestTimestamps;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpAuth;
import com.cleversafe.og.http.HttpResponse;
import com.cleversafe.og.http.NoneAuth;
import com.cleversafe.og.http.ResponseBodyConsumer;
import com.cleversafe.og.util.Context;
import com.cleversafe.og.util.io.MonitoringInputStream;
import com.cleversafe.og.util.io.Streams;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ForwardingListenableFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * A {@code Client} implementation that uses the Apache HttpComponents HttpClient library as its
 * backing library for executing http requests
 * 
 * @since 1.0
 */
public class ApacheClient implements Client {
  private static final Logger _logger = LoggerFactory.getLogger(ApacheClient.class);
  private static final Logger _requestLogger = LoggerFactory.getLogger("RequestLogger");
  private final int connectTimeout;
  private final int soTimeout;
  private final boolean soReuseAddress;
  private final int soLinger;
  private final boolean soKeepAlive;
  private final boolean tcpNoDelay;
  private final int soSndBuf;
  private final int soRcvBuf;
  private final boolean persistentConnections;
  private final int validateAfterInactivity;
  private final int maxIdleTime;
  private final boolean chunkedEncoding;
  private final boolean expectContinue;
  private final int waitForContinue;
  private final int retryCount;
  private final boolean requestSentRetry;
  private final List<String> protocols;
  private final List<String> cipherSuites;
  private final File keyStore;
  private final String keyStorePassword;
  private final String keyPassword;
  private final File trustStore;
  private final String trustStorePassword;
  private final boolean trustSelfSignedCertificates;
  private final int dnsCacheTtl;
  private final int dnsCacheNegativeTtl;
  private final HttpAuth authentication;
  private final String userAgent;
  private final long writeThroughput;
  private final long readThroughput;
  private final Map<String, ResponseBodyConsumer> responseBodyConsumers;
  private volatile boolean running;
  private final AtomicInteger abortedRequestsAtShutdown;
  private final CloseableHttpClient client;
  private final ListeningExecutorService executorService;
  private final Gson gson;

  private ApacheClient(final Builder builder) {
    this.connectTimeout = builder.connectTimeout;
    this.soTimeout = builder.soTimeout;
    this.soReuseAddress = builder.soReuseAddress;
    this.soLinger = builder.soLinger;
    this.soKeepAlive = builder.soKeepAlive;
    this.tcpNoDelay = builder.tcpNoDelay;
    this.soSndBuf = builder.soSndBuf;
    this.soRcvBuf = builder.soRcvBuf;
    this.persistentConnections = builder.persistentConnections;
    this.validateAfterInactivity = builder.validateAfterInactivity;
    this.maxIdleTime = builder.maxIdleTime;
    this.chunkedEncoding = builder.chunkedEncoding;
    this.expectContinue = builder.expectContinue;
    this.waitForContinue = builder.waitForContinue;
    this.retryCount = builder.retryCount;
    this.requestSentRetry = builder.requestSentRetry;

    // TODO validate protocol values
    final List<String> protocols = builder.protocols;
    if (protocols != null) {
      this.protocols = ImmutableList.copyOf(protocols);
    } else {
      this.protocols = null;
    }

    final List<String> cipherSuites = builder.cipherSuites;
    if (cipherSuites != null) {
      this.cipherSuites = ImmutableList.copyOf(cipherSuites);
    } else {
      this.cipherSuites = null;
    }

    final String keyStore = builder.keyStore;
    if (keyStore != null) {
      this.keyStore = new File(keyStore);
      checkArgument(this.keyStore.exists(), "keyStore does not exist [%s]", this.keyStore);
    } else {
      this.keyStore = null;
    }
    this.keyStorePassword = builder.keyStorePassword;
    if (this.keyStorePassword != null) {
      checkArgument(this.keyStore != null,
          "if keyStorePassword is != null, keyStore must be != null");
    }
    this.keyPassword = builder.keyPassword;
    if (this.keyPassword != null) {
      checkArgument(this.keyStore != null, "if keyPassword is != null, keyStore must be != null");
    }

    final String trustStore = builder.trustStore;
    if (trustStore != null) {
      this.trustStore = new File(trustStore);
      checkArgument(this.trustStore.exists(), "trustStore does not exist [%s]", this.trustStore);
    } else {
      this.trustStore = null;
    }
    this.trustStorePassword = builder.trustStorePassword;
    if (this.trustStorePassword != null) {
      checkArgument(this.trustStore != null,
          "if trustStorePassword is != null, trustStore must be != null");
    }
    this.trustSelfSignedCertificates = builder.trustSelfSignedCertificates;
    this.dnsCacheTtl = builder.dnsCacheTtl;
    this.dnsCacheNegativeTtl = builder.dnsCacheNegativeTtl;
    this.authentication = checkNotNull(builder.authentication);
    this.userAgent = builder.userAgent;
    this.writeThroughput = builder.writeThroughput;
    this.readThroughput = builder.readThroughput;
    this.responseBodyConsumers = ImmutableMap.copyOf(builder.responseBodyConsumers);
    this.running = true;
    this.abortedRequestsAtShutdown = new AtomicInteger();
    final ThreadFactory fac = new ThreadFactoryBuilder().setNameFormat("client-%d").build();
    this.executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(fac));
    this.gson = createGson();

    // perform checks on instance fields rather than builder fields
    checkArgument(this.connectTimeout >= 0, "connectTimeout must be >= 0 [%s]",
        this.connectTimeout);
    checkArgument(this.soTimeout >= 0, "soTimeout must be >= 0 [%s]", this.soTimeout);
    checkArgument(this.soLinger >= -1, "soLinger must be >= -1 [%s]", this.soLinger);
    checkArgument(this.soSndBuf >= 0, "soSndBuf must be >= 0 [%s]", this.soSndBuf);
    checkArgument(this.soRcvBuf >= 0, "soRcvBuf must be >= 0 [%s]", this.soRcvBuf);
    checkArgument(this.validateAfterInactivity > 0, "validateAfterInactivity must be > 0 [%s]",
        this.validateAfterInactivity);
    checkArgument(this.maxIdleTime > 0, "maxIdleTime must be > 0 [%s]", this.maxIdleTime);
    checkArgument(this.waitForContinue > 0, "waitForContinue must be > 0 [%s]",
        this.waitForContinue);
    checkArgument(this.retryCount >= 0, "retryCount must be >= 0 [%s]", this.retryCount);
    checkArgument(this.dnsCacheTtl >= -1, "dnsCacheTtl must be >= -1 [%s]", this.dnsCacheTtl);
    checkArgument(this.dnsCacheNegativeTtl >= -1, "dnsCacheNegativeTtl must be >= -1 [%s]",
        this.dnsCacheNegativeTtl);
    checkArgument(this.writeThroughput >= 0, "writeThroughput must be >= 0 [%s]",
        this.writeThroughput);
    checkArgument(this.readThroughput >= 0, "readThroughput must be >= 0 [%s]",
        this.readThroughput);

    Security.setProperty("networkaddress.cache.ttl", String.valueOf(this.dnsCacheTtl));
    Security.setProperty("networkaddress.cache.negative.ttl",
        String.valueOf(this.dnsCacheNegativeTtl));

    this.client = createClient();
  }

  private Gson createGson() {
    return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .setLongSerializationPolicy(LongSerializationPolicy.STRING)
        .registerTypeAdapter(Double.class, new TypeAdapter<Double>() {
          @Override
          public void write(final JsonWriter out, final Double value) throws IOException {
            // round decimals to 2 places
            out.value(new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).doubleValue());
          }

          @Override
          public Double read(final JsonReader in) throws IOException {
            return in.nextDouble();
          }
        }.nullSafe()).create();
  }

  private CloseableHttpClient createClient() {
    final HttpClientBuilder builder = HttpClients.custom();
    if (this.userAgent != null) {
      builder.setUserAgent(this.userAgent);
    }

    // Some authentication implementations add Content-Length or Transfer-Encoding headers as a part
    // of their authentication algorithm; remove them here so that the default interceptors do not
    // throw a ProtocolException
    // @see RequestContent interceptor
    builder.addInterceptorFirst(new HttpRequestInterceptor() {
      @Override
      public void process(final HttpRequest request, final HttpContext context)
          throws HttpException, IOException {
        request.removeHeaders(HTTP.TRANSFER_ENCODING);
        request.removeHeaders(HTTP.CONTENT_LEN);
      }
    });

    return builder.setRequestExecutor(new HttpRequestExecutor(this.waitForContinue))
        .setConnectionManager(createConnectionManager())
        // TODO investigate ConnectionConfig, particularly bufferSize and fragmentSizeHint
        // TODO defaultCredentialsProvider and defaultAuthSchemeRegistry for pre/passive auth?
        .setConnectionReuseStrategy(createConnectionReuseStrategy())
        .setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE).disableConnectionState()
        .disableCookieManagement().disableContentCompression().disableAuthCaching()
        .setRetryHandler(new CustomHttpRequestRetryHandler(this.retryCount, this.requestSentRetry))
        .setRedirectStrategy(new CustomRedirectStrategy())
        .setDefaultRequestConfig(createRequestConfig()).evictExpiredConnections()
        .evictIdleConnections(Long.valueOf(this.maxIdleTime), TimeUnit.MILLISECONDS).build();
  }

  private HttpClientConnectionManager createConnectionManager() {
    final PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager(
        RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", createPlainConnectionSocketFactory())
            .register("https", createSslConnectionSocketFactory()).build(),
        null, null, null, -1, TimeUnit.MILLISECONDS);
    manager.setDefaultSocketConfig(createSocketConfig());
    manager.setMaxTotal(Integer.MAX_VALUE);
    manager.setDefaultMaxPerRoute(Integer.MAX_VALUE);
    manager.setValidateAfterInactivity(this.validateAfterInactivity);
    return manager;
  }

  private ConnectionSocketFactory createPlainConnectionSocketFactory() {
    return PlainConnectionSocketFactory.getSocketFactory();
  }

  private ConnectionSocketFactory createSslConnectionSocketFactory() {
    final SSLSocketFactory sslSocketFactory = createSSLSocketFactory();
    String[] configuredProtocols = null;
    String[] configuredCipherSuites = null;
    if (this.protocols != null) {
      configuredProtocols = Iterables.toArray(this.protocols, String.class);
    }
    if (this.cipherSuites != null) {
      final List<String> supportedCipherSuites =
          ImmutableList.copyOf(sslSocketFactory.getSupportedCipherSuites());
      for (final String cipherSuite : this.cipherSuites) {
        checkArgument(supportedCipherSuites.contains(cipherSuite), "Unsupported cipher suite [%s]",
            cipherSuite);
      }

      configuredCipherSuites = Iterables.toArray(this.cipherSuites, String.class);
    }
    final PublicSuffixMatcher suffixMatcher = PublicSuffixMatcherLoader.getDefault();
    final HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;

    return new SSLConnectionSocketFactory(sslSocketFactory, configuredProtocols,
        configuredCipherSuites, hostnameVerifier);

  }

  private SSLSocketFactory createSSLSocketFactory() {
    final SSLContextBuilder builder = SSLContextBuilder.create();
    configureKeyStores(builder);
    configureTrustStores(builder);
    try {
      return builder.build().getSocketFactory();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void configureKeyStores(final SSLContextBuilder builder) {
    if (this.keyStore != null) {
      try {
        final char[] storePassword = this.keyStorePassword.toCharArray();
        final char[] keyPassword = this.keyPassword.toCharArray();
        builder.loadKeyMaterial(this.keyStore, storePassword, keyPassword);
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void configureTrustStores(final SSLContextBuilder builder) {
    try {
      if (this.trustStore != null) {
        char[] password = null;
        if (this.trustStorePassword != null) {
          password = this.trustStorePassword.toCharArray();
        }
        builder.loadTrustMaterial(this.trustStore, password);
      }
      if (this.trustSelfSignedCertificates) {
        builder.loadTrustMaterial(TrustSelfSignedStrategy.INSTANCE);
      }
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private SocketConfig createSocketConfig() {
    return SocketConfig.custom().setSoTimeout(this.soTimeout).setSoReuseAddress(this.soReuseAddress)
        .setSoLinger(this.soLinger).setSoKeepAlive(this.soKeepAlive).setTcpNoDelay(this.tcpNoDelay)
        .setSndBufSize(this.soSndBuf).setRcvBufSize(this.soRcvBuf).build();
  }

  private ConnectionReuseStrategy createConnectionReuseStrategy() {
    return this.persistentConnections ? DefaultConnectionReuseStrategy.INSTANCE
        : NoConnectionReuseStrategy.INSTANCE;
  }

  // custom retry handler that will retry after any type of exception
  private class CustomHttpRequestRetryHandler extends DefaultHttpRequestRetryHandler {
    public CustomHttpRequestRetryHandler(final int retryCount,
        final boolean requestSentRetryEnabled) {
      super(retryCount, requestSentRetryEnabled,
          Collections.<Class<? extends IOException>>emptyList());
    }
  }

  private RequestConfig createRequestConfig() {
    return RequestConfig.custom().setExpectContinueEnabled(this.expectContinue)
        .setRedirectsEnabled(true).setRelativeRedirectsAllowed(true)
        .setConnectTimeout(this.connectTimeout).setSocketTimeout(this.soTimeout)
        // TODO should this be infinite? length of time allowed to request a connection
        // from the pool
        .setConnectionRequestTimeout(0).build();
  }

  @Override
  public ListenableFuture<Response> execute(final Request request) {
    // FIXME handle case where execute is called after shutdown
    checkNotNull(request);

    final BlockingHttpOperation operation = new BlockingHttpOperation(request);
    final ListenableFuture<Response> baseFuture = this.executorService.submit(operation);

    return new ForwardingListenableFuture.SimpleForwardingListenableFuture<Response>(baseFuture) {
      @Override
      public boolean cancel(final boolean mayInterruptIfRunning) {
        operation.getApacheRequest().abort();
        return delegate().cancel(mayInterruptIfRunning);
      }
    };
  }

  private HttpUriRequest createRequest(final AuthenticatedRequest request) {
    final RequestBuilder builder =
        RequestBuilder.create(request.getMethod().toString()).setUri(request.getUri());

    for (final Entry<String, String> header : request.headers().entrySet()) {
      builder.addHeader(header.getKey(), header.getValue());
    }

    if (DataType.NONE != request.getBody().getDataType()) {
      final AbstractHttpEntity entity = new CustomHttpEntity(request, this.writeThroughput);
      // TODO chunk size for chunked encoding is hardcoded to 2048 bytes. Can only be overridden
      // by implementing a custom connection factory
      entity.setChunked(this.chunkedEncoding);
      builder.setEntity(entity);
    }

    return builder.build();
  }

  @Override
  public ListenableFuture<Boolean> shutdown(final boolean immediate) {
    final SettableFuture<Boolean> future = SettableFuture.create();
    final Thread t = new Thread(getShutdownRunnable(future, immediate));
    t.setName("client-shutdown");
    this.running = false;
    t.start();
    return future;
  }

  private Runnable getShutdownRunnable(final SettableFuture<Boolean> future,
      final boolean immediate) {
    return new Runnable() {
      @Override
      public void run() {
        if (immediate) {
          closeSockets();
        }

        shutdownClient();
        future.set(true);
      }

      private void closeSockets() {
        try {
          _logger.info("Attempting to close client connection pool");
          ApacheClient.this.client.close();
          _logger.info("Client connection pool is closed");
        } catch (final IOException e) {
          _logger.error("Error closing client connection pool", e);
        }
      }

      private void shutdownClient() {
        _logger.info("Issuing client shutdown");
        ApacheClient.this.executorService.shutdown();
        while (!ApacheClient.this.executorService.isTerminated()) {
          awaitShutdown(1, TimeUnit.HOURS);
        }
        _logger.info("Client is shutdown");
        _logger.info("Number of requests aborted at shutdown [{}]",
            ApacheClient.this.abortedRequestsAtShutdown.get());
      }

      private void awaitShutdown(final long timeout, final TimeUnit unit) {
        try {
          _logger.info("Awaiting client executor service termination for {} {}", timeout, unit);
          final boolean result = ApacheClient.this.executorService.awaitTermination(timeout, unit);
          _logger.info("Client executor service termination result [{}]",
              result ? "success" : "failure");
        } catch (final InterruptedException e) {
          _logger.error("Interrupted while waiting for client executor service termination", e);
        }
      }
    };
  }

  private class BlockingHttpOperation implements Callable<Response> {
    private final Request request;
    private AuthenticatedRequest authenticatedRequest;
    private HttpUriRequest apacheRequest;
    private final RequestTimestamps timestamps;
    private final byte[] buf;

    public BlockingHttpOperation(final Request request) {
      this.request = checkNotNull(request);
      this.timestamps = new RequestTimestamps();
      // TODO inject buf size from config
      this.buf = new byte[4096];
    }

    @Override
    public Response call() {
      this.timestamps.startMillis = System.currentTimeMillis();
      this.timestamps.start = System.nanoTime();

      this.authenticatedRequest =
          ApacheClient.this.authentication.authenticate(checkNotNull(this.request));
      this.apacheRequest = ApacheClient.this.createRequest(this.authenticatedRequest);

      final HttpResponse.Builder responseBuilder = new HttpResponse.Builder();
      final String requestId = this.request.getContext().get(Context.X_OG_REQUEST_ID);
      if (requestId != null) {
        responseBuilder.withContext(Context.X_OG_REQUEST_ID, requestId);
      }
      final Response response;
      try {
        _logger.trace("Sending request {}", this.request);
        sendRequest(this.apacheRequest, responseBuilder);
      } catch (final Exception e) {
        if (ApacheClient.this.running) {
          _logger.error("Exception executing request", e);
        } else {
          ApacheClient.this.abortedRequestsAtShutdown.incrementAndGet();
        }
        responseBuilder.withStatusCode(599);
      }
      response = responseBuilder.build();
      _logger.trace("Received response {}", response);
      this.timestamps.finish = System.nanoTime();
      this.timestamps.finishMillis = System.currentTimeMillis();

      // do not log requests with 599 response after client shutdown (known aborted requests)
      if (ApacheClient.this.running || response.getStatusCode() != 599) {
        final RequestLogEntry entry = new RequestLogEntry(this.request, response,
            ApacheClient.this.userAgent, this.timestamps);
        _requestLogger.info(ApacheClient.this.gson.toJson(entry));
      }
      return response;
    }

    private void sendRequest(final HttpUriRequest apacheRequest,
        final HttpResponse.Builder responseBuilder) throws IOException {
      ApacheClient.this.client.execute(apacheRequest, new ResponseHandler<Void>() {
        @Override
        public Void handleResponse(final org.apache.http.HttpResponse response) throws IOException {
          setRequestContentTimestamps(apacheRequest);
          setResponseStatusCode(responseBuilder, response);
          setResponseHeaders(responseBuilder, response);
          receiveResponseContent(responseBuilder, response);
          return null;
        }
      });
    }

    private void setRequestContentTimestamps(final HttpUriRequest apacheRequest) {
      if (apacheRequest instanceof HttpEntityEnclosingRequest) {
        final HttpEntityEnclosingRequest request = (HttpEntityEnclosingRequest) apacheRequest;
        if (request.getEntity() instanceof CustomHttpEntity) {
          final CustomHttpEntity entity = (CustomHttpEntity) request.getEntity();
          this.timestamps.requestContentStart = entity.getRequestContentStart();
          this.timestamps.requestContentFinish = entity.getRequestContentFinish();
        }
      }
    }

    private void setResponseStatusCode(final HttpResponse.Builder responseBuilder,
        final org.apache.http.HttpResponse response) {
      responseBuilder.withStatusCode(response.getStatusLine().getStatusCode());
    }

    private void setResponseHeaders(final HttpResponse.Builder responseBuilder,
        final org.apache.http.HttpResponse response) {
      final HeaderIterator headers = response.headerIterator();
      while (headers.hasNext()) {
        final Header header = headers.nextHeader();
        // TODO header value may be null, is this acceptable?
        responseBuilder.withHeader(header.getName(), header.getValue());
      }
    }

    private void receiveResponseContent(final HttpResponse.Builder responseBuilder,
        final org.apache.http.HttpResponse response) throws IOException {
      final HttpEntity entity = response.getEntity();
      if (entity != null) {
        InputStream entityStream = entity.getContent();
        final long readThroughput = ApacheClient.this.readThroughput;
        if (readThroughput > 0) {
          entityStream = Streams.throttle(entityStream, readThroughput);
        }

        final MonitoringInputStream in = new MonitoringInputStream(entityStream);

        // TODO clean this up, should always try to set response entity to response size;
        // will InstrumentedInputStream help with this?
        final String consumerId =
            this.request.getContext().get(Context.X_OG_RESPONSE_BODY_CONSUMER);
        final ResponseBodyConsumer consumer =
            ApacheClient.this.responseBodyConsumers.get(consumerId);
        this.timestamps.responseContentStart = System.nanoTime();
        if (consumer != null) {
          for (final Map.Entry<String, String> e : consumer
              .consume(response.getStatusLine().getStatusCode(), in).entrySet()) {
            responseBuilder.withContext(e.getKey(), e.getValue());
          }
        } else {
          consumeBytes(responseBuilder, in);
        }
        this.timestamps.responseContentFirstBytes = in.getFirstRead();
        this.timestamps.responseContentFinish = System.nanoTime();
      }
    }

    private void consumeBytes(final HttpResponse.Builder responseBuilder,
        final InputStream responseContent) throws IOException {
      long totalBytes = 0;
      int bytesRead;
      while ((bytesRead = responseContent.read(this.buf)) > 0) {
        totalBytes += bytesRead;
      }

      if (totalBytes > 0) {
        responseBuilder.withBody(Bodies.zeroes(totalBytes));
      }
    }

    public HttpUriRequest getApacheRequest() {
      return this.apacheRequest;
    }
  }

  @Override
  public String toString() {
    return String.format(
        "ApacheClient [%n" + "connectTimeout=%s,%n" + "soTimeout=%s,%n" + "soReuseAddress=%s,%n"
            + "soLinger=%s,%n" + "soKeepAlive=%s,%n" + "tcpNoDelay=%s,%n" + "soSndBuf=%s,%n"
            + "soRcvBuf=%s,%n" + "persistentConnections=%s,%n" + "validateAfterInactivity=%s,%n"
            + "maxIdleTime=%s,%n" + "chunkedEncoding=%s,%n" + "expectContinue=%s,%n"
            + "waitForContinue=%s,%n" + "retryCount=%s,%n" + "requestSentRetry=%s,%n"
            + "protocols=%s,%n" + "cipherSuites=%s,%n" + "keyStore=%s,%n" + "keyStorePassword=%s,%n"
            + "keyPassword=%s,%n" + "trustStore=%s,%n" + "trustStorePassword=%s,%n"
            + "trustSelfSignedCertificates=%s,%n" + "dnsCacheTtl=%s,%n"
            + "dnsCacheNegativeTtl=%s,%n" + "authentication=%s,%n" + "userAgent=%s,%n"
            + "writeThroughput=%s,%n" + "readThroughput=%s,%n" + "responseBodyConsumers=%s%n]",
        this.connectTimeout, this.soTimeout, this.soReuseAddress, this.soLinger, this.soKeepAlive,
        this.tcpNoDelay, this.soSndBuf, this.soRcvBuf, this.persistentConnections,
        this.validateAfterInactivity, this.maxIdleTime, this.chunkedEncoding, this.expectContinue,
        this.waitForContinue, this.retryCount, this.requestSentRetry, this.protocols,
        this.cipherSuites, this.keyStore, this.keyStorePassword, this.keyPassword, this.trustStore,
        this.trustStorePassword, this.trustSelfSignedCertificates, this.dnsCacheTtl,
        this.dnsCacheNegativeTtl, this.authentication, this.userAgent, this.writeThroughput,
        this.readThroughput, this.responseBodyConsumers);
  }

  /**
   * A builder of apache client instances
   */
  public static class Builder {
    private int connectTimeout;
    private int soTimeout;
    private boolean soReuseAddress;
    private int soLinger;
    private boolean soKeepAlive;
    private boolean tcpNoDelay;
    private int soSndBuf;
    private int soRcvBuf;
    private boolean persistentConnections;
    private int validateAfterInactivity;
    private int maxIdleTime;
    private boolean chunkedEncoding;
    private boolean expectContinue;
    private int waitForContinue;
    private int retryCount;
    private boolean requestSentRetry;
    private List<String> protocols;
    private List<String> cipherSuites;
    public String keyStore;
    public String keyStorePassword;
    public String keyPassword;
    private String trustStore;
    private String trustStorePassword;
    private boolean trustSelfSignedCertificates;
    private int dnsCacheTtl;
    private int dnsCacheNegativeTtl;
    private HttpAuth authentication;
    private String userAgent;
    private long writeThroughput;
    private long readThroughput;
    private final Map<String, ResponseBodyConsumer> responseBodyConsumers;

    /**
     * Constructs a new builder
     */
    public Builder() {
      this.connectTimeout = 0;
      this.soTimeout = 0;
      this.soReuseAddress = false;
      this.soLinger = -1;
      this.soKeepAlive = true;
      this.tcpNoDelay = true;
      this.soSndBuf = 0;
      this.soRcvBuf = 0;
      this.persistentConnections = true;
      this.validateAfterInactivity = 10000;
      this.maxIdleTime = 60000;
      this.chunkedEncoding = false;
      this.expectContinue = false;
      this.waitForContinue = 3000;
      this.retryCount = 0;
      this.requestSentRetry = true;
      this.protocols = null;
      this.cipherSuites = null;
      this.keyStore = null;
      this.keyStorePassword = null;
      this.keyPassword = null;
      this.trustStore = null;
      this.trustStorePassword = null;
      this.trustSelfSignedCertificates = false;
      this.dnsCacheTtl = 60;
      this.dnsCacheNegativeTtl = 10;
      this.authentication = new NoneAuth();
      this.writeThroughput = 0;
      this.readThroughput = 0;
      this.responseBodyConsumers = Maps.newHashMap();
    }

    /**
     * Configures the timeout in milliseconds until a connection is established. A timeout of zero
     * is interpreted as an infinite timeout
     * 
     * @param connectTimeout connection open timeout, in milliseconds
     * @return this builder
     */
    public Builder withConnectTimeout(final int connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }

    /**
     * Configures the socket {@code SO_TIMEOUT} timeout in milliseconds, the maximum duration
     * between consecutive packets. A timeout of zero is interpreted as an infinite timeout
     * 
     * @param soTimeout socket timeout, in milliseconds
     * @return this builder
     */
    public Builder withSoTimeout(final int soTimeout) {
      this.soTimeout = soTimeout;
      return this;
    }

    /**
     * Configures the {@code SO_REUSEADDR} socket option
     * 
     * @param soReuseAddress socket reuse flag
     * @return this builder
     */
    public Builder usingSoReuseAddress(final boolean soReuseAddress) {
      this.soReuseAddress = soReuseAddress;
      return this;
    }

    /**
     * Configures {@code SO_LINGER} in <em>seconds</em>. A linger of zero disables linger, and a
     * linger of {@code -1} uses the system default.
     * 
     * @param soLinger linger, in seconds
     * @return this builder
     */
    public Builder withSoLinger(final int soLinger) {
      this.soLinger = soLinger;
      return this;
    }

    /**
     * Configures the {@code SO_KEEPALIVE} socket option
     * 
     * @param soKeepAlive keepalive flag
     * @return this builder
     */
    public Builder usingSoKeepAlive(final boolean soKeepAlive) {
      this.soKeepAlive = soKeepAlive;
      return this;
    }

    /**
     * Configures the {@code TCP_NODELAY} socket option
     * 
     * @param tcpNoDelay tcp no delay flag
     * @return this builder
     */
    public Builder usingTcpNoDelay(final boolean tcpNoDelay) {
      this.tcpNoDelay = tcpNoDelay;
      return this;
    }

    /**
     * Configures {@code SO_SNDBUF}. A buffer of zero uses the system default.
     * 
     * @param soSndBuf, a suggested send buffer size for connections
     * @return this builder
     */
    public Builder withSoSndBuf(final int soSndBuf) {
      this.soSndBuf = soSndBuf;
      return this;
    }

    /**
     * Configures {@code SO_RCVBUF}. A buffer of zero uses the system default.
     * 
     * @param soRcvBuf, a suggested receive buffer size for connections
     * @return this builder
     */
    public Builder withSoRcvBuf(final int soRcvBuf) {
      this.soRcvBuf = soRcvBuf;
      return this;
    }

    /**
     * Configures the use of persistent tcp connections
     * 
     * @param persistentConnections persistent connections flag
     * @return this builder
     */
    public Builder usingPersistentConnections(final boolean persistentConnections) {
      this.persistentConnections = persistentConnections;
      return this;
    }

    /**
     * Configures the maximum amount of time a connection is allowed to remain idle and subsequently
     * be leased without first checking if the connection is stale. Stale connection check costs
     * 20-30ms.
     * 
     * @param validateAfterInactivity maximum idle time, in milliseconds
     * @return this builder
     */
    public Builder withValidateAfterInactivity(final int validateAfterInactivity) {
      this.validateAfterInactivity = validateAfterInactivity;
      return this;
    }

    /**
     * Configures the maximum amount of time a connection is allowed to remain idle and subsequently
     * be leased. Connections that are idle longer than maxIdleTime will be closed.
     * 
     * @param maxIdleTime maximum idle time prior to connection closure.
     * @return this builder
     */
    public Builder withMaxIdleTime(final int maxIdleTime) {
      this.maxIdleTime = maxIdleTime;
      return this;
    }

    /**
     * Configures the use of http chunked encoding for request bodies
     * 
     * @param chunkedEncoding chunked encoding flag
     * @return this builder
     */
    public Builder usingChunkedEncoding(final boolean chunkedEncoding) {
      this.chunkedEncoding = chunkedEncoding;
      return this;
    }

    /**
     * Configures the use of expect: 100-continue flag for PUT and POST requests
     * 
     * @param expectContinue expect continue flag
     * @return this builder
     */
    public Builder usingExpectContinue(final boolean expectContinue) {
      this.expectContinue = expectContinue;
      return this;
    }

    /**
     * Configure the duration to wait for a continue response from the target host after sending a
     * 100-continue message prior to continuing with the request. Duration is in milliseconds
     * 
     * @param waitForContinue wait for continue duration, in milliseconds
     * @return this builder
     */
    public Builder withWaitForContinue(final int waitForContinue) {
      this.waitForContinue = waitForContinue;
      return this;
    }

    /**
     * Configures the number of attempts to retry a request if an exception was thrown during its
     * execution
     * 
     * @param retryCount the number of retry attempts
     * @return this builder
     */
    public Builder withRetryCount(final int retryCount) {
      this.retryCount = retryCount;
      return this;
    }

    /**
     * Configures whether or not to retry a request when it has already been sent to the host
     * 
     * @param requestSentRetry whether or not to retry a request which has already been sent
     * @return this builder
     */
    public Builder usingRequestSentRetry(final boolean requestSentRetry) {
      this.requestSentRetry = requestSentRetry;
      return this;
    }

    /**
     * Configures a list of SSL/TLS protocols to support, in preferred order
     * 
     * @param protocols a list of protocols, in preferred order
     * @return this builder
     */
    public Builder withProtocols(final List<String> protocols) {
      this.protocols = protocols;
      return this;
    }

    /**
     * Configures a list of cipher suites for SSL/TLS requests, in preferred order
     * 
     * @param cipherSuites a list of cipher suites, in preferred order
     * @return this builder
     */
    public Builder withCipherSuites(final List<String> cipherSuites) {
      this.cipherSuites = cipherSuites;
      return this;
    }

    /**
     * Configures a path to a key store to use for storing certificates requests
     * 
     * @param keyStore path to a certificate key store file
     * @return this builder
     */
    public Builder withKeyStore(final String keyStore) {
      this.keyStore = keyStore;
      return this;
    }

    /**
     * Configures a password to use for a configured key store
     * 
     * @param keyStorePassword password for configured key store
     * @return this builder
     */
    public Builder withKeyStorePassword(final String keyStorePassword) {
      this.keyStorePassword = keyStorePassword;
      return this;
    }

    /**
     * Configures a password to use for a certificate in the configured key store
     * 
     * @param keyPassword password for a certificate in the configured key store
     * @return this builder
     */
    public Builder withKeyPassword(final String keyPassword) {
      this.keyPassword = keyPassword;
      return this;
    }

    /**
     * Configures a path to a trust store to use for validating server certificates for SSL/TLS
     * requests
     * 
     * @param trustStore path to a certificate trust store file
     * @return this builder
     */
    public Builder withTrustStore(final String trustStore) {
      this.trustStore = trustStore;
      return this;
    }

    /**
     * Configures a password to use for a configured trust store
     * 
     * @param trustStorePassword password for configured trust store
     * @return this builder
     */
    public Builder withTrustStorePassword(final String trustStorePassword) {
      this.trustStorePassword = trustStorePassword;
      return this;
    }

    /**
     * Configures whether to trust self signed certificates for SSL/TLS requests
     * 
     * @param trustSelfSignedCertificates whether to trust self signed certificates
     * @return this builder
     */
    public Builder usingTrustSelfSignedCertificates(final boolean trustSelfSignedCertificates) {
      this.trustSelfSignedCertificates = trustSelfSignedCertificates;
      return this;
    }

    /**
     * Configures dns cache ttl, in seconds
     * 
     * @param dnsCacheTtl, cache ttl, in seconds
     * @return this builder
     */
    public Builder withDnsCacheTtl(final int dnsCacheTtl) {
      this.dnsCacheTtl = dnsCacheTtl;
      return this;
    }

    /**
     * Configures dns cache ttl for negative responses, in seconds
     * 
     * @param dnsCacheNegativeTtl, cache ttl for negative responses, in seconds
     * @return this builder
     */
    public Builder withDnsCacheNegativeTtl(final int dnsCacheNegativeTtl) {
      this.dnsCacheNegativeTtl = dnsCacheNegativeTtl;
      return this;
    }

    /**
     * Configures the use of authentication for every request
     * 
     * @param authentication the authentication type to use
     * @return this builder
     */
    public Builder withAuthentication(final HttpAuth authentication) {
      this.authentication = authentication;
      return this;
    }

    /**
     * Configures the user-agent request header to send with every request
     * 
     * @param userAgent the user agent string to send
     * @return this builder
     */
    public Builder withUserAgent(final String userAgent) {
      this.userAgent = userAgent;
      return this;
    }

    /**
     * Configures throughput throttling for PUT and POST requests
     * 
     * @param bytesPerSecond maximum throughput
     * @return this builder
     */
    public Builder withWriteThroughput(final long bytesPerSecond) {
      this.writeThroughput = bytesPerSecond;
      return this;
    }

    /**
     * Configures throughput throttling for GET and HEAD requests
     * 
     * @param bytesPerSecond maximum throughput
     * @return this builder
     */
    public Builder withReadThroughput(final long bytesPerSecond) {
      this.readThroughput = bytesPerSecond;
      return this;
    }

    /**
     * Configures a response body consumer to be used to process response bodies for requests
     * configured with a matching consumerId
     * 
     * @param consumerId the consumerId for which the provided consumer should be used
     * @param consumer a response body consumer
     * @return this builder
     * @see Headers#X_OG_RESPONSE_BODY_CONSUMER
     */
    public Builder withResponseBodyConsumer(final String consumerId,
        final ResponseBodyConsumer consumer) {
      this.responseBodyConsumers.put(consumerId, consumer);
      return this;
    }

    /**
     * Constructs a new apache client instance
     * 
     * @return an apache client instance
     * @throws IllegalArgumentException if connectTimeout, soTimeout, writeThroughput, or
     *         readThroughput are negative
     * @throws IllegalArgumentException if soLinger is less than {@code -1}
     * @throws IllegalArgumentException if waitForContinue is negative or zero
     */
    public ApacheClient build() {
      return new ApacheClient(this);
    }
  }
}
