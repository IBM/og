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
// Date: Jun 15, 2014
// ---------------------

package com.cleversafe.oom.cli.json;

public class ClientConfig
{
   private final int connectTimeout;
   private final int soTimeout;
   private final boolean soReuseAddress;
   private final int soLinger;
   private final boolean soKeepAlive;
   private final boolean tcpNoDelay;
   private final boolean chunkedEncoding;
   private final boolean expectContinue;

   public ClientConfig()
   {
      this.connectTimeout = 0;
      this.soTimeout = 0;
      this.soReuseAddress = false;
      this.soLinger = -1;
      this.soKeepAlive = true;
      this.tcpNoDelay = true;
      this.chunkedEncoding = false;
      this.expectContinue = false;
   }

   /**
    * @return the connectTimeout
    */
   public int getConnectTimeout()
   {
      return this.connectTimeout;
   }

   /**
    * @return the soTimeout
    */
   public int getSoTimeout()
   {
      return this.soTimeout;
   }

   /**
    * @return the soReuseAddress
    */
   public boolean isSoReuseAddress()
   {
      return this.soReuseAddress;
   }

   /**
    * @return the soLinger
    */
   public int getSoLinger()
   {
      return this.soLinger;
   }

   /**
    * @return the soKeepAlive
    */
   public boolean isSoKeepAlive()
   {
      return this.soKeepAlive;
   }

   /**
    * @return the tcpNoDelay
    */
   public boolean isTcpNoDelay()
   {
      return this.tcpNoDelay;
   }

   /**
    * @return the chunkedEncoding
    */
   public boolean isChunkedEncoding()
   {
      return this.chunkedEncoding;
   }

   /**
    * @return the expectContinue
    */
   public boolean isExpectContinue()
   {
      return this.expectContinue;
   }
}
