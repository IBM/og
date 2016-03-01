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

import javax.annotation.Nullable;
import javax.inject.Named;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Client;
import com.cleversafe.og.api.DataType;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Operation;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.client.ApacheClient;
import com.cleversafe.og.guice.annotation.DeleteHeaders;
import com.cleversafe.og.guice.annotation.DeleteHost;
import com.cleversafe.og.guice.annotation.DeleteObjectName;
import com.cleversafe.og.guice.annotation.ListHeaders;
import com.cleversafe.og.guice.annotation.ListHost;
import com.cleversafe.og.guice.annotation.ListQueryParameters;
import com.cleversafe.og.guice.annotation.MetadataHeaders;
import com.cleversafe.og.guice.annotation.MetadataHost;
import com.cleversafe.og.guice.annotation.MetadataObjectName;
import com.cleversafe.og.guice.annotation.OverwriteHeaders;
import com.cleversafe.og.guice.annotation.OverwriteHost;
import com.cleversafe.og.guice.annotation.OverwriteObjectName;
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
import com.cleversafe.og.http.NoneAuth;
import com.cleversafe.og.http.QueryParameters;
import com.cleversafe.og.http.ResponseBodyConsumer;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.json.AuthType;
import com.cleversafe.og.json.ChoiceConfig;
import com.cleversafe.og.json.ClientConfig;
import com.cleversafe.og.json.ConcurrencyConfig;
import com.cleversafe.og.json.ConcurrencyType;
import com.cleversafe.og.json.ContainerConfig;
import com.cleversafe.og.json.DistributionType;
import com.cleversafe.og.json.FilesizeConfig;
import com.cleversafe.og.json.OGConfig;
import com.cleversafe.og.json.ObjectConfig;
import com.cleversafe.og.json.ObjectManagerConfig;
import com.cleversafe.og.json.OperationConfig;
import com.cleversafe.og.json.SelectionConfig;
import com.cleversafe.og.json.SelectionType;
import com.cleversafe.og.json.StoppingConditionsConfig;
import com.cleversafe.og.object.*;
import com.cleversafe.og.openstack.KeystoneAuth;
import com.cleversafe.og.s3.v2.AWSV2Auth;
import com.cleversafe.og.s3.v4.AWSV4Auth;
import com.cleversafe.og.scheduling.ConcurrentRequestScheduler;
import com.cleversafe.og.scheduling.RequestRateScheduler;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.soh.SOHWriteResponseBodyConsumer;
import com.cleversafe.og.statistic.Counter;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.supplier.DeleteObjectNameFunction;
import com.cleversafe.og.supplier.MetadataObjectNameFunction;
import com.cleversafe.og.supplier.RandomSupplier;
import com.cleversafe.og.supplier.ReadObjectNameFunction;
import com.cleversafe.og.supplier.RequestSupplier;
import com.cleversafe.og.supplier.Suppliers;
import com.cleversafe.og.supplier.UUIDObjectNameFunction;
import com.cleversafe.og.test.LoadTest;
import com.cleversafe.og.test.LoadTestSubscriberExceptionHandler;
import com.cleversafe.og.test.RequestManager;
import com.cleversafe.og.test.SimpleRequestManager;
import com.cleversafe.og.test.condition.ConcurrentRequestCondition;
import com.cleversafe.og.test.condition.CounterCondition;
import com.cleversafe.og.test.condition.RuntimeCondition;
import com.cleversafe.og.test.condition.StatusCodeCondition;
import com.cleversafe.og.test.condition.TestCondition;
import com.cleversafe.og.util.Context;
import com.cleversafe.og.util.Distribution;
import com.cleversafe.og.util.Distributions;
import com.cleversafe.og.util.MoreFunctions;
import com.cleversafe.og.util.SizeUnit;
import com.cleversafe.og.util.Version;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.google.inject.spi.ProvisionListener;
import com.google.inject.util.Providers;

/**
 * A guice configuration module for wiring up all OG test components
 * 
 * @since 1.0
 */
public class OGModule extends AbstractModule {
  private final OGConfig config;
  private static final String SOH_PUT_OBJECT = "soh.put_object";
  private final LoadTestSubscriberExceptionHandler handler;
  private final EventBus eventBus;

  /**
   * Creates an instance
   * 
   * @param config json source configuration
   * @throws NullPointerException if config is null
   */
  public OGModule(final OGConfig config) {
    this.config = checkNotNull(config);
    this.handler = new LoadTestSubscriberExceptionHandler();
    this.eventBus = new EventBus(this.handler);
  }

  @Override
  protected void configure() {
    bind(Scheme.class).toInstance(this.config.scheme);
    bind(Integer.class).annotatedWith(Names.named("port"))
        .toProvider(Providers.of(this.config.port));
    bind(Api.class).toProvider(new Provider<Api>() {
      @Override
      public Api get() {
        return checkNotNull(OGModule.this.config.api, "api must not be null");
      }
    });
    bindConstant().annotatedWith(Names.named("write.weight")).to(this.config.write.weight);
    bindConstant().annotatedWith(Names.named("overwrite.weight")).to(this.config.overwrite.weight);
    bindConstant().annotatedWith(Names.named("read.weight")).to(this.config.read.weight);
    bindConstant().annotatedWith(Names.named("metadata.weight")).to(this.config.metadata.weight);
    bindConstant().annotatedWith(Names.named("delete.weight")).to(this.config.delete.weight);
    bindConstant().annotatedWith(Names.named("list.weight")).to(this.config.list.weight);
    bindConstant().annotatedWith(Names.named("virtualhost")).to(this.config.virtualHost);
    bind(AuthType.class).toInstance(this.config.authentication.type);
    bind(DataType.class).toInstance(this.config.data);
    bind(String.class).annotatedWith(Names.named("authentication.username"))
        .toProvider(Providers.of(this.config.authentication.username));
    bind(String.class).annotatedWith(Names.named("authentication.password"))
        .toProvider(Providers.of(this.config.authentication.password));
    bind(String.class).annotatedWith(Names.named("authentication.keystoneToken"))
        .toProvider(Providers.of(this.config.authentication.keystoneToken));
    bindConstant().annotatedWith(Names.named("authentication.awsChunked"))
        .to(this.config.authentication.awsChunked);
    bindConstant().annotatedWith(Names.named("authentication.awsCacheSize"))
        .to(this.config.authentication.awsCacheSize);
    // FIXME create something like MoreProviders.notNull as a variant of Providers.of which does a
    // null check at creation time, with a custom error message; replace all uses of this pattern
    bind(ConcurrencyConfig.class).toProvider(new Provider<ConcurrencyConfig>() {
      @Override
      public ConcurrencyConfig get() {
        return checkNotNull(OGModule.this.config.concurrency, "concurrency must not be null");
      }
    });
    bind(StoppingConditionsConfig.class).toInstance(this.config.stoppingConditions);
    bindConstant().annotatedWith(Names.named("shutdownImmediate"))
        .to(this.config.shutdownImmediate);

    final MapBinder<AuthType, HttpAuth> httpAuthBinder =
        MapBinder.newMapBinder(binder(), AuthType.class, HttpAuth.class);
    httpAuthBinder.addBinding(AuthType.NONE).to(NoneAuth.class);
    httpAuthBinder.addBinding(AuthType.BASIC).to(BasicAuth.class);
    httpAuthBinder.addBinding(AuthType.AWSV2).to(AWSV2Auth.class);
    httpAuthBinder.addBinding(AuthType.AWSV4).to(AWSV4Auth.class);
    httpAuthBinder.addBinding(AuthType.KEYSTONE).to(KeystoneAuth.class);

    final MapBinder<String, ResponseBodyConsumer> responseBodyConsumers =
        MapBinder.newMapBinder(binder(), String.class, ResponseBodyConsumer.class);
    responseBodyConsumers.addBinding(SOH_PUT_OBJECT).to(SOHWriteResponseBodyConsumer.class);

    bind(RequestManager.class).to(SimpleRequestManager.class);
    bind(LoadTest.class).in(Singleton.class);
    bind(EventBus.class).toInstance(this.eventBus);
    bind(Statistics.class).in(Singleton.class);
    bind(ObjectManager.class).to(RandomObjectPopulator.class).in(Singleton.class);
    bindListener(Matchers.any(), new ProvisionListener() {
      @Override
      public <T> void onProvision(final ProvisionInvocation<T> provision) {
        // register every non-null provisioned instance with the global event bus. EventBus treats
        // registration of instances without an @Subscribe method as a no-op and handles duplicate
        // registration such that a given @Subscribe annotated method will only be triggered once
        // per event
        final T instance = provision.provision();
        if (instance != null) {
          OGModule.this.eventBus.register(instance);
        }
        if (instance instanceof LoadTest) {
          // register LoadTest with the event bus' exception handler
          OGModule.this.handler.setLoadTest((LoadTest) instance);
        }
      }
    });
  }

  @Provides
  @Singleton
  public List<TestCondition> provideTestConditions(final LoadTest test, final EventBus eventBus,
      final Statistics stats, final ConcurrencyConfig concurrency,
      final StoppingConditionsConfig config) {
    checkNotNull(test);
    checkNotNull(stats);
    checkNotNull(config);
    checkArgument(config.operations >= 0, "operations must be >= 0 [%s]", config.operations);
    checkArgument(config.runtime >= 0.0, "runtime must be >= 0.0 [%s]", config.runtime);
    checkNotNull(config.runtimeUnit);
    checkArgument(config.concurrentRequests >= 0, "concurrentRequests must be >= 0 [%s]",
        config.concurrentRequests);
    checkNotNull(config.statusCodes);
    for (final Entry<Integer, Integer> sc : config.statusCodes.entrySet()) {
      checkArgument(sc.getValue() >= 0.0, "status code [%s] value must be >= 0.0 [%s]", sc.getKey(),
          sc.getValue());
    }

    final List<TestCondition> conditions = Lists.newArrayList();

    if (config.operations > 0) {
      conditions.add(
          new CounterCondition(Operation.ALL, Counter.OPERATIONS, config.operations, test, stats));
    }

    final Map<Integer, Integer> scMap = config.statusCodes;
    for (final Entry<Integer, Integer> sc : scMap.entrySet()) {
      if (sc.getValue() > 0) {
        conditions
            .add(new StatusCodeCondition(Operation.ALL, sc.getKey(), sc.getValue(), test, stats));
      }
    }

    if (config.runtime > 0) {
      conditions.add(new RuntimeCondition(test, config.runtime, config.runtimeUnit));
    }

    // maximum concurrent requests only makes sense in the context of an ops test, so check for that
    if (config.concurrentRequests > 0 && concurrency.type == ConcurrencyType.OPS) {
      conditions.add(
          new ConcurrentRequestCondition(Operation.ALL, config.concurrentRequests, test, stats));
    }

    for (final TestCondition condition : conditions) {
      eventBus.register(condition);
    }

    return conditions;
  }

  @Provides
  @Singleton
  @Named("request.id")
  public Function<Map<String, String>, String> provideIdSupplier() {
    final Supplier<String> idSupplier = new Supplier<String>() {
      private final AtomicLong id = new AtomicLong();

      @Override
      public String get() {
        return String.valueOf(this.id.getAndIncrement());
      }
    };

    return MoreFunctions.forSupplier(idSupplier);
  }

  @Provides
  @Singleton
  @Named("host")
  public Function<Map<String, String>, String> provideHost() {
    return createHost(this.config.host);
  }

  @Provides
  @Singleton
  @WriteHost
  public Function<Map<String, String>, String> provideWriteHost(
      @Named("host") final Function<Map<String, String>, String> host) {
    return provideHost(this.config.write, host);
  }

  @Provides
  @Singleton
  @OverwriteHost
  public Function<Map<String, String>, String> provideOverwriteHost(
      @Named("host") final Function<Map<String, String>, String> host) {
    return provideHost(this.config.overwrite, host);
  }

  @Provides
  @Singleton
  @ReadHost
  public Function<Map<String, String>, String> provideReadHost(
      @Named("host") final Function<Map<String, String>, String> host) {
    return provideHost(this.config.read, host);
  }

  @Provides
  @Singleton
  @MetadataHost
  public Function<Map<String, String>, String> provideMetadataHost(
      @Named("host") final Function<Map<String, String>, String> host) {
    return provideHost(this.config.metadata, host);
  }

  @Provides
  @Singleton
  @DeleteHost
  public Function<Map<String, String>, String> provideDeleteHost(
      @Named("host") final Function<Map<String, String>, String> host) {
    return provideHost(this.config.delete, host);
  }

  @Provides
  @Singleton
  @ListHost
  public Function<Map<String, String>, String> provideListHost(
      @Named("host") final Function<Map<String, String>, String> host) {
    return provideHost(this.config.list, host);
  }

  private Function<Map<String, String>, String> provideHost(final OperationConfig operationConfig,
      final Function<Map<String, String>, String> testHost) {
    checkNotNull(operationConfig);
    checkNotNull(testHost);

    final SelectionConfig<String> operationHost = operationConfig.host;
    if (operationHost != null && !operationHost.choices.isEmpty()) {
      return createHost(operationConfig.host);
    }

    return testHost;
  }

  private Function<Map<String, String>, String> createHost(final SelectionConfig<String> host) {
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
      final Supplier<String> hostSupplier = Suppliers.cycle(hostList);
      return MoreFunctions.forSupplier(hostSupplier);
    }

    final RandomSupplier.Builder<String> wrc = Suppliers.random();
    for (final ChoiceConfig<String> choice : host.choices) {
      wrc.withChoice(choice.choice, choice.weight);
    }
    final Supplier<String> hostSupplier = wrc.build();
    return MoreFunctions.forSupplier(hostSupplier);
  }

  @Provides
  @Singleton
  @Named("uri.root")
  public String provideUriRoot(final Api api) {
    final String uriRoot = this.config.uriRoot;
    if (uriRoot != null) {
      final String root = CharMatcher.is('/').trimFrom(uriRoot);
      if (root.length() > 0) {
        return root;
      }
      return null;
    }

    return this.config.api.toString().toLowerCase();
  }

  private Supplier<Integer> createContainerSuffixes(final ContainerConfig config) {
    checkNotNull(config);
    if ((ContainerConfig.NONE == config.minSuffix) || (ContainerConfig.NONE == config.maxSuffix)) {
      return null;
    }
    checkArgument(config.maxSuffix >= config.minSuffix,
        "container max_suffix must be greater than or equal to min_suffix");

    if (SelectionType.ROUNDROBIN == config.selection) {
      final List<Integer> containerList = Lists.newArrayList();
      for (int i = config.minSuffix; i <= config.maxSuffix; ++i) {
        containerList.add(i);
      }
      return Suppliers.cycle(containerList);
    } else if (SelectionType.RANDOM == config.selection) {
      final RandomSupplier.Builder<Integer> cid = Suppliers.random();
      if (config.weights != null) {
        for (int i = config.minSuffix; i <= config.maxSuffix; ++i) {
          cid.withChoice(i, config.weights.get(i - config.minSuffix));
        }
      } else {
        for (int i = config.minSuffix; i <= config.maxSuffix; ++i) {
          cid.withChoice(i);
        }
      }
      return cid.build();
    }
    return null;
  }

  @Provides
  @Singleton
  @Named("container")
  public Function<Map<String, String>, String> provideContainer() {
    final ContainerConfig containerConfig = this.config.container;
    final String container = checkNotNull(containerConfig.prefix);
    checkArgument(container.length() > 0, "container must not be empty string");

    final Supplier<Integer> suffixes = createContainerSuffixes(containerConfig);

    return new Function<Map<String, String>, String>() {

      @Override
      public String apply(final Map<String, String> input) {
        String suffix = input.get(Context.X_OG_CONTAINER_SUFFIX);
        if (suffix != null) {
          if (Integer.parseInt(suffix) == -1) {
            return container;
          } else {
            return container.concat(suffix);
          }
        } else {
          if (suffixes != null) {
            suffix = suffixes.get().toString();
            input.put(Context.X_OG_CONTAINER_SUFFIX, suffix);
            return container.concat(suffix);
          } else {
            input.put(Context.X_OG_CONTAINER_SUFFIX, "-1");
            return container;
          }
        }
      }
    };
  }

  @Provides
  @Singleton
  @WriteObjectName
  public Function<Map<String, String>, String> provideWriteObjectName(final Api api) {
    if (Api.SOH == api) {
      return null;
    }
    return MoreFunctions.keyLookup(Context.X_OG_OBJECT_NAME);
  }

  @Provides
  @Singleton
  @OverwriteObjectName
  public Function<Map<String, String>, String> provideOverwriteObjectName() {
    return MoreFunctions.keyLookup(Context.X_OG_OBJECT_NAME);
  }

  @Provides
  @Singleton
  @ReadObjectName
  public Function<Map<String, String>, String> provideReadObjectName() {
    return MoreFunctions.keyLookup(Context.X_OG_OBJECT_NAME);
  }

  @Provides
  @Singleton
  @MetadataObjectName
  public Function<Map<String, String>, String> provideMetadataObjectName() {
    return MoreFunctions.keyLookup(Context.X_OG_OBJECT_NAME);
  }

  @Provides
  @Singleton
  @DeleteObjectName
  public Function<Map<String, String>, String> provideDeleteObjectName() {
    return MoreFunctions.keyLookup(Context.X_OG_OBJECT_NAME);
  }

  private Function<Map<String, String>, String> provideObject(
      final OperationConfig operationConfig) {
    checkNotNull(operationConfig);

    final ObjectConfig objectConfig = checkNotNull(operationConfig.object);
    final String prefix = checkNotNull(objectConfig.prefix);
    final Supplier<Long> suffixes = createObjectSuffixes(objectConfig);
    return new Function<Map<String, String>, String>() {
      @Override
      public String apply(final Map<String, String> context) {
        final String objectName = prefix + suffixes.get();
        context.put(Context.X_OG_OBJECT_NAME, objectName);
        context.put(Context.X_OG_SEQUENTIAL_OBJECT_NAME, "true");

        return objectName;
      }
    };
  }

  private Supplier<Long> createObjectSuffixes(final ObjectConfig config) {
    checkArgument(config.minSuffix >= 0, "minSuffix must be > 0 [%s]", config.minSuffix);
    checkArgument(config.maxSuffix >= config.minSuffix,
        "maxSuffix must be greater than or equal to minSuffix");

    if (SelectionType.ROUNDROBIN == config.selection) {
      return Suppliers.cycle(config.minSuffix, config.maxSuffix);
    } else {
      return Suppliers.random(config.minSuffix, config.maxSuffix);
    }
  }

  @Provides
  @Singleton
  public List<AbstractObjectNameConsumer> provideObjectNameConsumers(
      final ObjectManager objectManager, final EventBus eventBus) {
    final Set<Integer> sc = HttpUtil.SUCCESS_STATUS_CODES;
    final List<AbstractObjectNameConsumer> consumers = Lists.newArrayList();
    consumers.add(new WriteObjectNameConsumer(objectManager, sc));
    consumers.add(new ReadObjectNameConsumer(objectManager, sc));
    consumers.add(new MetadataObjectNameConsumer(objectManager, sc));
    consumers.add(new OverwriteObjectNameConsumer(objectManager, sc));
    consumers.add(new ListObjectNameConsumer(objectManager, sc));

    for (final AbstractObjectNameConsumer consumer : consumers) {
      eventBus.register(consumer);
    }
    return consumers;
  }

  @Provides
  @Singleton
  @WriteHeaders
  public Map<String, Function<Map<String, String>, String>> provideWriteHeaders() {
    return provideHeaders(this.config.write.headers);
  }

  @Provides
  @Singleton
  @OverwriteHeaders
  public Map<String, Function<Map<String, String>, String>> provideOverwriteHeaders() {
    return provideHeaders(this.config.overwrite.headers);
  }

  @Provides
  @Singleton
  @ReadHeaders
  public Map<String, Function<Map<String, String>, String>> provideReadHeaders() {
    return provideHeaders(this.config.read.headers);
  }

  @Provides
  @Singleton
  @MetadataHeaders
  public Map<String, Function<Map<String, String>, String>> provideMetadataHeaders() {
    return provideHeaders(this.config.metadata.headers);
  }

  @Provides
  @Singleton
  @DeleteHeaders
  public Map<String, Function<Map<String, String>, String>> provideDeleteHeaders() {
    return provideHeaders(this.config.delete.headers);
  }

  @Provides
  @Singleton
  @ListHeaders
  public Map<String, Function<Map<String, String>, String>> provideListHeaders(final Api api) {


    final Map<String, Function<Map<String, String>, String>> headers =
        provideHeaders(this.config.list.headers);

    if (api == Api.SOH) {
      final Supplier<String> operationSupplier = Suppliers.of("list");
      final Function<Map<String, String>, String> operation =
          MoreFunctions.forSupplier(operationSupplier);
      headers.put(Headers.X_OPERATION, operation);
      headers.put(Headers.X_START_ID, MoreFunctions.keyLookup(Context.X_OG_OBJECT_NAME));
    }
    return headers;
  }

  private Map<String, Function<Map<String, String>, String>> provideHeaders(
      final Map<String, SelectionConfig<String>> operationHeaders) {
    checkNotNull(operationHeaders);
    Map<String, SelectionConfig<String>> configHeaders = this.config.headers;
    if (operationHeaders.size() > 0) {
      configHeaders = operationHeaders;
    }

    final Map<String, Function<Map<String, String>, String>> headers = Maps.newLinkedHashMap();
    for (final Map.Entry<String, SelectionConfig<String>> e : configHeaders.entrySet()) {
      headers.put(e.getKey(), createHeaderSuppliers(e.getValue()));
    }

    return headers;
  }

  @Provides
  @Singleton
  @Named("write.context")
  public List<Function<Map<String, String>, String>> provideWriteContext(final Api api) {
    final List<Function<Map<String, String>, String>> context = Lists.newArrayList();

    final OperationConfig operationConfig = checkNotNull(this.config.write);
    if (operationConfig.object.selection != null) {
      context.add(provideObject(operationConfig));
    } else {
      // default for writes
      context.add(new UUIDObjectNameFunction());
    }

    // SOH needs to use a special response consumer to extract the returned object id
    if (Api.SOH == api) {
      context.add(new Function<Map<String, String>, String>() {
        @Override
        public String apply(final Map<String, String> input) {
          input.put(Context.X_OG_RESPONSE_BODY_CONSUMER, SOH_PUT_OBJECT);

          return null;
        }
      });
    }

    return ImmutableList.copyOf(context);
  }

  @Provides
  @Singleton
  @Named("overwrite.context")
  public List<Function<Map<String, String>, String>> provideOverwriteContext(
      final ObjectManager objectManager) {
    // FIXME add check if user has configured random/roundrobin here, it is a logical error
    // Delete the object so we know no other threads will be using it
    final Function<Map<String, String>, String> function =
        new DeleteObjectNameFunction(objectManager);
    return ImmutableList.of(function);
  }

  @Provides
  @Singleton
  @Named("read.context")
  public List<Function<Map<String, String>, String>> provideReadContext(
      final ObjectManager objectManager) {
    Function<Map<String, String>, String> function;

    final OperationConfig operationConfig = checkNotNull(this.config.read);
    if (operationConfig.object.selection != null) {
      function = provideObject(operationConfig);
    } else {
      function = new ReadObjectNameFunction(objectManager);
    }

    return ImmutableList.of(function);
  }

  @Provides
  @Singleton
  @Named("metadata.context")
  public List<Function<Map<String, String>, String>> provideMetadataContext(
      final ObjectManager objectManager) {
    Function<Map<String, String>, String> function;

    final OperationConfig operationConfig = checkNotNull(this.config.metadata);
    if (operationConfig.object.selection != null) {
      function = provideObject(operationConfig);
    } else {
      function = new MetadataObjectNameFunction(objectManager);
    }

    return ImmutableList.of(function);
  }

  @Provides
  @Singleton
  @Named("delete.context")
  public List<Function<Map<String, String>, String>> provideDeleteContext(
      final ObjectManager objectManager) {
    Function<Map<String, String>, String> function;

    final OperationConfig operationConfig = checkNotNull(this.config.delete);
    if (operationConfig.object.selection != null) {
      function = provideObject(operationConfig);
    } else {
      function = new DeleteObjectNameFunction(objectManager);
    }

    return ImmutableList.of(function);
  }

  @Provides
  @Singleton
  @Named("list.context")
  public List<Function<Map<String, String>, String>> provideListContext(
      final ObjectManager objectManager) {
    Function<Map<String, String>, String> function;

    final OperationConfig operationConfig = checkNotNull(this.config.list);
    if (operationConfig.object.selection != null) {
      function = provideObject(operationConfig);
    } else {
      function = new ReadObjectNameFunction(objectManager);
    }

    return ImmutableList.of(function);
  }


  private Function<Map<String, String>, String> createHeaderSuppliers(
      final SelectionConfig<String> selectionConfig) {
    // FIXME create generalized process for creating random or roundrobin suppliers regardless
    // of config type
    if (SelectionType.ROUNDROBIN == selectionConfig.selection) {
      final List<String> choiceList = Lists.newArrayList();
      for (final ChoiceConfig<String> choice : selectionConfig.choices) {
        choiceList.add(choice.choice);
      }
      final Supplier<String> headerSupplier = Suppliers.cycle(choiceList);
      return MoreFunctions.forSupplier(headerSupplier);
    }

    final RandomSupplier.Builder<String> wrc = Suppliers.random();
    for (final ChoiceConfig<String> choice : selectionConfig.choices) {
      wrc.withChoice(choice.choice, choice.weight);
    }
    final Supplier<String> headerSupplier = wrc.build();
    return MoreFunctions.forSupplier(headerSupplier);
  }

  @Provides
  @Singleton
  @ListQueryParameters
  public Map<String, Function<Map<String, String>, String>> provideListQueryParameters(
      final Api api) {
    final Map<String, Function<Map<String, String>, String>> queryParameters;

    queryParameters = provideQueryParameters(this.config.list.parameters);

    if (api == Api.S3) {
      queryParameters.put(QueryParameters.S3_MARKER,
          MoreFunctions.keyLookup(Context.X_OG_OBJECT_NAME));
    } else if (api == Api.OPENSTACK) {
      queryParameters.put(QueryParameters.OPENSTACK_MARKER,
          MoreFunctions.keyLookup(Context.X_OG_OBJECT_NAME));
    }

    return queryParameters;
  }

  Map<String, Function<Map<String, String>, String>> provideQueryParameters(
      Map<String, String> operationQueryParameters) {
    final Map<String, Function<Map<String, String>, String>> queryParameters = Maps.newHashMap();

    for (final Map.Entry<String, String> e: operationQueryParameters.entrySet()) {
      final Supplier<String> queryParameterSupplier = new Supplier<String>() {
        final private String queryParamValue = e.getValue();
        @Override
        public String get() {
          return queryParamValue;
        }
      };
      Function<Map<String, String>, String> queryParameterFunction =
          MoreFunctions.forSupplier(queryParameterSupplier);
      queryParameters.put(e.getKey(), queryParameterFunction);
    }

    return queryParameters;
  }

  @Provides
  @Singleton
  public Function<Map<String, String>, Body> provideBody() {
    final SelectionConfig<FilesizeConfig> filesizeConfig =
        checkNotNull(this.config.filesize, "filesize must not be null");
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
    final SizeUnit averageUnit = checkNotNull(filesize.averageUnit);
    final SizeUnit spreadUnit = checkNotNull(filesize.spreadUnit);
    final DistributionType distribution = checkNotNull(filesize.distribution);
    checkNotNull(filesize.average, "filesize average must not be null");

    final double average = filesize.average * averageUnit.toBytes(1);
    final double spread = filesize.spread * spreadUnit.toBytes(1);

    switch (distribution) {
      case NORMAL:
        return Distributions.normal(average, spread);
      case LOGNORMAL:
        return Distributions.lognormal(average, spread);
      case UNIFORM:
        return Distributions.uniform(average, spread);
      default:
        throw new IllegalArgumentException(
            String.format("unacceptable filesize distribution [%s]", distribution));
    }
  }

  private Function<Map<String, String>, Body> createBodySupplier(final Supplier<Distribution> distributionSupplier) {
    final DataType data = checkNotNull(this.config.data);
    checkArgument(DataType.NONE != data, "Unacceptable data [%s]", data);

    final Supplier<Body> bodySupplier = new Supplier<Body>() {
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

    return MoreFunctions.forSupplier(bodySupplier);
  }

  @Provides
  @Singleton
  @Named("objectfile.location")
  public String provideObjectFileLocation() throws IOException {
    final String path = checkNotNull(this.config.objectManager.objectFileLocation);
    checkArgument(path.length() > 0, "path must not be empty string");

    final File f = new File(path).getCanonicalFile();
    if (!f.exists()) {
      final boolean success = f.mkdirs();
      if (!success) {
        throw new RuntimeException(
            String.format("failed to create object location directories [%s]", f.toString()));
      }
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
    final ObjectManagerConfig objectManagerConfig = checkNotNull(this.config.objectManager);
    final String objectFileName = objectManagerConfig.objectFileName;

    if (objectFileName != null && !objectFileName.isEmpty()) {
      return objectFileName;
    }
    return this.config.container.prefix + "-" + api.toString().toLowerCase();
  }

  @Provides
  @Singleton
  @Named("objectfile.maxsize")
  public long provideObjectFileMaxSize() {
    return checkNotNull(this.config.objectManager).objectFileMaxSize;
  }

  @Provides
  @Singleton
  @Named("objectfile.persistfrequency")
  public long provideObjectFilePersistFrequency() {
    return checkNotNull(this.config.objectManager).objectFilePersistFrequency;
  }

  @Provides
  @Singleton
  @Named("objectfile.index")
  public Integer provideObjectFileIndex() {
    return checkNotNull(this.config.objectManager).objectFileIndex;
  }

  @Provides
  @Singleton
  public Scheduler provideScheduler(final ConcurrencyConfig concurrency, final EventBus eventBus) {
    final ConcurrencyType type =
        checkNotNull(concurrency.type, "concurrency type must not be null");
    checkNotNull(concurrency.count, "concurrency count must not be null");

    if (ConcurrencyType.THREADS == type) {
      final Scheduler scheduler = new ConcurrentRequestScheduler(
          (int) Math.round(concurrency.count), concurrency.rampup, concurrency.rampupUnit);
      eventBus.register(scheduler);
      return scheduler;
    }
    return new RequestRateScheduler(concurrency.count, concurrency.unit, concurrency.rampup,
        concurrency.rampupUnit);
  }

  @Provides
  @Singleton
  public Client provideClient(final AuthType authType, final Map<AuthType, HttpAuth> authentication,
      final Map<String, ResponseBodyConsumer> responseBodyConsumers) {
    final ClientConfig clientConfig = this.config.client;
    Preconditions.checkArgument(
        authentication.get(authType) instanceof AWSV4Auth ? !clientConfig.chunkedEncoding : true,
        "http layer chunked encoding is not supported with Chunked AWSV4");
    final ApacheClient.Builder b = new ApacheClient.Builder()
        .withConnectTimeout(clientConfig.connectTimeout).withSoTimeout(clientConfig.soTimeout)
        .usingSoReuseAddress(clientConfig.soReuseAddress).withSoLinger(clientConfig.soLinger)
        .usingSoKeepAlive(clientConfig.soKeepAlive).usingTcpNoDelay(clientConfig.tcpNoDelay)
        .withSoSndBuf(clientConfig.soSndBuf).withSoRcvBuf(clientConfig.soRcvBuf)
        .usingPersistentConnections(clientConfig.persistentConnections)
        .withValidateAfterInactivity(clientConfig.validateAfterInactivity)
        .withMaxIdleTime(clientConfig.maxIdleTime)
        .usingChunkedEncoding(clientConfig.chunkedEncoding)
        .usingExpectContinue(clientConfig.expectContinue)
        .withWaitForContinue(clientConfig.waitForContinue).withRetryCount(clientConfig.retryCount)
        .usingRequestSentRetry(clientConfig.requestSentRetry)
        .withAuthentication(authentication.get(authType))
        .withUserAgent(String.format("og-%s", Version.displayVersion()))
        .withWriteThroughput(clientConfig.writeThroughput)
        .withReadThroughput(clientConfig.readThroughput);

    for (final Entry<String, ResponseBodyConsumer> consumer : responseBodyConsumers.entrySet()) {
      b.withResponseBodyConsumer(consumer.getKey(), consumer.getValue());
    }

    return b.build();
  }

  @Provides
  @Singleton
  @Named("write")
  public Supplier<Request> provideWrite(
      @Named("request.id") final Function<Map<String, String>, String> id, final Api api,
      final Scheme scheme, @WriteHost final Function<Map<String, String>, String> host,
      @Nullable @Named("port") final Integer port,
      @Nullable @Named("uri.root") final String uriRoot,
      @Named("container") final Function<Map<String, String>, String> container,
      @Nullable @WriteObjectName final Function<Map<String, String>, String> object,
      @WriteHeaders final Map<String, Function<Map<String, String>, String>> headers,
      @Named("write.context") final List<Function<Map<String, String>, String>> context,
      final Function<Map<String, String>, Body> body,
      @Nullable @Named("authentication.username") final String username,
      @Nullable @Named("authentication.password") final String password,
      @Nullable @Named("authentication.keystoneToken") final String keystoneToken,
      @Named("virtualhost") final boolean virtualHost) {

    final Map<String, Function<Map<String, String>, String>> queryParameters =
        Collections.emptyMap();

    return createRequestSupplier(Operation.WRITE, id, Method.PUT, scheme, host, port, uriRoot,
        container, object, queryParameters, headers, context, body, username, password,
        keystoneToken, virtualHost);
  }

  @Provides
  @Singleton
  @Named("overwrite")
  public Supplier<Request> provideOverwrite(
      @Named("request.id") final Function<Map<String, String>, String> id, final Api api,
      final Scheme scheme, @OverwriteHost final Function<Map<String, String>, String> host,
      @Nullable @Named("port") final Integer port,
      @Nullable @Named("uri.root") final String uriRoot,
      @Named("container") final Function<Map<String, String>, String> container,
      @Nullable @OverwriteObjectName final Function<Map<String, String>, String> object,
      @OverwriteHeaders final Map<String, Function<Map<String, String>, String>> headers,
      @Named("overwrite.context") final List<Function<Map<String, String>, String>> context,
      final Function<Map<String, String>, Body> body,
      @Nullable @Named("authentication.username") final String username,
      @Nullable @Named("authentication.password") final String password,
      @Nullable @Named("authentication.keystoneToken") final String keystoneToken,
      @Named("virtualhost") final boolean virtualHost,
      @Named("overwrite.weight") final double overwriteWeight) throws Exception {
    // SOH needs to use a special response consumer to extract the returned object id
    if (Api.SOH == api && overwriteWeight > 0.0) {
      throw new Exception("Overwrites are not compatible with SOH");
    }

    final Map<String, Function<Map<String, String>, String>> queryParameters =
        Collections.emptyMap();

    return createRequestSupplier(Operation.OVERWRITE, id, Method.PUT, scheme, host, port, uriRoot,
        container, object, queryParameters, headers, context, body, username, password,
        keystoneToken, virtualHost);
  }

  @Provides
  @Singleton
  @Named("read")
  public Supplier<Request> provideRead(
      @Named("request.id") final Function<Map<String, String>, String> id, final Scheme scheme,
      @ReadHost final Function<Map<String, String>, String> host,
      @Nullable @Named("port") final Integer port,
      @Nullable @Named("uri.root") final String uriRoot,
      @Named("container") final Function<Map<String, String>, String> container,
      @Nullable @ReadObjectName final Function<Map<String, String>, String> object,
      @ReadHeaders final Map<String, Function<Map<String, String>, String>> headers,
      @Named("read.context") final List<Function<Map<String, String>, String>> context,
      @Nullable @Named("authentication.username") final String username,
      @Nullable @Named("authentication.password") final String password,
      @Nullable @Named("authentication.keystoneToken") final String keystoneToken,
      @Named("virtualhost") final boolean virtualHost) {

    final Map<String, Function<Map<String, String>, String>> queryParameters =
        Collections.emptyMap();

    final Supplier<Body> bodySupplier = Suppliers.of(Bodies.none());
    final Function<Map<String, String>, Body> body = MoreFunctions.forSupplier(bodySupplier);

    return createRequestSupplier(Operation.READ, id, Method.GET, scheme, host, port, uriRoot,
        container, object, queryParameters, headers, context, body, username, password,
        keystoneToken, virtualHost);
  }

  @Provides
  @Singleton
  @Named("metadata")
  public Supplier<Request> provideMetadata(
      @Named("request.id") final Function<Map<String, String>, String> id, final Scheme scheme,
      @MetadataHost final Function<Map<String, String>, String> host,
      @Nullable @Named("port") final Integer port,
      @Nullable @Named("uri.root") final String uriRoot,
      @Named("container") final Function<Map<String, String>, String> container,
      @Nullable @MetadataObjectName final Function<Map<String, String>, String> object,
      @MetadataHeaders final Map<String, Function<Map<String, String>, String>> headers,
      @Named("metadata.context") final List<Function<Map<String, String>, String>> context,
      @Nullable @Named("authentication.username") final String username,
      @Nullable @Named("authentication.password") final String password,
      @Nullable @Named("authentication.keystoneToken") final String keystoneToken,
      @Named("virtualhost") final boolean virtualHost) {

    final Map<String, Function<Map<String, String>, String>> queryParameters =
        Collections.emptyMap();

    final Supplier<Body> bodySupplier = Suppliers.of(Bodies.none());
    final Function<Map<String, String>, Body> body = MoreFunctions.forSupplier(bodySupplier);

    return createRequestSupplier(Operation.METADATA, id, Method.HEAD, scheme, host, port, uriRoot,
        container, object, queryParameters, headers, context, body, username, password,
        keystoneToken, virtualHost);
  }

  @Provides
  @Singleton
  @Named("delete")
  public Supplier<Request> provideDelete(
      @Named("request.id") final Function<Map<String, String>, String> id, final Scheme scheme,
      @DeleteHost final Function<Map<String, String>, String> host,
      @Nullable @Named("port") final Integer port,
      @Nullable @Named("uri.root") final String uriRoot,
      @Named("container") final Function<Map<String, String>, String> container,
      @Nullable @DeleteObjectName final Function<Map<String, String>, String> object,
      @DeleteHeaders final Map<String, Function<Map<String, String>, String>> headers,
      @Named("delete.context") final List<Function<Map<String, String>, String>> context,
      @Nullable @Named("authentication.username") final String username,
      @Nullable @Named("authentication.password") final String password,
      @Nullable @Named("authentication.keystoneToken") final String keystoneToken,
      @Named("virtualhost") final boolean virtualHost) {

    final Map<String, Function<Map<String, String>, String>> queryParameters =
        Collections.emptyMap();

    final Supplier<Body> bodySupplier = Suppliers.of(Bodies.none());
    final Function<Map<String, String>, Body> body = MoreFunctions.forSupplier(bodySupplier);

    return createRequestSupplier(Operation.DELETE, id, Method.DELETE, scheme, host, port, uriRoot,
        container, object, queryParameters, headers, context, body, username, password,
        keystoneToken, virtualHost);
  }

  @Provides
  @Singleton
  @Named("list")
  public Supplier<Request> provideList(
      @Named("request.id") final Function<Map<String, String>, String> id, final Api api,
      final Scheme scheme, @ListHost final Function<Map<String, String>, String> host,
      @Nullable @Named("port") final Integer port,
      @Nullable @Named("uri.root") final String uriRoot,
      @ListQueryParameters final Map<String, Function<Map<String, String>, String>> queryParameters,
      @Named("container") final Function<Map<String, String>, String> container,
      @ListHeaders final Map<String, Function<Map<String, String>, String>> headers,
      @Named("list.context") final List<Function<Map<String, String>, String>> context,
      @Nullable @Named("authentication.username") final String username,
      @Nullable @Named("authentication.password") final String password,
      @Nullable @Named("authentication.keystoneToken") final String keystoneToken,
      @Named("virtualhost") final boolean virtualHost) throws Exception {

    final Supplier<Body> bodySupplier = Suppliers.of(Bodies.none());
    final Function<Map<String, String>, Body> body = MoreFunctions.forSupplier(bodySupplier);

    return createRequestSupplier(Operation.LIST, id, Method.GET, scheme, host, port, uriRoot,
        container, null, queryParameters, headers, context, body, username, password, keystoneToken,
        virtualHost);
  }

  private Supplier<Request> createRequestSupplier(final Operation operation,
      @Named("request.id") final Function<Map<String, String>, String> id, final Method method,
      final Scheme scheme, final Function<Map<String, String>, String> host, final Integer port,
      final String uriRoot, final Function<Map<String, String>, String> container,
      final Function<Map<String, String>, String> object,
      final Map<String, Function<Map<String, String>, String>> queryParameters,
      final Map<String, Function<Map<String, String>, String>> headers,
      final List<Function<Map<String, String>, String>> context,
      final Function<Map<String, String>, Body> body, final String username, final String password,
      final String keystoneToken, final boolean virtualHost) {

    return new RequestSupplier(operation, id, method, scheme, host, port, uriRoot, container,
        object, queryParameters, false, headers, context, username, password, keystoneToken, body,
        virtualHost);
  }
}
