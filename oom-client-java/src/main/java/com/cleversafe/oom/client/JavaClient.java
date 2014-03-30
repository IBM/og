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
// Date: Jan 29, 2014
// ---------------------

package com.cleversafe.oom.client;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.oom.api.ByteBufferConsumer;
import com.cleversafe.oom.http.HttpRequestAccessLogEntry;
import com.cleversafe.oom.http.HttpResponse;
import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.operation.Response;
import com.cleversafe.oom.util.Entities;
import com.cleversafe.oom.util.MonitoringInputStream;
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

public class JavaClient implements Client
{
   private static Logger _requestLogger = LoggerFactory.getLogger("RequestLogger");
   private final JavaClientConfiguration config;
   private final Function<String, ByteBufferConsumer> byteBufferConsumers;
   private final ListeningExecutorService executorService;
   private final Gson gson;

   public JavaClient(
         final JavaClientConfiguration config,
         final Function<String, ByteBufferConsumer> byteBufferConsumers)
   {
      this.config = checkNotNull(config, "config must not be null");
      this.byteBufferConsumers =
            checkNotNull(byteBufferConsumers, "byteBufferConsumers must not be null");
      System.setProperty("http.keepalive", String.valueOf(config.getKeepAlive()));
      System.setProperty("http.maxConnections", String.valueOf(config.getKeepAliveMaxConnections()));
      System.setProperty("http.maxRedirects", String.valueOf(config.getMaxRedirects()));
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
      return this.executorService.submit(new BlockingHTTPOperation(this.config, request, consumer,
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

   private static class BlockingHTTPOperation implements Callable<Response>
   {
      private final JavaClientConfiguration config;
      private final Request request;
      private final ByteBufferConsumer consumer;
      private final HttpResponse.Builder responseBuilder;
      private final byte[] buf;
      private final ByteBuffer byteBuf;
      final Gson gson;
      private static final Joiner joiner = Joiner.on(',').skipNulls();

      public BlockingHTTPOperation(
            final JavaClientConfiguration config,
            final Request request,
            final ByteBufferConsumer consumer,
            final Gson gson)
      {
         this.config = config;
         this.request = request;
         this.consumer = consumer;
         this.responseBuilder = new HttpResponse.Builder();
         this.buf = new byte[config.getBufferSize()];
         this.byteBuf = ByteBuffer.allocate(config.getBufferSize());
         this.gson = gson;
      }

      @Override
      public Response call() throws Exception
      {
         this.responseBuilder.withRequestId(this.request.getId());
         final InputStream src = Entities.createInputStream(this.request.getEntity());
         try
         {
            final HttpURLConnection connection = getConnection();
            setRequestMethod(connection);
            setRequestHeaders(connection);
            sendRequestContent(connection, src);
            receiveResponseCode(connection);
            receiveResponseHeaders(connection);
            receiveResponseContent(connection);
         }
         catch (final IOException e)
         {
            // TODO add metadata to response, indicating a network exception event
         }
         finally
         {
            closeStream(src);
         }
         final Response response = this.responseBuilder.build();
         _requestLogger.info(this.gson.toJson(new HttpRequestAccessLogEntry(this.request, response)));
         return response;
      }

      private HttpURLConnection getConnection() throws IOException
      {
         final HttpURLConnection connection =
               (HttpURLConnection) this.request.getURL().openConnection();
         connection.setConnectTimeout(this.config.getConnectTimeout());
         connection.setReadTimeout(this.config.getReadTimeout());
         connection.setInstanceFollowRedirects(this.config.getFollowRedirects());
         connection.setAllowUserInteraction(this.config.getAllowUserInteraction());
         connection.setUseCaches(this.config.getUseCaches());
         connection.setDoInput(true);
         if (this.request.getEntity().getSize() > 0)
            connection.setDoOutput(true);
         return connection;
      }

      private void setRequestMethod(final HttpURLConnection connection) throws ProtocolException
      {
         connection.setRequestMethod(this.request.getMethod().toString());
      }

      private void setRequestHeaders(final HttpURLConnection connection)
      {
         final Iterator<Entry<String, String>> headers = this.request.headers();
         while (headers.hasNext())
         {
            final Entry<String, String> header = headers.next();
            connection.setRequestProperty(header.getKey(), header.getValue());
         }
      }

      private void setContentStreamingMode(final HttpURLConnection connection)
      {
         if (this.config.getStreamingMode().equals("fixed")
               && this.request.getEntity().getSize() > -1)
            connection.setFixedLengthStreamingMode(this.request.getEntity().getSize());
         else
            connection.setChunkedStreamingMode(this.config.getChunkLength());
      }

      private void sendRequestContent(final HttpURLConnection connection, final InputStream src)
            throws IOException
      {
         OutputStream contentDestination = null;
         if (src != null)
         {
            setContentStreamingMode(connection);
            try
            {
               contentDestination = connection.getOutputStream();
               int bytesRead;
               while ((bytesRead = src.read(this.buf)) > 0)
               {
                  contentDestination.write(this.buf, 0, bytesRead);
               }
            }
            finally
            {
               closeStream(contentDestination);
            }
         }
      }

      private void receiveResponseCode(final HttpURLConnection connection) throws IOException
      {
         this.responseBuilder.withStatusCode(connection.getResponseCode());
      }

      private void receiveResponseHeaders(final HttpURLConnection connection)
      {
         for (final Map.Entry<String, List<String>> h : connection.getHeaderFields().entrySet())
         {
            if (h.getKey() == null || h.getValue() == null)
               continue;
            if (h.getValue().size() == 1)
               this.responseBuilder.withHeader(h.getKey(), h.getValue().get(0));
            else if (h.getValue().size() > 1)
               this.responseBuilder.withHeader(h.getKey(), joiner.join(h.getValue()));
         }
      }

      private void receiveResponseContent(final HttpURLConnection connection) throws IOException
      {
         MonitoringInputStream responseContent = null;
         final InputStream responseContentErr = null;
         try
         {
            responseContent = new MonitoringInputStream(connection.getInputStream());
            receiveBytes(responseContent);
         }
         catch (final IOException e)
         {
            // http://docs.oracle.com/javase/1.5.0/docs/guide/net/http-keepalive.html
            consumeErrorStream(connection.getErrorStream());
            throw e;
         }
         finally
         {
            closeStream(responseContent);
            closeStream(responseContentErr);
         }
      }

      private void receiveBytes(final InputStream responseContent) throws IOException
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
            this.responseBuilder.withMetaDataEntry(e.getKey(), e.getValue());
         }
      }

      private void processReceivedBytes(final int bytesRead)
      {
         this.byteBuf.put(this.buf, 0, bytesRead);
         this.byteBuf.flip();
         this.consumer.consume(this.byteBuf);
         this.byteBuf.clear();
      }

      private void consumeErrorStream(final InputStream in) throws IOException
      {
         if (in == null)
            return;

         while (in.read(this.buf) > 0)
         {
            // do nothing
         }
      }

      private void closeStream(final Closeable stream)
      {
         if (stream != null)
         {
            try
            {
               stream.close();
            }
            catch (final IOException e)
            {
            }
         }
      }
   }
}
