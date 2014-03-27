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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.cleversafe.oom.api.Producer;
import com.cleversafe.oom.cli.json.FileSize;
import com.cleversafe.oom.cli.json.JSONConfiguration;
import com.cleversafe.oom.distribution.Distribution;
import com.cleversafe.oom.distribution.DistributionType;
import com.cleversafe.oom.distribution.LogNormalDistribution;
import com.cleversafe.oom.distribution.NormalDistribution;
import com.cleversafe.oom.distribution.UniformDistribution;
import com.cleversafe.oom.http.Scheme;
import com.cleversafe.oom.operation.Entity;
import com.cleversafe.oom.operation.EntityType;
import com.cleversafe.oom.operation.RequestContext;
import com.cleversafe.oom.util.Entities;
import com.cleversafe.oom.util.WeightedRandomChoice;
import com.cleversafe.oom.util.producer.Producers;

public class DefaultProducers
{
   private final Producer<Long> id;
   private final Producer<Scheme> scheme;
   private final Producer<String> host;
   private final Producer<Integer> port;
   private final Producer<String> container;
   private final Producer<Map<String, String>> queryParameters;
   private final Producer<Map<String, String>> headers;
   private final Producer<Entity> entity;
   private final Producer<Map<String, String>> metadata;

   public DefaultProducers(final JSONConfiguration config)
   {
      this.id = createIdProducer();
      this.scheme = Producers.of(config.getScheme());
      this.host = createHostProducer(config);
      this.port = Producers.of(config.getPort());
      this.container = Producers.of(config.getContainer());
      final Map<String, String> queryParams = new HashMap<String, String>();
      this.queryParameters = Producers.of(queryParams);
      final Map<String, String> headers = new HashMap<String, String>();
      this.headers = Producers.of(headers);
      this.entity = createEntityProducer(config);
      final Map<String, String> metadata = new HashMap<String, String>();
      this.metadata = Producers.of(metadata);
   }

   private static Producer<Long> createIdProducer()
   {
      return new Producer<Long>()
      {
         private final AtomicLong id = new AtomicLong();

         @Override
         public Long produce(final RequestContext context)
         {
            return this.id.getAndIncrement();
         }
      };
   }

   private static Producer<String> createHostProducer(final JSONConfiguration config)
   {
      final WeightedRandomChoice<String> wrc = new WeightedRandomChoice<String>();
      for (final String accesser : config.getHosts())
      {
         wrc.addChoice(accesser);
      }
      return Producers.of(wrc);
   }

   private static Producer<Entity> createEntityProducer(final JSONConfiguration config)
   {
      final WeightedRandomChoice<Distribution> wrc = new WeightedRandomChoice<Distribution>();
      for (final FileSize f : config.getFilesizes())
      {
         wrc.addChoice(createDistribution(f), f.getWeight());
      }

      return new Producer<Entity>()
      {
         private final WeightedRandomChoice<Distribution> sizes = wrc;

         @Override
         public Entity produce(final RequestContext context)
         {
            return Entities.of(EntityType.RANDOM, (long) this.sizes.nextChoice().nextSample());
         }
      };
   }

   private static Distribution createDistribution(final FileSize filesize)
   {
      final DistributionType type =
            DistributionType.parseDistribution(filesize.getDistribution());
      switch (type)
      {
      // TODO account for size and spread units
         case NORMAL :
            return new NormalDistribution(filesize.getAverage(), filesize.getSpread());
         case LOGNORMAL :
            return new LogNormalDistribution(filesize.getAverage(), filesize.getSpread());
         default :
            return new UniformDistribution(filesize.getAverage(), filesize.getSpread());
      }
   }

   public Producer<Long> getId()
   {
      return this.id;
   }

   public Producer<Scheme> getScheme()
   {
      return this.scheme;
   }

   public Producer<String> getHost()
   {
      return this.host;
   }

   public Producer<Integer> getPort()
   {
      return this.port;
   }

   public Producer<String> getContainer()
   {
      return this.container;
   }

   public Producer<Map<String, String>> getQueryParameters()
   {
      return this.queryParameters;
   }

   public Producer<Map<String, String>> getHeaders()
   {
      return this.headers;
   }

   public Producer<Entity> getEntity()
   {
      return this.entity;
   }

   public Producer<Map<String, String>> getMetaData()
   {
      return this.metadata;
   }
}
