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
// Date: Feb 13, 2014
// ---------------------

package com.cleversafe.oom.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.oom.api.ByteBufferConsumer;
import com.cleversafe.oom.api.Consumer;
import com.cleversafe.oom.api.OperationManager;
import com.cleversafe.oom.api.Producer;
import com.cleversafe.oom.cli.json.FileSize;
import com.cleversafe.oom.cli.json.JSONConfiguration;
import com.cleversafe.oom.client.Client;
import com.cleversafe.oom.client.JavaClient;
import com.cleversafe.oom.client.JavaClientConfiguration;
import com.cleversafe.oom.distribution.Distribution;
import com.cleversafe.oom.distribution.DistributionType;
import com.cleversafe.oom.distribution.LogNormalDistribution;
import com.cleversafe.oom.distribution.NormalDistribution;
import com.cleversafe.oom.distribution.UniformDistribution;
import com.cleversafe.oom.http.Scheme;
import com.cleversafe.oom.http.producer.RequestProducer;
import com.cleversafe.oom.http.producer.URLProducer;
import com.cleversafe.oom.operation.Entity;
import com.cleversafe.oom.operation.EntityType;
import com.cleversafe.oom.operation.Method;
import com.cleversafe.oom.operation.OperationType;
import com.cleversafe.oom.operation.OperationTypeMix;
import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.operation.RequestContext;
import com.cleversafe.oom.operation.Response;
import com.cleversafe.oom.scheduling.RequestRateScheduler;
import com.cleversafe.oom.scheduling.Scheduler;
import com.cleversafe.oom.soh.SOHOperationManager;
import com.cleversafe.oom.test.LoadTest;
import com.cleversafe.oom.util.ByteBufferConsumers;
import com.cleversafe.oom.util.Entities;
import com.cleversafe.oom.util.WeightedRandomChoice;
import com.cleversafe.oom.util.producer.Producers;
import com.google.common.base.Function;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

public class OOM
{
   private static Logger _logger = LoggerFactory.getLogger(OOM.class);
   private static String TEST_JSON_RESOURCE_NAME = "test.json";
   public static int ERROR_CONFIGURATION = 1;

   public static void main(final String[] args)
   {
      final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .setPrettyPrinting()
            .create();
      final JSONConfiguration config = createJSONConfiguration(gson);
      verifyJSONConfiguration(config);
      _logger.info(gson.toJson(config));
      final OperationManager operationManager = createOperationManager(config);
      final Client client = createClient(config);
      final ExecutorService executorService = Executors.newCachedThreadPool();
      final LoadTest test = new LoadTest(operationManager, client, executorService);
      test.runTest();
   }

   private static JSONConfiguration createJSONConfiguration(final Gson gson)
   {
      final URL configURL = createConfigURL(TEST_JSON_RESOURCE_NAME);
      final Reader configReader = createConfigReader(configURL);
      JSONConfiguration config = null;
      try
      {
         config = gson.fromJson(configReader, JSONConfiguration.class);
      }
      catch (final Exception e)
      {
         _logger.error("", e);
         System.exit(ERROR_CONFIGURATION);
      }
      return config;
   }

   private static URL createConfigURL(final String resourceName)
   {
      final URL configURL = ClassLoader.getSystemResource(resourceName);
      if (configURL == null)
      {
         _logger.error("Could not find configuration file on classpath [{}]", resourceName);
         System.exit(ERROR_CONFIGURATION);
      }
      return configURL;
   }

   private static Reader createConfigReader(final URL configURL)
   {
      Reader configReader = null;
      try
      {
         configReader = new FileReader(new File(configURL.toURI()));
      }
      catch (final Exception e)
      {
         _logger.error("", e);
         System.exit(ERROR_CONFIGURATION);
      }
      return configReader;
   }

   private static void verifyJSONConfiguration(final JSONConfiguration config)
   {
      try
      {
         checkArgument(config.getHosts().size() > 0, "At least one accesser must be specified");
         checkNotNull(config.getContainer(), "vault must not be null");
      }
      catch (final Exception e)
      {
         _logger.error("", e);
         System.exit(ERROR_CONFIGURATION);
      }
   }

   private static OperationManager createOperationManager(final JSONConfiguration config)
   {
      final DefaultProducers defaults = new DefaultProducers(config);
      return createSOHOperationManager(config, defaults);
   }

   private static OperationManager createSOHOperationManager(
         final JSONConfiguration config,
         final DefaultProducers defaults)
   {

      final OperationTypeMix mix = createOperationTypeMix(config);
      final Map<OperationType, Producer<Request>> producers =
            new HashMap<OperationType, Producer<Request>>();
      producers.put(OperationType.WRITE, createSOHWriteProducer(config, defaults));
      producers.put(OperationType.READ, createSOHReadProducer(config, defaults));
      producers.put(OperationType.DELETE, createSOHDeleteProducer(config, defaults));

      final List<Consumer<Response>> consumers = new ArrayList<Consumer<Response>>();

      // TODO account for scheduler units and threaded vs iops scheduling
      final Distribution sleepDuration =
            new UniformDistribution(config.getConcurrency().getCount(), 0);
      final Scheduler scheduler = new RequestRateScheduler(sleepDuration, TimeUnit.SECONDS);
      return new SOHOperationManager(mix, producers, consumers, scheduler);
   }

   private static Producer<Request> createSOHWriteProducer(
         final JSONConfiguration config,
         final DefaultProducers defaults)
   {
      final List<Producer<String>> parts = new ArrayList<Producer<String>>();
      parts.add(defaults.getContainer());
      final Producer<URL> writeURL =
            new URLProducer(defaults.getScheme(), defaults.getHost(), defaults.getPort(), parts,
                  defaults.getQueryParameters());

      return new RequestProducer(defaults.getId(),
            Producers.of("soh.put_object"),
            Producers.of(Method.PUT),
            writeURL,
            defaults.getHeaders(),
            defaults.getEntity(),
            defaults.getMetaData());
   }

   private static Producer<Request> createSOHReadProducer(
         final JSONConfiguration config,
         final DefaultProducers defaults)
   {
      return null;
   }

   private static Producer<Request> createSOHDeleteProducer(
         final JSONConfiguration config,
         final DefaultProducers defaults)
   {
      return null;
   }

   private static class DefaultProducers
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

   private static OperationTypeMix createOperationTypeMix(final JSONConfiguration config)
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

   private static Client createClient(final JSONConfiguration config)
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
