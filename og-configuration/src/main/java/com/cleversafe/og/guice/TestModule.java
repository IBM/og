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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Data;
import com.cleversafe.og.guice.annotation.Container;
import com.cleversafe.og.guice.annotation.DeleteHeaders;
import com.cleversafe.og.guice.annotation.DeleteHost;
import com.cleversafe.og.guice.annotation.DeleteWeight;
import com.cleversafe.og.guice.annotation.Headers;
import com.cleversafe.og.guice.annotation.Host;
import com.cleversafe.og.guice.annotation.Id;
import com.cleversafe.og.guice.annotation.ObjectFileLocation;
import com.cleversafe.og.guice.annotation.ObjectFileName;
import com.cleversafe.og.guice.annotation.Password;
import com.cleversafe.og.guice.annotation.ReadHeaders;
import com.cleversafe.og.guice.annotation.ReadHost;
import com.cleversafe.og.guice.annotation.ReadWeight;
import com.cleversafe.og.guice.annotation.UriRoot;
import com.cleversafe.og.guice.annotation.Username;
import com.cleversafe.og.guice.annotation.WriteHeaders;
import com.cleversafe.og.guice.annotation.WriteHost;
import com.cleversafe.og.guice.annotation.WriteWeight;
import com.cleversafe.og.http.Api;
import com.cleversafe.og.http.BasicAuth;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.HttpAuth;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.json.AuthType;
import com.cleversafe.og.json.ConcurrencyConfig;
import com.cleversafe.og.json.ConcurrencyType;
import com.cleversafe.og.json.DistributionType;
import com.cleversafe.og.json.FilesizeConfig;
import com.cleversafe.og.json.HostConfig;
import com.cleversafe.og.json.ObjectManagerConfig;
import com.cleversafe.og.json.OperationConfig;
import com.cleversafe.og.json.SelectionType;
import com.cleversafe.og.json.StoppingConditionsConfig;
import com.cleversafe.og.json.TestConfig;
import com.cleversafe.og.s3.AWSAuthV2;
import com.cleversafe.og.scheduling.ConcurrentRequestScheduler;
import com.cleversafe.og.scheduling.RequestRateScheduler;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.supplier.RandomSupplier;
import com.cleversafe.og.supplier.Suppliers;
import com.cleversafe.og.util.SizeUnit;
import com.cleversafe.og.util.distribution.Distribution;
import com.cleversafe.og.util.distribution.LogNormalDistribution;
import com.cleversafe.og.util.distribution.NormalDistribution;
import com.cleversafe.og.util.distribution.PoissonDistribution;
import com.cleversafe.og.util.distribution.UniformDistribution;
import com.google.common.base.CharMatcher;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
   @Id
   public Supplier<String> provideIdSupplier()
   {
      return new Supplier<String>()
      {
         private final AtomicLong id = new AtomicLong();

         @Override
         public String get()
         {
            return String.valueOf(this.id.getAndIncrement());
         }
      };
   }

   @Provides
   @Singleton
   public Supplier<Scheme> provideScheme()
   {
      return Suppliers.of(this.config.getScheme());
   }

   @Provides
   @Singleton
   @Host
   public Supplier<String> provideHost()
   {
      return createHost(this.config.getHostSelection(), this.config.getHost());
   }

   @Provides
   @Singleton
   @WriteHost
   public Supplier<String> provideWriteHost(@Host final Supplier<String> host)
   {
      return provideHost(this.config.getWrite(), host);
   }

   @Provides
   @Singleton
   @ReadHost
   public Supplier<String> provideReadHost(@Host final Supplier<String> host)
   {
      return provideHost(this.config.getRead(), host);
   }

   @Provides
   @Singleton
   @DeleteHost
   public Supplier<String> provideDeleteHost(@Host final Supplier<String> host)
   {
      return provideHost(this.config.getDelete(), host);
   }

   private Supplier<String> provideHost(
         final OperationConfig operationConfig,
         final Supplier<String> testHost)
   {
      checkNotNull(operationConfig);
      checkNotNull(testHost);

      final List<HostConfig> operationHost = operationConfig.getHost();
      if (operationHost != null && !operationHost.isEmpty())
         return createHost(operationConfig.getHostSelection(), operationHost);

      return testHost;
   }

   private Supplier<String> createHost(
         final SelectionType hostSelection,
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

      if (SelectionType.ROUNDROBIN == hostSelection)
      {
         final List<String> hostList = Lists.newArrayList();
         for (final HostConfig h : host)
         {
            hostList.add(h.getHost());
         }
         return Suppliers.cycle(hostList);
      }

      final RandomSupplier.Builder<String> wrc = Suppliers.random();
      for (final HostConfig h : host)
      {
         wrc.withChoice(h.getHost(), h.getWeight());
      }
      return wrc.build();
   }

   @Provides
   @Singleton
   public Supplier<Integer> providePort()
   {
      if (this.config.getPort() != null)
         return Suppliers.of(this.config.getPort());
      return null;
   }

   @Provides
   public Api provideApi()
   {
      return checkNotNull(this.config.getApi());
   }

   @Provides
   @Singleton
   @UriRoot
   public Supplier<String> provideUriRoot()
   {
      final String uriRoot = this.config.getUriRoot();
      if (uriRoot != null)
      {
         final String root = CharMatcher.is('/').trimFrom(uriRoot);
         if (root.length() > 0)
            return Suppliers.of(root);
         return null;
      }

      return Suppliers.of(this.config.getApi().toString().toLowerCase());
   }

   @Provides
   @Singleton
   @Container
   public Supplier<String> provideContainer()
   {
      final String container = checkNotNull(this.config.getContainer());
      checkArgument(container.length() > 0, "container must not be empty string");
      return Suppliers.of(this.config.getContainer());
   }

   @Provides
   @Singleton
   @Username
   public Supplier<String> provideUsername()
   {
      final String username = this.config.getAuthentication().getUsername();
      if (username != null)
      {
         checkArgument(username.length() > 0, "username must not be empty string");
         return Suppliers.of(username);
      }
      return null;
   }

   @Provides
   @Singleton
   @Password
   public Supplier<String> providePassword()
   {
      final String password = this.config.getAuthentication().getPassword();
      if (password != null)
      {
         checkArgument(password.length() > 0, "password must not be empty string");
         return Suppliers.of(password);
      }
      return null;
   }

   @Provides
   @Singleton
   public HttpAuth provideAuthentication(
         @Username final Supplier<String> username,
         @Password final Supplier<String> password)
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
   public StoppingConditionsConfig provideStoppingConditionsConfig()
   {
      return this.config.getStoppingConditions();
   }

   @Provides
   @Singleton
   @Headers
   public Map<Supplier<String>, Supplier<String>> provideHeaders(@Id final Supplier<String> id)
   {
      return createHeaders(id, this.config.getHeaders());
   }

   @Provides
   @Singleton
   @WriteHeaders
   public Map<Supplier<String>, Supplier<String>> provideWriteHeaders(
         @Id final Supplier<String> id,
         @Headers final Map<Supplier<String>, Supplier<String>> headers)
   {
      return provideHeaders(this.config.getWrite(), id, headers);
   }

   @Provides
   @Singleton
   @ReadHeaders
   public Map<Supplier<String>, Supplier<String>> provideReadHeaders(
         @Id final Supplier<String> id,
         @Headers final Map<Supplier<String>, Supplier<String>> headers)
   {
      return provideHeaders(this.config.getRead(), id, headers);
   }

   @Provides
   @Singleton
   @DeleteHeaders
   public Map<Supplier<String>, Supplier<String>> provideDeleteHeaders(
         @Id final Supplier<String> id,
         @Headers final Map<Supplier<String>, Supplier<String>> headers)
   {
      return provideHeaders(this.config.getDelete(), id, headers);
   }

   private Map<Supplier<String>, Supplier<String>> provideHeaders(
         final OperationConfig operationConfig,
         @Id final Supplier<String> id,
         final Map<Supplier<String>, Supplier<String>> testHeaders)
   {
      checkNotNull(operationConfig);
      checkNotNull(testHeaders);

      final Map<String, String> operationHeaders = operationConfig.getHeaders();
      if (operationHeaders != null && !operationHeaders.isEmpty())
         return createHeaders(id, operationHeaders);

      return testHeaders;
   }

   private Map<Supplier<String>, Supplier<String>> createHeaders(
         final Supplier<String> id,
         final Map<String, String> headers)
   {
      final Map<Supplier<String>, Supplier<String>> h = Maps.newLinkedHashMap();
      for (final Entry<String, String> header : checkNotNull(headers.entrySet()))
      {
         h.put(Suppliers.of(header.getKey()), Suppliers.of(header.getValue()));
      }
      h.put(Suppliers.of(com.cleversafe.og.http.Headers.X_OG_REQUEST_ID), id);
      return h;
   }

   @Provides
   @Singleton
   public Supplier<Body> provideBody()
   {
      final SelectionType filesizeSelection = checkNotNull(this.config.getFilesizeSelection());
      final List<FilesizeConfig> filesizes = checkNotNull(this.config.getFilesize());
      checkArgument(!filesizes.isEmpty(), "filesize must not be empty");

      if (SelectionType.ROUNDROBIN == filesizeSelection)
      {
         final List<Distribution> distributions = Lists.newArrayList();
         for (final FilesizeConfig f : filesizes)
         {
            distributions.add(createSizeDistribution(f));
         }
         return createBodySupplier(Suppliers.cycle(distributions));
      }

      final RandomSupplier.Builder<Distribution> wrc = Suppliers.random();
      for (final FilesizeConfig f : filesizes)
      {
         wrc.withChoice(createSizeDistribution(f), f.getWeight());
      }
      return createBodySupplier(wrc.build());
   }

   private static Distribution createSizeDistribution(final FilesizeConfig filesize)
   {
      final SizeUnit averageUnit = checkNotNull(filesize.getAverageUnit());
      final SizeUnit spreadUnit = checkNotNull(filesize.getSpreadUnit());
      final DistributionType distribution = checkNotNull(filesize.getDistribution());

      final double average = filesize.getAverage() * averageUnit.toBytes(1);
      final double spread = filesize.getSpread() * spreadUnit.toBytes(1);

      final Random random = new Random();
      switch (distribution)
      {
         case NORMAL :
            return new NormalDistribution(average, spread, random);
         case LOGNORMAL :
            return new LogNormalDistribution(average, spread, random);
         case UNIFORM :
            return new UniformDistribution(average, spread, random);
         default :
            throw new IllegalArgumentException(String.format(
                  "Unacceptable filesize distribution [%s]", distribution));
      }
   }

   private Supplier<Body> createBodySupplier(final Supplier<Distribution> distributionSupplier)
   {
      final Data data = checkNotNull(this.config.getData());
      checkArgument(Data.NONE != data, "Unacceptable data [%s]", data);

      return new Supplier<Body>()
      {
         @Override
         public Body get()
         {
            final long sample = (long) distributionSupplier.get().nextSample();

            switch (data)
            {
               case ZEROES :
                  return Bodies.zeroes(sample);
               default :
                  return Bodies.random(sample);
            }
         }
      };
   }

   @Provides
   @Singleton
   @ObjectFileLocation
   public String provideObjectFileLocation() throws IOException
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
   @ObjectFileName
   public String provideObjectFileName(@Container final Supplier<String> container, final Api api)
   {
      checkNotNull(container);
      checkNotNull(api);
      final ObjectManagerConfig objectManagerConfig = checkNotNull(this.config.getObjectManager());
      final String objectFileName = objectManagerConfig.getObjectFileName();

      if (objectFileName != null && !objectFileName.isEmpty())
         return objectFileName;
      // FIXME this naming scheme will break unless @TestContainer is a constant supplier
      return container.get() + "-" + api.toString().toLowerCase();
   }

   @Provides
   @WriteWeight
   public double provideWriteWeight(@ReadWeight final double read, @DeleteWeight final double delete)
   {
      final double write = this.config.getWrite().getWeight();
      checkArgument(PERCENTAGE.contains(write), "write must be in range [0.0, 100.0] [%s]", write);
      if (allEqual(0.0, write, read, delete))
         return 100.0;
      return write;
   }

   @Provides
   @ReadWeight
   public double provideReadWeight()
   {
      final double read = this.config.getRead().getWeight();
      checkArgument(PERCENTAGE.contains(read), "read must be in range [0.0, 100.0] [%s]", read);
      return read;
   }

   @Provides
   @DeleteWeight
   public double provideDeleteWeight()
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
   public Scheduler provideScheduler(final EventBus eventBus)
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
      final Random random = new Random();
      switch (distribution)
      {
         case POISSON :
            count = new PoissonDistribution(concurrency.getCount(), random);
            break;
         case UNIFORM :
            count = new UniformDistribution(concurrency.getCount(), 0.0, random);
            break;
         default :
            throw new IllegalArgumentException(String.format(
                  "Unacceptable scheduler distribution [%s]", distribution));
      }
      return new RequestRateScheduler(count, concurrency.getUnit(), concurrency.getRamp(),
            concurrency.getRampUnit());
   }
}
