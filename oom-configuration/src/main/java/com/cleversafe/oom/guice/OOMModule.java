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
// Date: Mar 27, 2014
// ---------------------

package com.cleversafe.oom.guice;

import static com.google.common.base.Preconditions.checkNotNull;

import com.cleversafe.oom.api.ByteBufferConsumer;
import com.cleversafe.oom.api.OperationManager;
import com.cleversafe.oom.cli.json.JSONConfiguration;
import com.cleversafe.oom.client.Client;
import com.cleversafe.oom.client.JavaClient;
import com.cleversafe.oom.client.JavaClientConfiguration;
import com.cleversafe.oom.operation.OperationTypeMix;
import com.cleversafe.oom.soh.SOHOperationManager;
import com.cleversafe.oom.util.ByteBufferConsumers;
import com.google.common.base.Function;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class OOMModule extends AbstractModule
{
   private final JSONConfiguration config;

   public OOMModule(final JSONConfiguration config)
   {
      this.config = checkNotNull(config, "config must not be null");
   }

   @Override
   protected void configure()
   {
      bind(SOHOperationManager.class).toProvider(SOHOperationManagerProvider.class).in(
            Singleton.class);
   }

   @Provides
   JSONConfiguration provideJSONConfiguration()
   {
      return this.config;
   }

   @Provides
   @Singleton
   DefaultProducers provideDefaultProducers(final JSONConfiguration config)
   {
      return new DefaultProducers(config);
   }

   @Provides
   @Singleton
   OperationTypeMix provideOperationTypeMix(final JSONConfiguration config)
   {
      // TODO make decision on integer or decimal percentages
      final long write = (long) getDouble(config.getWrite(), 100.0);
      final long read = (long) getDouble(config.getRead(), 0.0);
      final long delete = (long) getDouble(config.getDelete(), 0.0);
      final long floor = (long) getDouble(config.getFloor(), 0.0);
      final long ceiling = (long) getDouble(config.getCeiling(), 100.0);
      return new OperationTypeMix(read, write, delete, floor, ceiling);
   }

   private static double getDouble(final Double candidate, final double defaultDouble)
   {
      return candidate != null ? candidate.doubleValue() : defaultDouble;
   }

   @Provides
   @Singleton
   OperationManager provideOperationManager(
         final JSONConfiguration config,
         final Provider<SOHOperationManager> sohOperationManager)
   {
      return sohOperationManager.get();
   }

   @Provides
   @Singleton
   Client provideClient(final JSONConfiguration config)
   {
      final JavaClientConfiguration clientConfig = new JavaClientConfiguration();
      final Function<String, ByteBufferConsumer> byteBufferConsumers =
            new Function<String, ByteBufferConsumer>()
            {

               @Override
               public ByteBufferConsumer apply(final String input)
               {
                  return ByteBufferConsumers.noOp();
               }

            };
      return new JavaClient(clientConfig, byteBufferConsumers);
   }
}
