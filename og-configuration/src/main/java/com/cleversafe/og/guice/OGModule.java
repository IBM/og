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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Named;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Client;
import com.cleversafe.og.api.Data;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.client.ApacheClient;
import com.cleversafe.og.guice.annotation.DeleteHeaders;
import com.cleversafe.og.guice.annotation.DeleteHost;
import com.cleversafe.og.guice.annotation.DeleteObjectName;
import com.cleversafe.og.guice.annotation.ReadHeaders;
import com.cleversafe.og.guice.annotation.ReadHost;
import com.cleversafe.og.guice.annotation.ReadObjectName;
import com.cleversafe.og.guice.annotation.WriteHeaders;
import com.cleversafe.og.guice.annotation.WriteHost;
import com.cleversafe.og.guice.annotation.WriteObjectName;
import com.cleversafe.og.http.Api;
import com.cleversafe.og.http.BasicAuth;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpAuth;
import com.cleversafe.og.http.HttpUtil;
import com.cleversafe.og.http.ResponseBodyConsumer;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.json.AuthType;
import com.cleversafe.og.json.ChoiceConfig;
import com.cleversafe.og.json.ClientConfig;
import com.cleversafe.og.json.ConcurrencyConfig;
import com.cleversafe.og.json.ConcurrencyType;
import com.cleversafe.og.json.DistributionType;
import com.cleversafe.og.json.FilesizeConfig;
import com.cleversafe.og.json.OGConfig;
import com.cleversafe.og.json.ObjectManagerConfig;
import com.cleversafe.og.json.OperationConfig;
import com.cleversafe.og.json.SelectionConfig;
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
import com.cleversafe.og.supplier.DeleteObjectNameSupplier;
import com.cleversafe.og.supplier.RandomSupplier;
import com.cleversafe.og.supplier.ReadObjectNameSupplier;
import com.cleversafe.og.supplier.RequestSupplier;
import com.cleversafe.og.supplier.Suppliers;
import com.cleversafe.og.supplier.UUIDObjectNameSupplier;
import com.cleversafe.og.test.LoadTest;
import com.cleversafe.og.test.LoadTestSubscriberExceptionHandler;
import com.cleversafe.og.test.RequestManager;
import com.cleversafe.og.test.SimpleRequestManager;
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
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.google.inject.spi.ProvisionListener;
import com.google.inject.util.Providers;

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
    bind(Integer.class).annotatedWith(Names.named("port")).toProvider(
        Providers.of(this.config.getPort()));
    bind(Api.class).toInstance(this.config.getApi());
    bindConstant().annotatedWith(Names.named("write.weight"))
        .to(this.config.getWrite().getWeight());
    bindConstant().annotatedWith(Names.named("read.weight")).to(this.config.getRead().getWeight());
    bindConstant().annotatedWith(Names.named("delete.weight")).to(
        this.config.getDelete().getWeight());
    bind(AuthType.class).toInstance(this.config.getAuthentication().getType());
    bind(String.class).annotatedWith(Names.named("authentication.username")).toProvider(
        Providers.of(this.config.getAuthentication().getUsername()));
    bind(String.class).annotatedWith(Names.named("authentication.password")).toProvider(
        Providers.of(this.config.getAuthentication().getPassword()));
    bind(StoppingConditionsConfig.class).toInstance(this.config.getStoppingConditions());

    MapBinder<AuthType, HttpAuth> httpAuthBinder =
        MapBinder.newMapBinder(binder(), AuthType.class, HttpAuth.class);
    httpAuthBinder.addBinding(AuthType.AWSV2).to(AWSAuthV2.class);
    httpAuthBinder.addBinding(AuthType.BASIC).to(BasicAuth.class);

    MapBinder<String, ResponseBodyConsumer> responseBodyConsumers =
        MapBinder.newMapBinder(binder(), String.class, ResponseBodyConsumer.class);
    responseBodyConsumers.addBinding(SOH_PUT_OBJECT).to(SOHWriteResponseBodyConsumer.class);

    bind(RequestManager.class).to(SimpleRequestManager.class);
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
  @WriteObjectName
  public Function<Map<String, String>, String> provideWriteObjectName(final Api api) {
    if (Api.SOH == checkNotNull(api))
      return null;
    return new UUIDObjectNameSupplier();
  }

  @Provides
  @Singleton
  @ReadObjectName
  public Function<Map<String, String>, String> provideReadObjectName(
      final ObjectManager objectManager) {
    return new ReadObjectNameSupplier(objectManager);
  }

  @Provides
  @Singleton
  @DeleteObjectName
  public Function<Map<String, String>, String> provideDeleteObjectName(
      final ObjectManager objectManager) {
    return new DeleteObjectNameSupplier(objectManager);
  }

  @Provides
  @Singleton
  public List<AbstractObjectNameConsumer> provideObjectNameConsumers(
      final ObjectManager objectManager, final EventBus eventBus) {
    final Set<Integer> sc = HttpUtil.SUCCESS_STATUS_CODES;
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
  @Named("request.id")
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
  @Named("host")
  public Supplier<String> provideHost() {
    return createHost(this.config.getHost());
  }

  @Provides
  @Singleton
  @WriteHost
  public Supplier<String> provideWriteHost(@Named("host") final Supplier<String> host) {
    return provideHost(this.config.getWrite(), host);
  }

  @Provides
  @Singleton
  @ReadHost
  public Supplier<String> provideReadHost(@Named("host") final Supplier<String> host) {
    return provideHost(this.config.getRead(), host);
  }

  @Provides
  @Singleton
  @DeleteHost
  public Supplier<String> provideDeleteHost(@Named("host") final Supplier<String> host) {
    return provideHost(this.config.getDelete(), host);
  }

  private Supplier<String> provideHost(final OperationConfig operationConfig,
      final Supplier<String> testHost) {
    checkNotNull(operationConfig);
    checkNotNull(testHost);

    final SelectionConfig<String> operationHost = operationConfig.getHost();
    if (operationHost != null && !operationHost.choices.isEmpty())
      return createHost(operationConfig.getHost());

    return testHost;
  }

  private Supplier<String> createHost(SelectionConfig<String> host) {
    checkNotNull(host);
    checkNotNull(host.selection);
    checkNotNull(host.choices);
    checkArgument(!host.choices.isEmpty(), "must specify at least one host");
    for (final ChoiceConfig<String> choice : host.choices) {
      checkNotNull(choice);
      checkNotNull(choice.choice);
      checkArgument(choice.choice.length() > 0, "host must not be empty string");
    }

    if (SelectionType.ROUNDROBIN == host.selection) {
      final List<String> hostList = Lists.newArrayList();
      for (final ChoiceConfig<String> choice : host.choices) {
        hostList.add(choice.choice);
      }
      return Suppliers.cycle(hostList);
    }

    final RandomSupplier.Builder<String> wrc = Suppliers.random();
    for (final ChoiceConfig<String> choice : host.choices) {
      wrc.withChoice(choice.choice, choice.weight);
    }
    return wrc.build();
  }

  @Provides
  @Singleton
  @Named("uri.root")
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
  @Named("container")
  public Function<Map<String, String>, String> provideContainer() {
    final String container = checkNotNull(this.config.getContainer());
    checkArgument(container.length() > 0, "container must not be empty string");
    // FIXME may need to extract a real Function implementation, especially for multi container
    final Supplier<String> objectSupplier = Suppliers.of(this.config.getContainer());
    return new Function<Map<String, String>, String>() {

      @Override
      public String apply(Map<String, String> input) {
        return objectSupplier.get();
      }
    };
  }

  @Provides
  @Singleton
  @WriteHeaders
  public Map<String, Supplier<String>> provideWriteHeaders() {
    return provideHeaders(this.config.getWrite().getHeaders());
  }

  @Provides
  @Singleton
  @ReadHeaders
  public Map<String, Supplier<String>> provideReadHeaders() {
    return provideHeaders(this.config.getRead().getHeaders());
  }

  @Provides
  @Singleton
  @DeleteHeaders
  public Map<String, Supplier<String>> provideDeleteHeaders() {
    return provideHeaders(this.config.getDelete().getHeaders());
  }

  private Map<String, Supplier<String>> provideHeaders(
      Map<String, SelectionConfig<String>> operationHeaders) {
    checkNotNull(operationHeaders);
    Map<String, SelectionConfig<String>> configHeaders = this.config.getHeaders();
    if (operationHeaders.size() > 0) {
      configHeaders = operationHeaders;
    }

    Map<String, Supplier<String>> headers = Maps.newLinkedHashMap();
    for (Map.Entry<String, SelectionConfig<String>> e : configHeaders.entrySet()) {
      headers.put(e.getKey(), createHeaderSuppliers(e.getValue()));
    }

    return headers;
  }

  private Supplier<String> createHeaderSuppliers(SelectionConfig<String> selectionConfig) {
    // FIXME create generalized process for creating random or roundrobin suppliers regardless
    // of config type
    if (SelectionType.ROUNDROBIN == selectionConfig.selection) {
      final List<String> choiceList = Lists.newArrayList();
      for (final ChoiceConfig<String> choice : selectionConfig.choices) {
        choiceList.add(choice.choice);
      }
      return Suppliers.cycle(choiceList);
    }

    final RandomSupplier.Builder<String> wrc = Suppliers.random();
    for (final ChoiceConfig<String> choice : selectionConfig.choices) {
      wrc.withChoice(choice.choice, choice.weight);
    }
    return wrc.build();
  }

  @Provides
  @Singleton
  public Supplier<Body> provideBody() {
    SelectionConfig<FilesizeConfig> filesizeConfig = this.config.getFilesize();
    final SelectionType filesizeSelection = checkNotNull(filesizeConfig.selection);
    final List<ChoiceConfig<FilesizeConfig>> filesizes = checkNotNull(filesizeConfig.choices);
    checkArgument(!filesizes.isEmpty(), "filesize must not be empty");

    if (SelectionType.ROUNDROBIN == filesizeSelection) {
      final List<Distribution> distributions = Lists.newArrayList();
      for (final ChoiceConfig<FilesizeConfig> choice : filesizes) {
        distributions.add(createSizeDistribution(choice.choice));
      }
      return createBodySupplier(Suppliers.cycle(distributions));
    }

    final RandomSupplier.Builder<Distribution> wrc = Suppliers.random();
    for (final ChoiceConfig<FilesizeConfig> f : filesizes) {
      wrc.withChoice(createSizeDistribution(f.choice), f.weight);
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
  public String provideObjectFileName(
      @Named("container") final Function<Map<String, String>, String> container, final Api api) {
    checkNotNull(container);
    checkNotNull(api);
    final ObjectManagerConfig objectManagerConfig = checkNotNull(this.config.getObjectManager());
    final String objectFileName = objectManagerConfig.getObjectFileName();

    if (objectFileName != null && !objectFileName.isEmpty())
      return objectFileName;
    // FIXME this naming scheme will break unless @TestContainer is a constant supplier
    Map<String, String> context = Maps.newHashMap();
    return container.apply(context) + "-" + api.toString().toLowerCase();
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
  public Client provideClient(final AuthType authType,
      final Map<AuthType, HttpAuth> authentication,
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
            .withAuthentication(authentication.get(authType))
            .withUserAgent(Version.displayVersion())
            .withWriteThroughput(clientConfig.getWriteThroughput())
            .withReadThroughput(clientConfig.getReadThroughput());

    for (final Entry<String, ResponseBodyConsumer> consumer : responseBodyConsumers.entrySet()) {
      b.withResponseBodyConsumer(consumer.getKey(), consumer.getValue());
    }

    return b.build();
  }

  @Provides
  @Singleton
  @Named("write")
  public Supplier<Request> provideWrite(@Named("request.id") final Supplier<String> id,
      final Api api, final Scheme scheme, @WriteHost final Supplier<String> host,
      @Named("port") final Integer port, @Named("uri.root") final String uriRoot,
      @Named("container") final Function<Map<String, String>, String> container,
      @WriteObjectName final Function<Map<String, String>, String> object,
      @WriteHeaders final Map<String, Supplier<String>> headers, final Supplier<Body> body,
      @Named("authentication.username") final String username,
      @Named("authentication.password") final String password) {
    checkNotNull(api);
    // SOH needs to use a special response consumer to extract the returned object id
    if (Api.SOH == api)
      headers.put(Headers.X_OG_RESPONSE_BODY_CONSUMER, Suppliers.of(SOH_PUT_OBJECT));

    return createRequestSupplier(id, Method.PUT, scheme, host, port, uriRoot, container, object,
        headers, body, username, password);
  }

  @Provides
  @Singleton
  @Named("read")
  public Supplier<Request> provideRead(@Named("request.id") final Supplier<String> id,
      final Scheme scheme, @ReadHost final Supplier<String> host,
      @Named("port") final Integer port, @Named("uri.root") final String uriRoot,
      @Named("container") final Function<Map<String, String>, String> container,
      @ReadObjectName final Function<Map<String, String>, String> object,
      @ReadHeaders final Map<String, Supplier<String>> headers,
      @Named("authentication.username") final String username,
      @Named("authentication.password") final String password) {
    return createRequestSupplier(id, Method.GET, scheme, host, port, uriRoot, container, object,
        headers, Suppliers.of(Bodies.none()), username, password);
  }

  @Provides
  @Singleton
  @Named("delete")
  public Supplier<Request> provideDelete(@Named("request.id") final Supplier<String> id,
      final Scheme scheme, @DeleteHost final Supplier<String> host,
      @Named("port") final Integer port, @Named("uri.root") final String uriRoot,
      @Named("container") final Function<Map<String, String>, String> container,
      @DeleteObjectName final Function<Map<String, String>, String> object,
      @DeleteHeaders final Map<String, Supplier<String>> headers,
      @Named("authentication.username") final String username,
      @Named("authentication.password") final String password) {
    return createRequestSupplier(id, Method.DELETE, scheme, host, port, uriRoot, container, object,
        headers, Suppliers.of(Bodies.none()), username, password);
  }

  private Supplier<Request> createRequestSupplier(@Named("request.id") final Supplier<String> id,
      final Method method, Scheme scheme, final Supplier<String> host, final Integer port,
      final String uriRoot, final Function<Map<String, String>, String> container,
      final Function<Map<String, String>, String> object,
      final Map<String, Supplier<String>> headers, final Supplier<Body> body,
      final String username, final String password) {

    return new RequestSupplier(id, method, scheme, host, port, uriRoot, container, object,
        Collections.<String, String>emptyMap(), false, headers, username, password, body);
  }
}
