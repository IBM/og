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
// Date: Jun 5, 2014
// ---------------------

package com.cleversafe.og.client;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpRequestExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.http.HttpResponse;
import com.cleversafe.og.http.auth.HttpAuth;
import com.cleversafe.og.operation.EntityType;
import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.util.Entities;
import com.cleversafe.og.util.consumer.ByteBufferConsumer;
import com.google.common.base.Function;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

public class ApacheClient implements Client
{
   private static final Logger _logger = LoggerFactory.getLogger(ApacheClient.class);
   private static final Logger _requestLogger = LoggerFactory.getLogger("RequestLogger");
   private final CloseableHttpClient client;
   private final Function<String, ByteBufferConsumer> byteBufferConsumers;
   private final ListeningExecutorService executorService;
   private final Gson gson;
   private final HttpAuth authentication;
   private final boolean chunkedEncoding;

   private ApacheClient(final Builder builder)
   {
      // create local copies to prevent builder from changing values between check and use
      final int connectTimeout = builder.connectTimeout;
      final int soTimeout = builder.soTimeout;
      final int soLinger = builder.soLinger;
      final int waitForContinue = builder.waitForContinue;
      final String userAgent = builder.userAgent;

      checkArgument(connectTimeout >= 0, "connectTimeout must be >= 0 [%s]", connectTimeout);
      checkArgument(soTimeout >= 0, "soTimeout must be >= 0 [%s]", soTimeout);
      checkArgument(soLinger >= -1, "soLinger must be >= -1 [%s]", soLinger);
      checkArgument(waitForContinue > 0, "waitForContinue must be > 0 [%s]", waitForContinue);
      this.byteBufferConsumers = checkNotNull(builder.byteBufferConsumers);

      final HttpClientBuilder clientBuilder = HttpClients.custom();
      if (userAgent != null)
         clientBuilder.setUserAgent(userAgent);

      this.client = clientBuilder
            // TODO HTTPS: setHostnameVerifier, setSslcontext, and SetSSLSocketFactory methods
            // TODO investigate ConnectionConfig, particularly bufferSize and fragmentSizeHint
            // TODO defaultCredentialsProvider and defaultAuthSchemeRegistry for pre/passive auth?
            .setRequestExecutor(new HttpRequestExecutor(waitForContinue))
            .setMaxConnTotal(Integer.MAX_VALUE)
            .setMaxConnPerRoute(Integer.MAX_VALUE)
            .setDefaultSocketConfig(SocketConfig.custom()
                  .setSoTimeout(soTimeout)
                  .setSoReuseAddress(builder.soReuseAddress)
                  .setSoLinger(soLinger)
                  .setSoKeepAlive(builder.soKeepAlive)
                  .setTcpNoDelay(builder.tcpNoDelay)
                  .build())
            .setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE)
            .setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
            .disableConnectionState()
            .disableCookieManagement()
            .disableContentCompression()
            .disableAuthCaching()
            .disableAutomaticRetries()
            .setRedirectStrategy(new CustomRedirectStrategy())
            .setDefaultRequestConfig(RequestConfig.custom()
                  .setExpectContinueEnabled(builder.expectContinue)
                  // TODO investigate performance impact of stale check (30ms reported)
                  .setStaleConnectionCheckEnabled(true)
                  .setRedirectsEnabled(true)
                  .setRelativeRedirectsAllowed(true)
                  .setConnectTimeout(connectTimeout)
                  .setSocketTimeout(soTimeout)
                  // TODO should this be infinite? length of time allowed to request a connection
                  // from the pool
                  .setConnectionRequestTimeout(0)
                  .build())
            .build();

      final ThreadFactory fac = new ThreadFactoryBuilder().setNameFormat("client-%d").build();
      this.executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(fac));
      this.gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .create();

      // optional
      this.authentication = builder.authentication;
      this.chunkedEncoding = builder.chunkedEncoding;
   }

   @Override
   public ListenableFuture<Response> execute(final Request request)
   {
      checkNotNull(request);
      final ByteBufferConsumer consumer =
            this.byteBufferConsumers.apply(request.getMetadata(Metadata.RESPONSE_BODY_PROCESSOR));
      return this.executorService.submit(new BlockingHttpOperation(this.client,
            this.authentication, request,
            consumer, this.gson, this.chunkedEncoding));
   }

   @Override
   public ListenableFuture<Boolean> shutdown(final boolean immediate)
   {
      final SettableFuture<Boolean> future = SettableFuture.create();
      final Thread t = new Thread(getShutdownRunnable(future, immediate));
      t.setName("clientShutdown");
      t.start();
      return future;
   }

   private Runnable getShutdownRunnable(
         final SettableFuture<Boolean> future,
         final boolean immediate)
   {
      return new Runnable()
      {
         @Override
         public void run()
         {
            if (immediate)
               immediateShutdown();
            else
               gracefulShutdown();
            future.set(true);
         }

         private void immediateShutdown()
         {
            _logger.info("Issuing immediate shutdown");
            closeSockets();
            _logger.info("Issuing shutdownNow call to ExecutorService");
            ApacheClient.this.executorService.shutdownNow();
            awaitShutdown(5, TimeUnit.SECONDS);
         }

         private void gracefulShutdown()
         {
            _logger.info("Issuing graceful shutdown");
            _logger.info("Issuing shutdown call to ExecutorService");
            ApacheClient.this.executorService.shutdown();
            awaitShutdown(1, TimeUnit.HOURS);
            if (!ApacheClient.this.executorService.isTerminated())
               immediateShutdown();
         }

         private void closeSockets()
         {
            try
            {
               _logger.info("Attempting to close connection pool");
               ApacheClient.this.client.close();
            }
            catch (final IOException e)
            {
               _logger.error("Error closing client connection pool", e);
            }
         }

         private void awaitShutdown(final long timeout, final TimeUnit unit)
         {
            try
            {
               _logger.info("Awaiting ExecutorService termination for {} {}", timeout, unit);
               final boolean result =
                     ApacheClient.this.executorService.awaitTermination(timeout, unit);
               _logger.info("ExecutorService termination result [{}]", result
                     ? "success"
                     : "failure");
            }
            catch (final InterruptedException e)
            {
               _logger.error("Interrupted while waiting for executor service termination", e);
            }
         }
      };
   }
   private static class BlockingHttpOperation implements Callable<Response>
   {
      private final CloseableHttpClient client;
      private final HttpAuth auth;
      private final Request request;
      private final ByteBufferConsumer consumer;

      private final byte[] buf;
      private final ByteBuffer byteBuf;
      final Gson gson;
      private final boolean chunkedEncoding;

      public BlockingHttpOperation(
            final CloseableHttpClient client,
            final HttpAuth auth,
            final Request request,
            final ByteBufferConsumer consumer,
            final Gson gson,
            final boolean chunkedEncoding)
      {
         this.client = client;
         this.auth = auth;
         this.request = request;
         this.consumer = consumer;
         // TODO inject buf size from config
         this.buf = new byte[4096];
         this.byteBuf = ByteBuffer.allocate(4096);
         this.gson = gson;
         this.chunkedEncoding = chunkedEncoding;
      }

      @Override
      public Response call()
      {
         final long timestampStart = System.currentTimeMillis();
         final RequestBuilder requestBuilder =
               RequestBuilder.create(this.request.getMethod().toString());
         setRequestURI(requestBuilder);
         setRequestHeaders(requestBuilder);
         setRequestContent(requestBuilder);
         final HttpResponse.Builder responseBuilder = new HttpResponse.Builder();
         final String requestId = this.request.getMetadata(Metadata.REQUEST_ID);
         if (requestId != null)
            responseBuilder.withMetadata(Metadata.REQUEST_ID, requestId);
         final Response response;
         try
         {
            sendRequest(requestBuilder.build(), responseBuilder);
         }
         catch (final Exception e)
         {
            _logger.error("Exception executing request", e);
            responseBuilder.withStatusCode(499).withMetadata(Metadata.ABORTED, "");
         }
         response = responseBuilder.build();
         final long timestampFinish = System.currentTimeMillis();

         _requestLogger.info(this.gson.toJson(new RequestLogEntry(this.request, response,
               timestampStart, timestampFinish)));

         return response;
      }

      private void setRequestURI(final RequestBuilder requestBuilder)
      {
         requestBuilder.setUri(this.request.getUri());
      }

      private void setRequestHeaders(final RequestBuilder requestBuilder)
      {
         setAuthHeader(requestBuilder);
         final Iterator<Entry<String, String>> headers = this.request.headers();
         while (headers.hasNext())
         {
            final Entry<String, String> header = headers.next();
            requestBuilder.addHeader(header.getKey(), header.getValue());
         }
      }

      private void setAuthHeader(final RequestBuilder requestBuilder)
      {
         if (this.auth != null)
         {
            final String authValue = this.auth.nextAuthorizationHeader(this.request);
            requestBuilder.addHeader("Authorization", authValue);
         }

      }

      private void setRequestContent(final RequestBuilder requestBuilder)
      {
         if (EntityType.NONE != this.request.getEntity().getType())
            requestBuilder.setEntity(createEntity());
      }

      private HttpEntity createEntity()
      {
         // TODO verify httpclient consumes request entity correctly automatically
         // TODO may need to implement a custom HttpEntity that returns false for isStreaming call,
         // if this makes a performance difference
         final InputStream stream = Entities.createInputStream(this.request.getEntity());
         final InputStreamEntity entity =
               new InputStreamEntity(stream, this.request.getEntity().getSize());
         // TODO chunk size for chunked encoding is hardcoded to 2048 bytes. Can only be overridden
         // by implementing a custom connection factory
         entity.setChunked(this.chunkedEncoding);
         return entity;
      }

      private void sendRequest(
            final HttpUriRequest apacheRequest,
            final HttpResponse.Builder responseBuilder) throws IOException
      {
         this.client.execute(apacheRequest, new ResponseHandler<Void>()
         {
            @Override
            public Void handleResponse(final org.apache.http.HttpResponse response)
                  throws IOException
            {
               setResponseStatusCode(responseBuilder, response);
               setResponseHeaders(responseBuilder, response);
               receiveResponseContent(responseBuilder, response);
               return null;
            }
         });
      }

      private void setResponseStatusCode(
            final HttpResponse.Builder responseBuilder,
            final org.apache.http.HttpResponse response)
      {
         responseBuilder.withStatusCode(response.getStatusLine().getStatusCode());
      }

      private void setResponseHeaders(
            final HttpResponse.Builder responseBuilder,
            final org.apache.http.HttpResponse response)
      {
         final HeaderIterator headers = response.headerIterator();
         while (headers.hasNext())
         {
            final Header header = headers.nextHeader();
            // TODO header value may be null, is this acceptable?
            responseBuilder.withHeader(header.getName(), header.getValue());
         }
      }

      private void receiveResponseContent(
            final HttpResponse.Builder responseBuilder,
            final org.apache.http.HttpResponse response) throws IOException
      {
         final HttpEntity entity = response.getEntity();
         if (entity != null)
            receiveBytes(responseBuilder, entity.getContent());
      }

      private void receiveBytes(
            final HttpResponse.Builder responseBuilder,
            final InputStream responseContent) throws IOException
      {
         long totalBytes = 0;
         int bytesRead;
         while ((bytesRead = responseContent.read(this.buf)) > 0)
         {
            totalBytes += bytesRead;
            processReceivedBytes(bytesRead);
         }

         if (totalBytes > 0)
            responseBuilder.withEntity(Entities.of(EntityType.ZEROES, totalBytes));

         final Iterator<Entry<String, String>> it = this.consumer.metadata();
         while (it.hasNext())
         {
            final Entry<String, String> e = it.next();
            responseBuilder.withMetadata(e.getKey(), e.getValue());
         }
      }

      private void processReceivedBytes(final int bytesRead)
      {
         this.byteBuf.put(this.buf, 0, bytesRead);
         this.byteBuf.flip();
         this.consumer.consume(this.byteBuf);
         this.byteBuf.clear();
      }
   }

   public static class Builder
   {
      private int connectTimeout;
      private int soTimeout;
      private boolean soReuseAddress;
      private int soLinger;
      private boolean soKeepAlive;
      private boolean tcpNoDelay;
      private boolean chunkedEncoding;
      private boolean expectContinue;
      private int waitForContinue;
      private HttpAuth authentication;
      private final Function<String, ByteBufferConsumer> byteBufferConsumers;
      private String userAgent;

      public Builder(final Function<String, ByteBufferConsumer> byteBufferConsumers)
      {
         this.waitForContinue = 3000;
         this.byteBufferConsumers = byteBufferConsumers;
      }

      public Builder withConnectTimeout(final int connectTimeout)
      {
         this.connectTimeout = connectTimeout;
         return this;
      }

      public Builder withSoTimeout(final int soTimeout)
      {
         this.soTimeout = soTimeout;
         return this;
      }

      public Builder usingSoReuseAddress(final boolean soReuseAddress)
      {
         this.soReuseAddress = soReuseAddress;
         return this;
      }

      public Builder withSoLinger(final int soLinger)
      {
         this.soLinger = soLinger;
         return this;
      }

      public Builder usingSoKeepAlive(final boolean soKeepAlive)
      {
         this.soKeepAlive = soKeepAlive;
         return this;
      }

      public Builder usingTcpNoDelay(final boolean tcpNoDelay)
      {
         this.tcpNoDelay = tcpNoDelay;
         return this;
      }

      public Builder usingChunkedEncoding(final boolean chunkedEncoding)
      {
         this.chunkedEncoding = chunkedEncoding;
         return this;
      }

      public Builder usingExpectContinue(final boolean expectContinue)
      {
         this.expectContinue = expectContinue;
         return this;
      }

      public Builder withWaitForContinue(final int waitForContinue)
      {
         this.waitForContinue = waitForContinue;
         return this;
      }

      public Builder withAuthentication(final HttpAuth authentication)
      {
         this.authentication = authentication;
         return this;
      }

      public Builder withUserAgent(final String userAgent)
      {
         this.userAgent = userAgent;
         return this;
      }

      public ApacheClient build()
      {
         return new ApacheClient(this);
      }
   }
}
