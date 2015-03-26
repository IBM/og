/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.guice;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Named;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Client;
import com.cleversafe.og.api.Data;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.client.ApacheClient;
import com.cleversafe.og.guice.annotation.Container;
import com.cleversafe.og.guice.annotation.Delete;
import com.cleversafe.og.guice.annotation.DeleteHeaders;
import com.cleversafe.og.guice.annotation.DeleteHost;
import com.cleversafe.og.guice.annotation.DeleteObjectName;
import com.cleversafe.og.guice.annotation.DeleteWeight;
import com.cleversafe.og.guice.annotation.Host;
import com.cleversafe.og.guice.annotation.Id;
import com.cleversafe.og.guice.annotation.Password;
import com.cleversafe.og.guice.annotation.Read;
import com.cleversafe.og.guice.annotation.ReadHeaders;
import com.cleversafe.og.guice.annotation.ReadHost;
import com.cleversafe.og.guice.annotation.ReadObjectName;
import com.cleversafe.og.guice.annotation.ReadWeight;
import com.cleversafe.og.guice.annotation.UriRoot;
import com.cleversafe.og.guice.annotation.Username;
import com.cleversafe.og.guice.annotation.Write;
import com.cleversafe.og.guice.annotation.WriteHeaders;
import com.cleversafe.og.guice.annotation.WriteHost;
import com.cleversafe.og.guice.annotation.WriteObjectName;
import com.cleversafe.og.guice.annotation.WriteWeight;
import com.cleversafe.og.http.Api;
import com.cleversafe.og.http.BasicAuth;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpAuth;
import com.cleversafe.og.http.HttpUtil;
import com.cleversafe.og.http.ResponseBodyConsumer;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.json.AuthType;
import com.cleversafe.og.json.ClientConfig;
import com.cleversafe.og.json.ConcurrencyConfig;
import com.cleversafe.og.json.ConcurrencyType;
import com.cleversafe.og.json.DistributionType;
import com.cleversafe.og.json.FilesizeConfig;
import com.cleversafe.og.json.HostConfig;
import com.cleversafe.og.json.OGConfig;
import com.cleversafe.og.json.ObjectManagerConfig;
import com.cleversafe.og.json.OperationConfig;
import com.cleversafe.og.json.SelectionType;
import com.cleversafe.og.json.StoppingConditionsConfig;
import com.cleversafe.og.object.AbstractObjectNameConsumer;
import com.cleversafe.og.object.ObjectManager;
import com.cleversafe.og.object.RandomObjectPopulator;
import com.cleversafe.og.object.ReadObjectNameConsumer;
import com.cleversafe.og.object.WriteObjectNameConsumer;
import com.cleversafe.og.s3.AWSAuthV2;
import com.cleversafe.og.scheduling.ConcurrentRequestScheduler;
import com.cleversafe.og.scheduling.RequestRateScheduler;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.soh.SOHWriteResponseBodyConsumer;
import com.cleversafe.og.statistic.Counter;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.supplier.CachingSupplier;
import com.cleversafe.og.supplier.DeleteObjectNameSupplier;
import com.cleversafe.og.supplier.RandomSupplier;
import com.cleversafe.og.supplier.ReadObjectNameSupplier;
import com.cleversafe.og.supplier.RequestSupplier;
import com.cleversafe.og.supplier.Suppliers;
import com.cleversafe.og.supplier.UUIDObjectNameSupplier;
import com.cleversafe.og.test.LoadTest;
import com.cleversafe.og.test.LoadTestSubscriberExceptionHandler;
import com.cleversafe.og.test.condition.CounterCondition;
import com.cleversafe.og.test.condition.RuntimeCondition;
import com.cleversafe.og.test.condition.StatusCodeCondition;
import com.cleversafe.og.test.condition.TestCondition;
import com.cleversafe.og.util.Distribution;
import com.cleversafe.og.util.Distributions;
import com.cleversafe.og.util.Operation;
import com.cleversafe.og.util.SizeUnit;
import com.cleversafe.og.util.Version;
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
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.ProvisionListener;

public class OGModule extends AbstractModule {
  private final OGConfig config;
  private static final double ERR = Math.pow(0.1, 6);
  private static final Range<Double> PERCENTAGE = Range.closed(0.0, 100.0);
  private static final String SOH_PUT_OBJECT = "soh.put_object";
  private final LoadTestSubscriberExceptionHandler handler;
  private final EventBus eventBus;

  public OGModule(final OGConfig config) {
    this.config = checkNotNull(config);
    this.handler = new LoadTestSubscriberExceptionHandler();
    this.eventBus = new EventBus(this.handler);
  }

  @Override
  protected void configure() {
    bind(Scheme.class).toInstance(this.config.getScheme());
    bind(Integer.class).toInstance(this.config.getPort());

    bind(LoadTest.class).in(Singleton.class);
    bind(LoadTestSubscriberExceptionHandler.class).toInstance(this.handler);
    bind(EventBus.class).toInstance(this.eventBus);
    bind(Statistics.class).in(Singleton.class);
    bind(ObjectManager.class).to(RandomObjectPopulator.class).in(Singleton.class);
    bindListener(Matchers.any(), new ProvisionListener() {
      @Override
      public <T> void onProvision(ProvisionInvocation<T> provision) {
        // register every non-null provisioned instance with the global event bus. EventBus treats
        // registration of instances without an @Subscribe method as a no-op and handles duplicate
        // registration such that a given @Subscribe annotated method will only be triggered once
        // per event
        T instance = provision.provision();
        if (instance != null) {
          OGModule.this.eventBus.register(instance);
        }
      }
    });
  }

  @Provides
  @Singleton
  public List<TestCondition> provideTestConditions(final LoadTest test, final EventBus eventBus,
      final Statistics stats, final StoppingConditionsConfig config) {
    checkNotNull(test);
    checkNotNull(eventBus);
    checkNotNull(stats);
    checkNotNull(config);

    final List<TestCondition> conditions = Lists.newArrayList();

    if (config.getOperations() > 0)
      conditions.add(new CounterCondition(Operation.ALL, Counter.OPERATIONS,
          config.getOperations(), test, stats));

    final Map<Integer, Integer> scMap = config.getStatusCodes();
    for (final Entry<Integer, Integer> sc : scMap.entrySet()) {
      if (sc.getValue() > 0)
        conditions.add(new StatusCodeCondition(Operation.ALL, sc.getKey(), sc.getValue(), test,
            stats));
    }

    if (config.getRuntime() > 0)
      conditions.add(new RuntimeCondition(test, config.getRuntime(), config.getRuntimeUnit()));

    for (final TestCondition condition : conditions) {
      eventBus.register(condition);
    }

    return conditions;
  }

  @Provides
  @Singleton
  public Supplier<Request> provideRequestSupplier(@Write final Supplier<Request> write,
      @Read final Supplier<Request> read, @Delete final Supplier<Request> delete,
      @WriteWeight final double writeWeight, @ReadWeight final double readWeight,
      @DeleteWeight final double deleteWeight) {
    checkNotNull(write);
    checkNotNull(read);
    checkNotNull(delete);
    final double sum = readWeight + writeWeight + deleteWeight;
    checkArgument(DoubleMath.fuzzyEquals(sum, 100.0, ERR), "sum of percentages must be 100.0 [%s]",
        sum);

    final RandomSupplier.Builder<Supplier<Request>> wrc = Suppliers.random();
    if (writeWeight > 0.0)
      wrc.withChoice(write, writeWeight);
    if (readWeight > 0.0)
      wrc.withChoice(read, readWeight);
    if (deleteWeight > 0.0)
      wrc.withChoice(delete, deleteWeight);

    return Suppliers.chain(wrc.build());
  }

  @Provides
  @Singleton
  @WriteObjectName
  public CachingSupplier<String> provideWriteObjectName(final Api api) {
    if (Api.SOH == checkNotNull(api))
      return null;
    return new CachingSupplier<String>(new UUIDObjectNameSupplier());
  }

  @Provides
  @Singleton
  @ReadObjectName
  public CachingSupplier<String> provideReadObjectName(final ObjectManager objectManager) {
    return new CachingSupplier<String>(new ReadObjectNameSupplier(objectManager));
  }

  @Provides
  @Singleton
  @DeleteObjectName
  public CachingSupplier<String> provideDeleteObjectName(final ObjectManager objectManager) {
    return new CachingSupplier<String>(new DeleteObjectNameSupplier(objectManager));
  }

  @Provides
  @Singleton
  public List<AbstractObjectNameConsumer> provideObjectNameConsumers(
      final ObjectManager objectManager, final EventBus eventBus) {
    final List<Integer> sc = HttpUtil.SUCCESS_STATUS_CODES;
    final List<AbstractObjectNameConsumer> consumers = Lists.newArrayList();
    consumers.add(new WriteObjectNameConsumer(objectManager, sc));
    consumers.add(new ReadObjectNameConsumer(objectManager, sc));

    for (final AbstractObjectNameConsumer consumer : consumers) {
      eventBus.register(consumer);
    }
    return consumers;
  }

  @Provides
  @Singleton
  @Id
  public Supplier<String> provideIdSupplier() {
    return new Supplier<String>() {
      private final AtomicLong id = new AtomicLong();

      @Override
      public String get() {
        return String.valueOf(this.id.getAndIncrement());
      }
    };
  }

  @Provides
  @Singleton
  @Host
  public Supplier<String> provideHost() {
    return createHost(this.config.getHostSelection(), this.config.getHost());
  }

  @Provides
  @Singleton
  @WriteHost
  public Supplier<String> provideWriteHost(@Host final Supplier<String> host) {
    return provideHost(this.config.getWrite(), host);
  }

  @Provides
  @Singleton
  @ReadHost
  public Supplier<String> provideReadHost(@Host final Supplier<String> host) {
    return provideHost(this.config.getRead(), host);
  }

  @Provides
  @Singleton
  @DeleteHost
  public Supplier<String> provideDeleteHost(@Host final Supplier<String> host) {
    return provideHost(this.config.getDelete(), host);
  }

  private Supplier<String> provideHost(final OperationConfig operationConfig,
      final Supplier<String> testHost) {
    checkNotNull(operationConfig);
    checkNotNull(testHost);

    final List<HostConfig> operationHost = operationConfig.getHost();
    if (operationHost != null && !operationHost.isEmpty())
      return createHost(operationConfig.getHostSelection(), operationHost);

    return testHost;
  }

  private Supplier<String> createHost(final SelectionType hostSelection, final List<HostConfig> host) {
    checkNotNull(hostSelection);
    checkNotNull(host);
    checkArgument(!host.isEmpty(), "must specify at least one host");
    for (final HostConfig h : host) {
      checkNotNull(h);
      checkNotNull(h.getHost());
      checkArgument(h.getHost().length() > 0, "host must not be empty string");
    }

    if (SelectionType.ROUNDROBIN == hostSelection) {
      final List<String> hostList = Lists.newArrayList();
      for (final HostConfig h : host) {
        hostList.add(h.getHost());
      }
      return Suppliers.cycle(hostList);
    }

    final RandomSupplier.Builder<String> wrc = Suppliers.random();
    for (final HostConfig h : host) {
      wrc.withChoice(h.getHost(), h.getWeight());
    }
    return wrc.build();
  }


  @Provides
  public Api provideApi() {
    return checkNotNull(this.config.getApi());
  }

  @Provides
  @Singleton
  @UriRoot
  public String provideUriRoot() {
    final String uriRoot = this.config.getUriRoot();
    if (uriRoot != null) {
      final String root = CharMatcher.is('/').trimFrom(uriRoot);
      if (root.length() > 0)
        return root;
      return null;
    }

    return this.config.getApi().toString().toLowerCase();
  }

  @Provides
  @Singleton
  @Container
  public Supplier<String> provideContainer() {
    final String container = checkNotNull(this.config.getContainer());
    checkArgument(container.length() > 0, "container must not be empty string");
    return Suppliers.of(this.config.getContainer());
  }

  @Provides
  @Singleton
  @Username
  public String provideUsername() {
    final String username = this.config.getAuthentication().getUsername();
    if (username != null) {
      checkArgument(username.length() > 0, "username must not be empty string");
    }
    return username;
  }

  @Provides
  @Singleton
  @Password
  public String providePassword() {
    final String password = this.config.getAuthentication().getPassword();
    if (password != null) {
      checkArgument(password.length() > 0, "password must not be empty string");
    }
    return password;
  }

  @Provides
  @Singleton
  public HttpAuth provideAuthentication(@Username final String username,
      @Password final String password) {
    final AuthType type = checkNotNull(this.config.getAuthentication().getType());

    if (username != null && password != null) {
      if (AuthType.AWSV2 == type)
        return new AWSAuthV2();
      else
        return new BasicAuth();
    } else if (username == null && password == null)
      return null;

    throw new IllegalArgumentException("iff username is not null password must also be not null");
  }

  @Provides
  public StoppingConditionsConfig provideStoppingConditionsConfig() {
    return this.config.getStoppingConditions();
  }

  @Provides
  @Singleton
  @WriteHeaders
  public Map<Supplier<String>, Supplier<String>> provideWriteHeaders(@Id final Supplier<String> id) {
    return provideHeaders(this.config.getWrite().getHeaders(), id);
  }

  @Provides
  @Singleton
  @ReadHeaders
  public Map<Supplier<String>, Supplier<String>> provideReadHeaders(@Id final Supplier<String> id) {
    return provideHeaders(this.config.getRead().getHeaders(), id);
  }

  @Provides
  @Singleton
  @DeleteHeaders
  public Map<Supplier<String>, Supplier<String>> provideDeleteHeaders(@Id final Supplier<String> id) {
    return provideHeaders(this.config.getDelete().getHeaders(), id);
  }

  private Map<Supplier<String>, Supplier<String>> provideHeaders(
      Map<String, String> operationHeaders, @Id final Supplier<String> id) {
    checkNotNull(operationHeaders);

    Map<String, String> headers = Maps.newLinkedHashMap();
    headers.putAll(this.config.getHeaders());
    headers.putAll(operationHeaders);

    final Map<Supplier<String>, Supplier<String>> supplierHeaders = Maps.newLinkedHashMap();

    for (final Entry<String, String> header : headers.entrySet()) {
      supplierHeaders.put(Suppliers.of(header.getKey()), Suppliers.of(header.getValue()));
    }
    supplierHeaders.put(Suppliers.of(com.cleversafe.og.http.Headers.X_OG_REQUEST_ID), id);
    return supplierHeaders;
  }

  @Provides
  @Singleton
  public Supplier<Body> provideBody() {
    final SelectionType filesizeSelection = checkNotNull(this.config.getFilesizeSelection());
    final List<FilesizeConfig> filesizes = checkNotNull(this.config.getFilesize());
    checkArgument(!filesizes.isEmpty(), "filesize must not be empty");

    if (SelectionType.ROUNDROBIN == filesizeSelection) {
      final List<Distribution> distributions = Lists.newArrayList();
      for (final FilesizeConfig f : filesizes) {
        distributions.add(createSizeDistribution(f));
      }
      return createBodySupplier(Suppliers.cycle(distributions));
    }

    final RandomSupplier.Builder<Distribution> wrc = Suppliers.random();
    for (final FilesizeConfig f : filesizes) {
      wrc.withChoice(createSizeDistribution(f), f.getWeight());
    }
    return createBodySupplier(wrc.build());
  }

  private static Distribution createSizeDistribution(final FilesizeConfig filesize) {
    final SizeUnit averageUnit = checkNotNull(filesize.getAverageUnit());
    final SizeUnit spreadUnit = checkNotNull(filesize.getSpreadUnit());
    final DistributionType distribution = checkNotNull(filesize.getDistribution());

    final double average = filesize.getAverage() * averageUnit.toBytes(1);
    final double spread = filesize.getSpread() * spreadUnit.toBytes(1);

    switch (distribution) {
      case NORMAL:
        return Distributions.normal(average, spread);
      case LOGNORMAL:
        return Distributions.lognormal(average, spread);
      case UNIFORM:
        return Distributions.uniform(average, spread);
      default:
        throw new IllegalArgumentException(String.format("unacceptable filesize distribution [%s]",
            distribution));
    }
  }

  private Supplier<Body> createBodySupplier(final Supplier<Distribution> distributionSupplier) {
    final Data data = checkNotNull(this.config.getData());
    checkArgument(Data.NONE != data, "Unacceptable data [%s]", data);

    return new Supplier<Body>() {
      @Override
      public Body get() {
        final long sample = (long) distributionSupplier.get().nextSample();

        switch (data) {
          case ZEROES:
            return Bodies.zeroes(sample);
          default:
            return Bodies.random(sample);
        }
      }
    };
  }

  @Provides
  @Singleton
  @Named("objectfile.location")
  public String provideObjectFileLocation() throws IOException {
    final String path = checkNotNull(this.config.getObjectManager().getObjectFileLocation());
    checkArgument(path.length() > 0, "path must not be empty string");

    final File f = new File(path).getCanonicalFile();
    if (!f.exists()) {
      final boolean success = f.mkdirs();
      if (!success)
        throw new RuntimeException(String.format("failed to create object location directories",
            f.toString()));
    }

    checkArgument(f.isDirectory(), "object location is not a directory [%s]", f.toString());
    return f.toString();
  }

  @Provides
  @Singleton
  @Named("objectfile.name")
  public String provideObjectFileName(@Container final Supplier<String> container, final Api api) {
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
  public double provideWriteWeight(@ReadWeight final double read, @DeleteWeight final double delete) {
    final double write = this.config.getWrite().getWeight();
    checkArgument(PERCENTAGE.contains(write), "write must be in range [0.0, 100.0] [%s]", write);
    if (allEqual(0.0, write, read, delete))
      return 100.0;
    return write;
  }

  @Provides
  @ReadWeight
  public double provideReadWeight() {
    final double read = this.config.getRead().getWeight();
    checkArgument(PERCENTAGE.contains(read), "read must be in range [0.0, 100.0] [%s]", read);
    return read;
  }

  @Provides
  @DeleteWeight
  public double provideDeleteWeight() {
    final double delete = this.config.getDelete().getWeight();
    checkArgument(PERCENTAGE.contains(delete), "delete must be in range [0.0, 100.0] [%s]", delete);
    return delete;
  }

  private boolean allEqual(final double compare, final double... values) {
    for (final double v : values) {
      if (!DoubleMath.fuzzyEquals(v, compare, ERR))
        return false;
    }
    return true;
  }

  @Provides
  @Singleton
  public Scheduler provideScheduler(final EventBus eventBus) {
    checkNotNull(eventBus);
    final ConcurrencyConfig concurrency = checkNotNull(this.config.getConcurrency());
    final ConcurrencyType type = checkNotNull(concurrency.getType());
    final DistributionType distribution = checkNotNull(concurrency.getDistribution());

    if (ConcurrencyType.THREADS == type) {
      final Scheduler scheduler =
          new ConcurrentRequestScheduler((int) Math.round(concurrency.getCount()),
              concurrency.getRampup(), concurrency.getRampupUnit());
      eventBus.register(scheduler);
      return scheduler;
    }

    Distribution count;
    switch (distribution) {
      case POISSON:
        count = Distributions.poisson(concurrency.getCount());
        break;
      case UNIFORM:
        count = Distributions.uniform(concurrency.getCount(), 0.0);
        break;
      default:
        throw new IllegalArgumentException(String.format(
            "unacceptable scheduler distribution [%s]", distribution));
    }
    return new RequestRateScheduler(count, concurrency.getUnit(), concurrency.getRampup(),
        concurrency.getRampupUnit());
  }

  @Provides
  @Singleton
  public Client provideClient(final HttpAuth authentication,
      final Map<String, ResponseBodyConsumer> responseBodyConsumers) {
    ClientConfig clientConfig = this.config.getClient();
    final ApacheClient.Builder b =
        new ApacheClient.Builder().withConnectTimeout(clientConfig.getConnectTimeout())
            .withSoTimeout(clientConfig.getSoTimeout())
            .usingSoReuseAddress(clientConfig.isSoReuseAddress())
            .withSoLinger(clientConfig.getSoLinger())
            .usingSoKeepAlive(clientConfig.isSoKeepAlive())
            .usingTcpNoDelay(clientConfig.isTcpNoDelay())
            .usingPersistentConnections(clientConfig.isPersistentConnections())
            .usingChunkedEncoding(clientConfig.isChunkedEncoding())
            .usingExpectContinue(clientConfig.isExpectContinue())
            .withWaitForContinue(clientConfig.getWaitForContinue())
            .withRetryCount(clientConfig.getRetryCount())
            .usingRequestSentRetry(clientConfig.isRequestSentRetry())
            .withAuthentication(authentication).withUserAgent(Version.displayVersion())
            .withWriteThroughput(clientConfig.getWriteThroughput())
            .withReadThroughput(clientConfig.getReadThroughput());

    for (final Entry<String, ResponseBodyConsumer> consumer : responseBodyConsumers.entrySet()) {
      b.withResponseBodyConsumer(consumer.getKey(), consumer.getValue());
    }

    return b.build();
  }

  @Provides
  @Singleton
  @Write
  public Supplier<Request> provideWrite(final Api api, final Scheme scheme,
      @WriteHost final Supplier<String> host, final Integer port, @UriRoot final String uriRoot,
      @Container final Supplier<String> container,
      @WriteObjectName final CachingSupplier<String> object,
      @WriteHeaders final Map<Supplier<String>, Supplier<String>> headers,
      final Supplier<Body> body, @Username final String username, @Password final String password) {
    checkNotNull(api);
    // SOH needs to use a special response consumer to extract the returned object id
    if (Api.SOH == api)
      headers.put(Suppliers.of(Headers.X_OG_RESPONSE_BODY_CONSUMER), Suppliers.of(SOH_PUT_OBJECT));

    return createRequestSupplier(Method.PUT, scheme, host, port, uriRoot, container, object,
        headers, body, username, password);
  }

  @Provides
  @Singleton
  @Read
  public Supplier<Request> provideRead(final Scheme scheme, @ReadHost final Supplier<String> host,
      final Integer port, @UriRoot final String uriRoot,
      @Container final Supplier<String> container,
      @ReadObjectName final CachingSupplier<String> object,
      @ReadHeaders final Map<Supplier<String>, Supplier<String>> headers,
      @Username final String username, @Password final String password) {
    return createRequestSupplier(Method.GET, scheme, host, port, uriRoot, container, object,
        headers, Suppliers.of(Bodies.none()), username, password);
  }

  @Provides
  @Singleton
  @Delete
  public Supplier<Request> provideDelete(final Scheme scheme,
      @DeleteHost final Supplier<String> host, final Integer port, @UriRoot final String uriRoot,
      @Container final Supplier<String> container,
      @DeleteObjectName final CachingSupplier<String> object,
      @DeleteHeaders final Map<Supplier<String>, Supplier<String>> headers,
      @Username final String username, @Password final String password) {
    return createRequestSupplier(Method.DELETE, scheme, host, port, uriRoot, container, object,
        headers, Suppliers.of(Bodies.none()), username, password);
  }

  private Supplier<Request> createRequestSupplier(final Method method, Scheme scheme,
      final Supplier<String> host, final Integer port, final String uriRoot,
      final Supplier<String> container, final CachingSupplier<String> object,
      final Map<Supplier<String>, Supplier<String>> headers, final Supplier<Body> body,
      final String username, final String password) {
    checkNotNull(method);
    checkNotNull(scheme);
    checkNotNull(host);
    checkNotNull(container);
    checkNotNull(headers);
    checkNotNull(body);

    final RequestSupplier.Builder b =
        new RequestSupplier.Builder(method, host, container).withScheme(scheme)
            .withUriRoot(uriRoot).withObject(object).onPort(port)
            .withCredentials(username, password);

    for (final Entry<Supplier<String>, Supplier<String>> header : headers.entrySet()) {
      b.withHeader(header.getKey(), header.getValue());
    }

    return b.withBody(body).build();
  }

  @Provides
  @Singleton
  public Map<String, ResponseBodyConsumer> provideResponseBodyConsumers() {
    final Map<String, ResponseBodyConsumer> consumers = Maps.newHashMap();
    consumers.put(SOH_PUT_OBJECT, new SOHWriteResponseBodyConsumer());

    return consumers;
  }
}
