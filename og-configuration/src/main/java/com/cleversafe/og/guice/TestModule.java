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

package com.cleversafe.og.guice;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import com.cleversafe.og.guice.annotation.DeleteHeaders;
import com.cleversafe.og.guice.annotation.DeleteHost;
import com.cleversafe.og.guice.annotation.DeleteWeight;
import com.cleversafe.og.guice.annotation.ReadHeaders;
import com.cleversafe.og.guice.annotation.ReadHost;
import com.cleversafe.og.guice.annotation.ReadWeight;
import com.cleversafe.og.guice.annotation.TestContainer;
import com.cleversafe.og.guice.annotation.TestEntity;
import com.cleversafe.og.guice.annotation.TestHeaders;
import com.cleversafe.og.guice.annotation.TestHost;
import com.cleversafe.og.guice.annotation.TestId;
import com.cleversafe.og.guice.annotation.TestObjectFileLocation;
import com.cleversafe.og.guice.annotation.TestObjectFileName;
import com.cleversafe.og.guice.annotation.TestPassword;
import com.cleversafe.og.guice.annotation.TestPort;
import com.cleversafe.og.guice.annotation.TestQueryParams;
import com.cleversafe.og.guice.annotation.TestScheme;
import com.cleversafe.og.guice.annotation.TestUriRoot;
import com.cleversafe.og.guice.annotation.TestUsername;
import com.cleversafe.og.guice.annotation.WriteHeaders;
import com.cleversafe.og.guice.annotation.WriteHost;
import com.cleversafe.og.guice.annotation.WriteWeight;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.http.auth.BasicAuth;
import com.cleversafe.og.http.auth.HttpAuth;
import com.cleversafe.og.http.util.Api;
import com.cleversafe.og.json.ConcurrencyConfig;
import com.cleversafe.og.json.FilesizeConfig;
import com.cleversafe.og.json.HostConfig;
import com.cleversafe.og.json.ObjectManagerConfig;
import com.cleversafe.og.json.OperationConfig;
import com.cleversafe.og.json.StoppingConditionsConfig;
import com.cleversafe.og.json.TestConfig;
import com.cleversafe.og.json.enums.AuthType;
import com.cleversafe.og.json.enums.CollectionAlgorithmType;
import com.cleversafe.og.json.enums.ConcurrencyType;
import com.cleversafe.og.json.enums.DistributionType;
import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.operation.EntityType;
import com.cleversafe.og.s3.auth.AWSAuthV2;
import com.cleversafe.og.scheduling.ConcurrentRequestScheduler;
import com.cleversafe.og.scheduling.RequestRateScheduler;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.util.Entities;
import com.cleversafe.og.util.SizeUnit;
import com.cleversafe.og.util.distribution.Distribution;
import com.cleversafe.og.util.distribution.LogNormalDistribution;
import com.cleversafe.og.util.distribution.NormalDistribution;
import com.cleversafe.og.util.distribution.PoissonDistribution;
import com.cleversafe.og.util.distribution.UniformDistribution;
import com.cleversafe.og.util.producer.Producer;
import com.cleversafe.og.util.producer.Producers;
import com.cleversafe.og.util.producer.RandomChoiceProducer;
import com.google.common.base.CharMatcher;
import com.google.common.collect.Range;
import com.google.common.eventbus.EventBus;
import com.google.common.math.DoubleMath;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class TestModule extends AbstractModule
{
   private final TestConfig config;
   private static final double ERR = Math.pow(0.1, 6);
   private static final Range<Double> PERCENTAGE = Range.closed(0.0, 100.0);

   public TestModule(final TestConfig config)
   {
      this.config = checkNotNull(config);
   }

   @Override
   protected void configure()
   {}

   @Provides
   @Singleton
   @TestId
   public Producer<String> testIdProducer()
   {
      return new Producer<String>()
      {
         private final AtomicLong id = new AtomicLong();

         @Override
         public String produce()
         {
            return String.valueOf(this.id.getAndIncrement());
         }
      };
   }

   @Provides
   @Singleton
   @TestScheme
   public Producer<Scheme> testScheme()
   {
      return Producers.of(this.config.getScheme());
   }

   @Provides
   @Singleton
   @TestHost
   public Producer<String> testHost()
   {
      return createHost(this.config.getHostSelection(), this.config.getHost());
   }

   @Provides
   @Singleton
   @WriteHost
   public Producer<String> testWriteHost(@TestHost final Producer<String> host)
   {
      return provideHost(this.config.getWrite(), host);
   }

   @Provides
   @Singleton
   @ReadHost
   public Producer<String> testReadHost(@TestHost final Producer<String> host)
   {
      return provideHost(this.config.getRead(), host);
   }

   @Provides
   @Singleton
   @DeleteHost
   public Producer<String> testDeleteHost(@TestHost final Producer<String> host)
   {
      return provideHost(this.config.getDelete(), host);
   }

   private Producer<String> provideHost(
         final OperationConfig operationConfig,
         final Producer<String> testHost)
   {
      checkNotNull(operationConfig);
      checkNotNull(testHost);

      final List<HostConfig> operationHost = operationConfig.getHost();
      if (operationHost != null && !operationHost.isEmpty())
         return createHost(operationConfig.getHostAlgorithm(), operationHost);

      return testHost;
   }

   private Producer<String> createHost(
         final CollectionAlgorithmType hostSelection,
         final List<HostConfig> host)
   {
      checkNotNull(hostSelection);
      checkNotNull(host);
      checkArgument(!host.isEmpty(), "host must not be empty string");
      for (final HostConfig h : host)
      {
         checkNotNull(h);
         checkNotNull(h.getHost());
         checkArgument(h.getHost().length() > 0, "host length must be > 0");
      }

      if (CollectionAlgorithmType.ROUNDROBIN == hostSelection)
      {
         final List<String> hostList = new ArrayList<String>();
         for (final HostConfig h : host)
         {
            hostList.add(h.getHost());
         }
         return Producers.cycle(hostList);
      }

      final RandomChoiceProducer.Builder<String> wrc =
            new RandomChoiceProducer.Builder<String>();
      for (final HostConfig h : host)
      {
         wrc.withChoice(h.getHost(), h.getWeight());
      }
      return wrc.build();
   }

   @Provides
   @Singleton
   @TestPort
   public Producer<Integer> testPort()
   {
      if (this.config.getPort() != null)
         return Producers.of(this.config.getPort());
      return null;
   }

   @Provides
   public Api testApi()
   {
      return checkNotNull(this.config.getApi());
   }

   @Provides
   @Singleton
   @TestUriRoot
   public Producer<String> testUriRoot()
   {
      final String uriRoot = this.config.getUriRoot();
      if (uriRoot != null)
      {
         final String root = CharMatcher.is('/').trimFrom(uriRoot);
         if (root.length() > 0)
            return Producers.of(root);
         return null;
      }

      return Producers.of(this.config.getApi().toString().toLowerCase());
   }

   @Provides
   @Singleton
   @TestContainer
   public Producer<String> testContainer()
   {
      final String container = checkNotNull(this.config.getContainer());
      checkArgument(container.length() > 0, "container must not be empty string");
      return Producers.of(this.config.getContainer());
   }

   @Provides
   @Singleton
   @TestQueryParams
   public Map<String, String> testQueryParams()
   {
      return new HashMap<String, String>();
   }

   @Provides
   @Singleton
   @TestUsername
   public Producer<String> testUsername()
   {
      final String username = this.config.getAuthentication().getUsername();
      if (username != null)
      {
         checkArgument(username.length() > 0, "username must not be empty string");
         return Producers.of(username);
      }
      return null;
   }

   @Provides
   @Singleton
   @TestPassword
   public Producer<String> testPassword()
   {
      final String password = this.config.getAuthentication().getPassword();
      if (password != null)
      {
         checkArgument(password.length() > 0, "password must not be empty string");
         return Producers.of(password);
      }
      return null;
   }

   @Provides
   @Singleton
   public HttpAuth testAuthentication(
         @TestUsername final Producer<String> username,
         @TestPassword final Producer<String> password)
   {
      final AuthType type = checkNotNull(this.config.getAuthentication().getType());

      if (username != null && password != null)
      {
         if (AuthType.AWSV2 == type)
            return new AWSAuthV2();
         else
            return new BasicAuth();
      }
      else if (username == null && password == null)
         return null;
      throw new IllegalArgumentException("If username is not null password must also be not null");
   }

   @Provides
   public StoppingConditionsConfig testStoppingConditionsConfig()
   {
      return this.config.getStoppingConditions();
   }

   @Provides
   @Singleton
   @TestHeaders
   public Map<Producer<String>, Producer<String>> testHeaders()
   {
      return createHeaders(this.config.getHeaders());
   }

   @Provides
   @Singleton
   @WriteHeaders
   public Map<Producer<String>, Producer<String>> testWriteHeaders(
         final Map<Producer<String>, Producer<String>> headers)
   {
      return provideHeaders(this.config.getWrite(), headers);
   }

   @Provides
   @Singleton
   @ReadHeaders
   public Map<Producer<String>, Producer<String>> testReadHeaders(
         final Map<Producer<String>, Producer<String>> headers)
   {
      return provideHeaders(this.config.getRead(), headers);
   }

   @Provides
   @Singleton
   @DeleteHeaders
   public Map<Producer<String>, Producer<String>> testDeleteHeaders(
         final Map<Producer<String>, Producer<String>> headers)
   {
      return provideHeaders(this.config.getDelete(), headers);
   }

   private Map<Producer<String>, Producer<String>> provideHeaders(
         final OperationConfig operationConfig,
         final Map<Producer<String>, Producer<String>> testHeaders)
   {
      checkNotNull(operationConfig);
      checkNotNull(testHeaders);

      final Map<String, String> operationHeaders = operationConfig.getHeaders();
      if (operationHeaders != null && !operationHeaders.isEmpty())
         return createHeaders(operationHeaders);

      return testHeaders;
   }

   private Map<Producer<String>, Producer<String>> createHeaders(final Map<String, String> headers)
   {
      final Map<Producer<String>, Producer<String>> h =
            new LinkedHashMap<Producer<String>, Producer<String>>();
      for (final Entry<String, String> header : checkNotNull(headers.entrySet()))
      {
         h.put(Producers.of(header.getKey()), Producers.of(header.getValue()));
      }
      return h;
   }

   @Provides
   @Singleton
   @TestEntity
   public Producer<Entity> testEntity()
   {
      final CollectionAlgorithmType filesizeSelection =
            checkNotNull(this.config.getFilesizeSelection());
      final List<FilesizeConfig> filesizes = checkNotNull(this.config.getFilesize());
      checkArgument(!filesizes.isEmpty(), "filesize must not be empty");

      if (CollectionAlgorithmType.ROUNDROBIN == filesizeSelection)
      {
         final List<Distribution> distributions = new ArrayList<Distribution>();
         for (final FilesizeConfig f : filesizes)
         {
            distributions.add(createSizeDistribution(f));
         }
         return createEntityProducer(Producers.cycle(distributions));
      }

      final RandomChoiceProducer.Builder<Distribution> wrc =
            new RandomChoiceProducer.Builder<Distribution>();
      for (final FilesizeConfig f : filesizes)
      {
         wrc.withChoice(createSizeDistribution(f), f.getWeight());
      }
      return createEntityProducer(wrc.build());
   }

   private static Distribution createSizeDistribution(final FilesizeConfig filesize)
   {
      final SizeUnit averageUnit = checkNotNull(filesize.getAverageUnit());
      final SizeUnit spreadUnit = checkNotNull(filesize.getSpreadUnit());
      final DistributionType distribution = checkNotNull(filesize.getDistribution());

      final double average = filesize.getAverage() * averageUnit.toBytes(1);
      final double spread = filesize.getSpread() * spreadUnit.toBytes(1);

      switch (distribution)
      {
         case NORMAL :
            return new NormalDistribution(average, spread);
         case LOGNORMAL :
            return new LogNormalDistribution(average, spread);
         case UNIFORM :
            return new UniformDistribution(average, spread);
         default :
            throw new IllegalArgumentException(String.format(
                  "Unacceptable filesize distribution [%s]", distribution));
      }
   }

   private Producer<Entity> createEntityProducer(final Producer<Distribution> distributionProducer)
   {
      final EntityType source = checkNotNull(this.config.getSource());
      checkArgument(EntityType.NONE != source, "Unacceptable source [%s]", source);

      return new Producer<Entity>()
      {
         @Override
         public Entity produce()
         {
            final long sample = (long) distributionProducer.produce().nextSample();

            switch (source)
            {
               case ZEROES :
                  return Entities.zeroes(sample);
               default :
                  return Entities.random(sample);
            }
         }
      };
   }

   @Provides
   @Singleton
   @TestObjectFileLocation
   public String testObjectFileLocation() throws IOException
   {
      final String path = checkNotNull(this.config.getObjectManager().getObjectFileLocation());
      checkArgument(path.length() > 0, "path must not be empty string");

      final File f = new File(path).getCanonicalFile();
      if (!f.exists())
      {
         final boolean success = f.mkdirs();
         if (!success)
            throw new RuntimeException(String.format(
                  "Failed to create object location directories", f.toString()));
      }

      checkArgument(f.isDirectory(), "object location is not a directory [%s]", f.toString());
      return f.toString();
   }

   @Provides
   @Singleton
   @TestObjectFileName
   public String testObjectFileName(@TestContainer final Producer<String> container, final Api api)
   {
      checkNotNull(container);
      checkNotNull(api);
      final ObjectManagerConfig objectManagerConfig = checkNotNull(this.config.getObjectManager());
      final String objectFileName = objectManagerConfig.getObjectFileName();

      if (objectFileName != null && !objectFileName.isEmpty())
         return objectFileName;
      // FIXME this naming scheme will break unless @TestContainer is a constant producer
      return container.produce() + "-" + api.toString().toLowerCase();
   }

   @Provides
   @WriteWeight
   public double testWriteWeight()
   {
      final double write = this.config.getWrite().getWeight();
      checkArgument(PERCENTAGE.contains(write), "write must be in range [0.0, 100.0] [%s]", write);
      final double read = this.config.getRead().getWeight();
      final double delete = this.config.getDelete().getWeight();
      if (allEqual(0.0, write, read, delete))
         return 100.0;
      return write;
   }

   @Provides
   @ReadWeight
   public double testReadWeight()
   {
      final double read = this.config.getRead().getWeight();
      checkArgument(PERCENTAGE.contains(read), "read must be in range [0.0, 100.0] [%s]", read);
      return read;
   }

   @Provides
   @DeleteWeight
   public double testDeleteWeight()
   {
      final double delete = this.config.getDelete().getWeight();
      checkArgument(PERCENTAGE.contains(delete), "delete must be in range [0.0, 100.0] [%s]",
            delete);
      return delete;
   }

   private boolean allEqual(final double compare, final double... values)
   {
      for (final double v : values)
      {
         if (!DoubleMath.fuzzyEquals(v, compare, ERR))
            return false;
      }
      return true;
   }

   @Provides
   @Singleton
   public Scheduler testScheduler(final EventBus eventBus)
   {
      checkNotNull(eventBus);
      final ConcurrencyConfig concurrency = checkNotNull(this.config.getConcurrency());
      final ConcurrencyType type = checkNotNull(concurrency.getType());
      final DistributionType distribution = checkNotNull(concurrency.getDistribution());

      if (ConcurrencyType.THREADS == type)
      {
         final Scheduler scheduler =
               new ConcurrentRequestScheduler((int) Math.round(concurrency.getCount()));
         eventBus.register(scheduler);
         return scheduler;
      }

      Distribution count;
      switch (distribution)
      {
         case POISSON :
            count = new PoissonDistribution(concurrency.getCount());
            break;
         case UNIFORM :
            count = new UniformDistribution(concurrency.getCount(), 0.0);
            break;
         default :
            throw new IllegalArgumentException(String.format(
                  "Unacceptable scheduler distribution [%s]", distribution));
      }
      return new RequestRateScheduler(count, concurrency.getUnit());
   }
}
