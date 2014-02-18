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
// Date: Jan 30, 2014
// ---------------------

package com.cleversafe.oom.client;

public class JavaClientConfiguration
{
   // http://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html
   private final boolean keepAlive;
   private final int keepAliveMaxConnections;
   private final int maxRedirects;

   private final int connectTimeout;
   private final int readTimeout;
   private final boolean followRedirects;
   private final boolean allowUserInteraction;
   private final boolean useCaches;
   private final String streamingMode;
   private final int chunkLength;
   private final int bufferSize;

   public JavaClientConfiguration()
   {
      this.keepAlive = true;
      this.keepAliveMaxConnections = 5000;
      this.maxRedirects = 20;

      this.connectTimeout = 3600000; // one hour
      this.readTimeout = 3600000; // one hour
      this.followRedirects = true;
      this.allowUserInteraction = false;
      this.useCaches = true;
      this.streamingMode = "chunked";
      this.chunkLength = 1024 * 1024;
      this.bufferSize = 8192;
   }

   public boolean getFollowRedirects()
   {
      return this.followRedirects;
   }

   public boolean getAllowUserInteraction()
   {
      return this.allowUserInteraction;
   }

   public boolean getUseCaches()
   {
      return this.useCaches;
   }

   public String getStreamingMode()
   {
      return this.streamingMode;
   }

   public int getChunkLength()
   {
      return this.chunkLength;
   }

   public int getConnectTimeout()
   {
      return this.connectTimeout;
   }

   public int getReadTimeout()
   {
      return this.readTimeout;
   }

   public boolean getKeepAlive()
   {
      return this.keepAlive;
   }

   public int getKeepAliveMaxConnections()
   {
      return this.keepAliveMaxConnections;
   }

   public int getMaxRedirects()
   {
      return this.maxRedirects;
   }

   public int getBufferSize()
   {
      return this.bufferSize;
   }
}
