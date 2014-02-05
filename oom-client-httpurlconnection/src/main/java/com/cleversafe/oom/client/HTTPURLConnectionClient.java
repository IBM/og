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

import com.cleversafe.oom.operation.HTTPOperation;
import com.cleversafe.oom.operation.OperationState;
import com.cleversafe.oom.operation.OperationType;
import com.cleversafe.oom.statistic.Statistics;
import com.cleversafe.oom.util.MonitoringInputStream;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class HTTPURLConnectionClient implements Client<HTTPOperation>
{
   private final HTTPURLConnectionClientConfiguration config;
   private final Statistics stats;
   private final ListeningExecutorService executorService;

   public HTTPURLConnectionClient(
         final HTTPURLConnectionClientConfiguration config,
         final Statistics stats)
   {
      this.config = checkNotNull(config, "config must not be null");
      this.stats = checkNotNull(stats, "stats must not be null");
      System.setProperty("http.keepalive", String.valueOf(config.getKeepAlive()));
      System.setProperty("http.maxConnections", String.valueOf(config.getKeepAliveMaxConnections()));
      System.setProperty("http.maxRedirects", String.valueOf(config.getMaxRedirects()));
      this.executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
   }

   @Override
   public ListenableFuture<HTTPOperation> execute(final HTTPOperation operation)
   {
      checkNotNull(operation, "operation must not be null");
      return this.executorService.submit(new BlockingHTTPOperation(this.config, this.stats,
            operation));
   }

   @Override
   public void shutdownNow()
   {
      this.executorService.shutdownNow();
   }

   private static class BlockingHTTPOperation implements Callable<HTTPOperation>
   {
      private final HTTPURLConnectionClientConfiguration config;
      private final Statistics stats;
      private final HTTPOperation operation;
      private final byte[] buf;
      private final ByteBuffer byteBuf;
      private static final Joiner joiner = Joiner.on(',').skipNulls();

      public BlockingHTTPOperation(
            final HTTPURLConnectionClientConfiguration config,
            final Statistics stats,
            final HTTPOperation operation)
      {
         this.config = config;
         this.stats = stats;
         this.operation = operation;
         this.buf = new byte[config.getBufferSize()];
         this.byteBuf = ByteBuffer.allocate(config.getBufferSize());
      }

      @Override
      public HTTPOperation call() throws Exception
      {
         final long beginTimestamp = this.stats.beginOperation(this.operation.getOperationType());
         final long endTimestamp;
         try
         {
            final HttpURLConnection connection = getConnection();
            setRequestMethod(connection);
            setRequestHeaders(connection);
            sendRequestContent(connection);
            receiveResponseCode(connection);
            receiveResponseHeaders(connection);
            receiveResponseContent(connection);
         }
         catch (final IOException e)
         {
            this.operation.setOperationState(OperationState.ABORTED);
         }
         finally
         {
            closeStream(this.operation.getRequestEntity().getInputStream());
         }

         switch (this.operation.getOperationState())
         {
            case ABORTED :
               endTimestamp =
                     this.stats.abortOperation(this.operation.getOperationType(), beginTimestamp);

               break;
            case FAILED :
               endTimestamp =
                     this.stats.failOperation(this.operation.getOperationType(), beginTimestamp);
               break;
            default :
               this.operation.setOperationState(OperationState.COMPLETED);
               endTimestamp =
                     this.stats.completeOperation(this.operation.getOperationType(), beginTimestamp);
               break;
         }
         this.operation.setDuration(endTimestamp - beginTimestamp);
         return this.operation;
      }

      private HttpURLConnection getConnection() throws IOException
      {
         final HttpURLConnection connection =
               (HttpURLConnection) this.operation.getURL().openConnection();
         connection.setConnectTimeout(this.config.getConnectTimeout());
         connection.setReadTimeout(this.config.getReadTimeout());
         connection.setInstanceFollowRedirects(this.config.getFollowRedirects());
         connection.setAllowUserInteraction(this.config.getAllowUserInteraction());
         connection.setUseCaches(this.config.getUseCaches());
         connection.setDoInput(true);
         if (this.operation.getRequestEntity().getInputStream() != null)
            connection.setDoOutput(true);
         return connection;
      }

      private void setRequestMethod(final HttpURLConnection connection) throws ProtocolException
      {
         connection.setRequestMethod(this.operation.getMethod().toString());
      }

      private void setRequestHeaders(final HttpURLConnection connection)
      {
         final Iterator<Entry<String, String>> headers = this.operation.requestHeaderIterator();
         while (headers.hasNext())
         {
            final Entry<String, String> header = headers.next();
            connection.setRequestProperty(header.getKey(), header.getValue());
         }
      }

      private void setContentStreamingMode(final HttpURLConnection connection)
      {
         if (this.config.getStreamingMode().equals("fixed")
               && this.operation.getRequestEntity().getSize() > -1)
            connection.setFixedLengthStreamingMode(this.operation.getRequestEntity().getSize());
         else
            connection.setChunkedStreamingMode(this.config.getChunkLength());
      }

      private void sendRequestContent(final HttpURLConnection connection) throws IOException
      {
         final InputStream requestContent = this.operation.getRequestEntity().getInputStream();
         OutputStream contentDestination = null;
         if (requestContent != null)
         {
            setContentStreamingMode(connection);
            try
            {
               contentDestination = connection.getOutputStream();
               int bytesRead;
               while ((bytesRead = requestContent.read(this.buf)) > 0)
               {
                  contentDestination.write(this.buf, 0, bytesRead);
                  this.operation.setBytesSent(bytesRead + this.operation.getBytesSent());
                  if (this.operation.getOperationType() == OperationType.WRITE)
                     this.stats.bytes(OperationType.WRITE, bytesRead);
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
         this.operation.setResponseCode(connection.getResponseCode());
      }

      private void receiveResponseHeaders(final HttpURLConnection connection)
      {
         for (final Map.Entry<String, List<String>> h : connection.getHeaderFields().entrySet())
         {
            if (h.getKey() == null || h.getValue() == null)
               continue;
            if (h.getValue().size() == 1)
               this.operation.setResponseHeader(h.getKey(), h.getValue().get(0));
            else if (h.getValue().size() > 1)
               this.operation.setResponseHeader(h.getKey(), joiner.join(h.getValue()));
         }
      }

      private void receiveResponseContent(final HttpURLConnection connection) throws IOException
      {
         MonitoringInputStream responseContent = null;
         final InputStream responseContentErr = null;
         try
         {
            responseContent = new MonitoringInputStream(connection.getInputStream());
            final int bytesRead = receiveFirstBytes(responseContent);
            if (bytesRead > 0)
            {
               receiveBytes(responseContent);
            }
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

      private int receiveFirstBytes(final MonitoringInputStream responseContent) throws IOException
      {
         final int bytesRead = responseContent.read(this.buf);
         if (bytesRead > 0)
         {
            this.operation.setTTFB(responseContent.getTTFB());
            this.stats.ttfb(this.operation.getOperationType(), responseContent.getTTFB());
            processReceivedBytes(bytesRead);
         }
         return bytesRead;
      }

      private void receiveBytes(final InputStream responseContent) throws IOException
      {
         int bytesRead;
         while ((bytesRead = responseContent.read(this.buf)) > 0)
         {
            processReceivedBytes(bytesRead);
         }
      }

      private void processReceivedBytes(final int bytesRead)
      {
         this.byteBuf.put(this.buf, 0, bytesRead);
         this.byteBuf.flip();
         this.operation.onReceivedContent(this.byteBuf);
         this.byteBuf.clear();

         if (this.operation.getOperationType() == OperationType.READ)
            this.stats.bytes(OperationType.READ, bytesRead);
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
