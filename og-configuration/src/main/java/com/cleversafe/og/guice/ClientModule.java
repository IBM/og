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
// Date: Jul 18, 2014
// ---------------------

package com.cleversafe.og.guice;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.cleversafe.og.client.ApacheClient;
import com.cleversafe.og.client.Client;
import com.cleversafe.og.http.auth.HttpAuth;
import com.cleversafe.og.json.ClientConfig;
import com.cleversafe.og.util.ResponseBodyConsumer;
import com.cleversafe.og.util.Version;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class ClientModule extends AbstractModule
{
   private final ClientConfig config;

   public ClientModule(final ClientConfig config)
   {
      this.config = checkNotNull(config);
   }

   @Override
   protected void configure()
   {}

   @Provides
   @Singleton
   public Client provideClient(
         final HttpAuth authentication,
         final Map<String, ResponseBodyConsumer> responseBodyConsumers)
   {
      return new ApacheClient.Builder(responseBodyConsumers)
            .withConnectTimeout(this.config.getConnectTimeout())
            .withSoTimeout(this.config.getSoTimeout())
            .usingSoReuseAddress(this.config.isSoReuseAddress())
            .withSoLinger(this.config.getSoLinger())
            .usingSoKeepAlive(this.config.isSoKeepAlive())
            .usingTcpNoDelay(this.config.isTcpNoDelay())
            .usingChunkedEncoding(this.config.isChunkedEncoding())
            .usingExpectContinue(this.config.isExpectContinue())
            .withWaitForContinue(this.config.getWaitForContinue())
            .withAuthentication(authentication)
            .withUserAgent(Version.displayVersion())
            .withWriteThroughput(this.config.getWriteThroughput())
            .withReadThroughput(this.config.getReadThroughput())
            .build();
   }
}
