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

package com.cleversafe.oom.client;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.oom.api.ByteBufferConsumer;
import com.cleversafe.oom.http.HttpRequestAccessLogEntry;
import com.cleversafe.oom.http.HttpResponse;
import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.operation.Response;
import com.cleversafe.oom.util.Entities;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
// TODO investigate stale connection check configuration, which may reduce performance
// http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
public class ApacheClient implements Client
{
   private static Logger _logger = LoggerFactory.getLogger(ApacheClient.class);
   private static Logger _requestLogger = LoggerFactory.getLogger("RequestLogger");
   private final CloseableHttpClient client;
   private final Function<String, ByteBufferConsumer> byteBufferConsumers;
   private final ListeningExecutorService executorService;
   private final Gson gson;

   private ApacheClient(
         final int soTimeout,
         final boolean soReuseAddress,
         final int soLinger,
         final boolean soKeepAlive,
         final boolean tcpNoDelay,
         final Function<String, ByteBufferConsumer> byteBufferConsumers)
   {
      checkArgument(soTimeout >= 0, "soTimeout must be >= 0 [%s]", soTimeout);
      checkArgument(soLinger >= -1, "soLinger must be >= -1 [%s]", soLinger);
      this.byteBufferConsumers =
            checkNotNull(byteBufferConsumers, "byteBufferConsumers must not be null");

      this.client = HttpClients.custom()
            // TODO investigate effect of waitForContinue duration in HttpRequestExecutor
            // TODO HTTPS: setHostnameVerifier, setSslcontext, and SetSSLSocketFactory methods
            // TODO investigate ConnectionConfig, particularly bufferSize and fragmentSizeHint
            // TODO setUserAgent to equal "tool name/tool version"
            // TODO defaultCredentialsProvider and defaultAuthSchemeRegistry for pre/passive auth?
            .setMaxConnTotal(Integer.MAX_VALUE)
            .setMaxConnPerRoute(Integer.MAX_VALUE)
            .setDefaultSocketConfig(SocketConfig.custom()
                  .setSoTimeout(soTimeout)
                  .setSoReuseAddress(soReuseAddress)
                  .setSoLinger(soLinger)
                  .setSoKeepAlive(soKeepAlive)
                  .setTcpNoDelay(tcpNoDelay)
                  .build())
            .setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE)
            .setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
            .disableConnectionState()
            .disableCookieManagement()
            .disableContentCompression()
            .disableAuthCaching()
            .disableAutomaticRetries()
            // TODO need to implement a redirectStrategy that will redirect PUT and POST
            .setRedirectStrategy(new LaxRedirectStrategy())
            .build();

      this.executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
      this.gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .create();
   }

   @Override
   public ListenableFuture<Response> execute(final Request request)
   {
      checkNotNull(request, "request must not be null");
      final ByteBufferConsumer consumer =
            this.byteBufferConsumers.apply(request.getCustomRequestKey());
      return this.executorService.submit(new BlockingHttpOperation(this.client, request, consumer,
            this.gson));
   }

   @Override
   public ListenableFuture<Boolean> shutdown(final boolean graceful)
   {
      final SettableFuture<Boolean> future = SettableFuture.create();
      if (!graceful)
      {
         this.executorService.shutdownNow();
         future.set(true);
      }
      // TODO implement graceful shutdown
      return future;
   }

   private static class BlockingHttpOperation implements Callable<Response>
   {
      private final CloseableHttpClient client;
      private final Request request;
      private final ByteBufferConsumer consumer;

      private final byte[] buf;
      private final ByteBuffer byteBuf;
      final Gson gson;
      private static final Joiner joiner = Joiner.on(',').skipNulls();

      public BlockingHttpOperation(
            final CloseableHttpClient client,
            final Request request,
            final ByteBufferConsumer consumer,
            final Gson gson)
      {
         this.client = client;
         this.request = request;
         this.consumer = consumer;
         // TODO inject buf size from config
         this.buf = new byte[4096];
         this.byteBuf = ByteBuffer.allocate(4096);
         this.gson = gson;
      }

      @Override
      public Response call() throws Exception
      {
         final HttpRequestBase baseRequest = createRequest();
         setRequestURI(baseRequest);
         setRequestHeaders(baseRequest);
         setRequestContent(baseRequest);
         final Response response = sendRequest(baseRequest);

         _requestLogger.info(this.gson.toJson(new HttpRequestAccessLogEntry(this.request, response)));
         return response;
      }

      private HttpRequestBase createRequest()
      {

         switch (this.request.getMethod())
         {
            case GET :
               return new HttpGet();
            case HEAD :
               return new HttpHead();
            case POST :
               return new HttpPost();
            case PUT :
               return new HttpPut();
            case DELETE :
               return new HttpDelete();
            default :
               throw new RuntimeException(String.format("Unrecognized Http Method [%s]",
                     this.request.getMethod()));
         }

      }

      private void setRequestURI(final HttpRequestBase request) throws URISyntaxException
      {
         // FIXME Request should use URI instead of URL? avoid URISyntaxException checked exception
         request.setURI(this.request.getURL().toURI());
      }

      private void setRequestHeaders(final HttpRequestBase request)
      {
         final Iterator<Entry<String, String>> headers = this.request.headers();
         while (headers.hasNext())
         {
            final Entry<String, String> header = headers.next();
            request.addHeader(header.getKey(), header.getValue());
         }
      }

      private void setRequestContent(final HttpRequestBase request)
      {
         if (request instanceof HttpEntityEnclosingRequestBase)
         {
            final HttpEntityEnclosingRequestBase r = (HttpEntityEnclosingRequestBase) request;
            r.setEntity(createEntity());
         }
      }

      private HttpEntity createEntity()
      {
         // TODO verify httpclient consumes request entity correctly automatically
         // TODO may need to implement a custom HttpEntity that returns false for isStreaming call,
         // if this makes a performance difference
         final InputStream stream = Entities.createInputStream(this.request.getEntity());
         return new InputStreamEntity(stream, this.request.getEntity().getSize());
      }

      private Response sendRequest(final HttpRequestBase request)
            throws ClientProtocolException, IOException
      {
         return this.client.execute(request, new ResponseHandler<Response>()
         {
            @Override
            public Response handleResponse(final org.apache.http.HttpResponse response)
                  throws ClientProtocolException, IOException
            {
               final HttpResponse.Builder responseBuilder = new HttpResponse.Builder()
                     .withRequestId(BlockingHttpOperation.this.request.getId());
               setResponseStatusCode(responseBuilder, response);
               setResponseHeaders(responseBuilder, response);
               receiveResponseContent(responseBuilder, response);
               return responseBuilder.build();
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

      // TODO handle IllegalStateException via logging, etc
      private void receiveResponseContent(
            final HttpResponse.Builder responseBuilder,
            final org.apache.http.HttpResponse response) throws IllegalStateException, IOException
      {
         final HttpEntity entity = response.getEntity();
         if (entity != null)
            receiveBytes(responseBuilder, entity.getContent());
      }

      private void receiveBytes(
            final HttpResponse.Builder responseBuilder,
            final InputStream responseContent) throws IOException
      {
         int bytesRead;
         while ((bytesRead = responseContent.read(this.buf)) > 0)
         {
            processReceivedBytes(bytesRead);
         }
         final Iterator<Entry<String, String>> it = this.consumer.metaData();
         while (it.hasNext())
         {
            final Entry<String, String> e = it.next();
            responseBuilder.withMetaDataEntry(e.getKey(), e.getValue());
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
      private int soTimeout;
      private boolean soReuseAddress;
      private int soLinger;
      private boolean soKeepAlive;
      private boolean tcpNoDelay;
      private Function<String, ByteBufferConsumer> byteBufferConsumers;

      public Builder()
      {
         // defaults
         this.soTimeout = 0;
         this.soReuseAddress = false;
         this.soLinger = -1;
         this.soKeepAlive = true;
         this.tcpNoDelay = true;
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

      public Builder withByteBufferConsumers(
            final Function<String, ByteBufferConsumer> byteBufferConsumers)
      {
         this.byteBufferConsumers = byteBufferConsumers;
         return this;
      }

      public ApacheClient build()
      {
         return new ApacheClient(this.soTimeout, this.soReuseAddress, this.soLinger,
               this.soKeepAlive, this.tcpNoDelay, this.byteBufferConsumers);
      }
   }
}
