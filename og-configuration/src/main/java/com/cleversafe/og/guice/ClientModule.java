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
// Date: Jun 25, 2014
// ---------------------

package com.cleversafe.og.guice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.cli.json.ClientConfig;
import com.cleversafe.og.client.ApacheClient;
import com.cleversafe.og.client.Client;
import com.cleversafe.og.http.auth.HttpAuth;
import com.cleversafe.og.util.consumer.ByteBufferConsumer;
import com.google.common.base.Function;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class ClientModule extends AbstractModule
{
   private static final Logger _logger = LoggerFactory.getLogger(ClientModule.class);

   public ClientModule()
   {}

   @Override
   protected void configure()
   {}

   @Provides
   @Singleton
   public Client provideClient(
         final ClientConfig clientConfig,
         final HttpAuth auth,
         final Function<String, ByteBufferConsumer> byteBufferConsumers)
   {
      return ApacheClient.custom()
            .withAuth(auth)
            .withConnectTimeout(clientConfig.getConnectTimeout())
            .withSoTimeout(clientConfig.getSoTimeout())
            .usingSoReuseAddress(clientConfig.isSoReuseAddress())
            .withSoLinger(clientConfig.getSoLinger())
            .usingSoKeepAlive(clientConfig.isSoKeepAlive())
            .usingTcpNoDelay(clientConfig.isTcpNoDelay())
            .usingChunkedEncoding(clientConfig.isChunkedEncoding())
            .usingExpectContinue(clientConfig.isExpectContinue())
            .withWaitForContinue(clientConfig.getWaitForContinue())
            .withByteBufferConsumers(byteBufferConsumers)
            .build();
   }
}
