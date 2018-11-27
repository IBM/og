/*
 * Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.guice;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;
import javax.inject.Named;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.google.inject.spi.ProvisionListener;
import com.google.inject.util.Providers;
import com.ibm.og.api.AuthType;
import com.ibm.og.api.Body;
import com.ibm.og.api.BodySource;
import com.ibm.og.api.Client;
import com.ibm.og.api.DataType;
import com.ibm.og.api.Method;
import com.ibm.og.api.Operation;
import com.ibm.og.api.Request;
import com.ibm.og.client.ApacheClient;
import com.ibm.og.guice.annotation.ContainerCreateHeaders;
import com.ibm.og.guice.annotation.ContainerCreateHost;
import com.ibm.og.guice.annotation.ContainerListHeaders;
import com.ibm.og.guice.annotation.ContainerListHost;
import com.ibm.og.guice.annotation.DeleteHeaders;
import com.ibm.og.guice.annotation.DeleteHost;
import com.ibm.og.guice.annotation.DeleteObjectName;
import com.ibm.og.guice.annotation.GetContainerLifecycleHeaders;
import com.ibm.og.guice.annotation.GetContainerProtectionHeaders;
import com.ibm.og.guice.annotation.ListHeaders;
import com.ibm.og.guice.annotation.ListHost;
import com.ibm.og.guice.annotation.MetadataHeaders;
import com.ibm.og.guice.annotation.MetadataHost;
import com.ibm.og.guice.annotation.MetadataObjectName;
import com.ibm.og.guice.annotation.MultiPartWriteBody;
import com.ibm.og.guice.annotation.MultipartWriteHeaders;
import com.ibm.og.guice.annotation.MultipartWriteHost;
import com.ibm.og.guice.annotation.MultipartWriteObjectName;
import com.ibm.og.guice.annotation.ObjectRestoreHeaders;
import com.ibm.og.guice.annotation.OverwriteBody;
import com.ibm.og.guice.annotation.OverwriteHeaders;
import com.ibm.og.guice.annotation.OverwriteHost;
import com.ibm.og.guice.annotation.OverwriteObjectName;
import com.ibm.og.guice.annotation.PutContainerLifecycleHeaders;
import com.ibm.og.guice.annotation.PutContainerProtectionHeaders;
import com.ibm.og.guice.annotation.ReadHeaders;
import com.ibm.og.guice.annotation.ReadHost;
import com.ibm.og.guice.annotation.ReadObjectName;
import com.ibm.og.guice.annotation.SourceReadObjectName;
import com.ibm.og.guice.annotation.WriteBody;
import com.ibm.og.guice.annotation.WriteCopyHeaders;
import com.ibm.og.guice.annotation.WriteHeaders;
import com.ibm.og.guice.annotation.WriteHost;
import com.ibm.og.guice.annotation.WriteObjectName;
import com.ibm.og.http.Api;
import com.ibm.og.http.BasicAuth;
import com.ibm.og.http.Bodies;
import com.ibm.og.http.Credential;
import com.ibm.og.http.Headers;
import com.ibm.og.http.HttpAuth;
import com.ibm.og.http.HttpUtil;
import com.ibm.og.http.IAMTokenAuth;
import com.ibm.og.http.NoneAuth;
import com.ibm.og.http.QueryParameters;
import com.ibm.og.http.ResponseBodyConsumer;
import com.ibm.og.http.Scheme;
import com.ibm.og.json.ChoiceConfig;
import com.ibm.og.json.ClientConfig;
import com.ibm.og.json.ConcurrencyConfig;
import com.ibm.og.json.ConcurrencyType;
import com.ibm.og.json.ContainerConfig;
import com.ibm.og.json.CredentialSource;
import com.ibm.og.json.ObjectDelimiterConfig;
import com.ibm.og.json.FailingConditionsConfig;
import com.ibm.og.json.FilesizeConfig;
import com.ibm.og.json.LegalHold;
import com.ibm.og.json.OGConfig;
import com.ibm.og.json.ObjectConfig;
import com.ibm.og.json.ObjectManagerConfig;
import com.ibm.og.json.OperationConfig;
import com.ibm.og.json.RetentionConfig;
import com.ibm.og.json.SelectionConfig;
import com.ibm.og.json.SelectionType;
import com.ibm.og.json.StoppingConditionsConfig;
import com.ibm.og.object.AbstractObjectNameConsumer;
import com.ibm.og.object.DeleteObjectConsumer;
import com.ibm.og.object.DeleteObjectLegalHoldConsumer;
import com.ibm.og.object.ExtendRetentionObjectNameConsumer;
import com.ibm.og.object.MetadataObjectNameConsumer;
import com.ibm.og.object.MultipartWriteObjectNameConsumer;
import com.ibm.og.object.ObjectManager;
import com.ibm.og.object.OverwriteObjectNameConsumer;
import com.ibm.og.object.RandomObjectPopulator;
import com.ibm.og.object.ReadObjectLegalHoldConsumer;
import com.ibm.og.object.ReadObjectNameConsumer;
import com.ibm.og.object.WriteCopyObjectNameConsumer;
import com.ibm.og.object.WriteLegalHoldObjectNameConsumer;
import com.ibm.og.object.WriteObjectNameConsumer;
import com.ibm.og.openstack.KeystoneAuth;
import com.ibm.og.s3.MultipartRequestSupplier;
import com.ibm.og.s3.S3ListResponseBodyConsumer;
import com.ibm.og.s3.S3MultipartWriteResponseBodyConsumer;
import com.ibm.og.s3.v2.AWSV2Auth;
import com.ibm.og.s3.v4.AWSV4Auth;
import com.ibm.og.scheduling.ConcurrentRequestScheduler;
import com.ibm.og.scheduling.RequestRateScheduler;
import com.ibm.og.scheduling.Scheduler;
import com.ibm.og.soh.SOHWriteResponseBodyConsumer;
import com.ibm.og.statistic.Counter;
import com.ibm.og.statistic.Statistics;
import com.ibm.og.supplier.CredentialGetterFunction;
import com.ibm.og.supplier.DeleteObjectNameFunction;
import com.ibm.og.supplier.LegalholdObjectNameFunction;
import com.ibm.og.supplier.MetadataObjectNameFunction;
import com.ibm.og.supplier.ObjectRetentionExtensionFunction;
import com.ibm.og.supplier.RandomPercentageSupplier;
import com.ibm.og.supplier.RandomSupplier;
import com.ibm.og.supplier.ReadObjectNameFunction;
import com.ibm.og.supplier.RequestSupplier;
import com.ibm.og.supplier.SourceReadObjectNameFunction;
import com.ibm.og.supplier.Suppliers;
import com.ibm.og.supplier.UUIDObjectNameFunction;
import com.ibm.og.test.LoadTest;
import com.ibm.og.test.LoadTestSubscriberExceptionHandler;
import com.ibm.og.test.RequestManager;
import com.ibm.og.test.SimpleRequestManager;
import com.ibm.og.test.condition.ConcurrentRequestCondition;
import com.ibm.og.test.condition.CounterCondition;
import com.ibm.og.test.condition.RuntimeCondition;
import com.ibm.og.test.condition.StatusCodeCondition;
import com.ibm.og.test.condition.TestCondition;
import com.ibm.og.util.Context;
import com.ibm.og.util.Distribution;
import com.ibm.og.util.Distributions;
import com.ibm.og.util.MoreFunctions;
import com.ibm.og.util.SizeUnit;
import com.ibm.og.util.Version;
import com.ibm.og.util.json.type.DistributionType;

/**
 * A guice configuration module for wiring up all OG test components
 *
 * @since 1.0
 */

public class OGModule extends AbstractModule {
  private final OGConfig config;
  private static final String SOH_PUT_OBJECT = "soh.put_object";
  private static final String S3_MULTIPART = "s3.multipart";
  private static final String S3_LIST = "s3.list";
  private final LoadTestSubscriberExceptionHandler handler;
  private final EventBus eventBus;
  final byte[] aesKey = SSECustomerKey();

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
    bindConstant().annotatedWith(Names.named("write.sseCDestination"))
        .to(this.config.write.sseCDestination);
    bindConstant().annotatedWith(Names.named("write.contentMd5")).to(this.config.write.contentMd5);
    bindConstant().annotatedWith(Names.named("overwrite.weight")).to(this.config.overwrite.weight);
    bindConstant().annotatedWith(Names.named("overwrite.sseCDestination"))
        .to(this.config.overwrite.sseCDestination);
    bindConstant().annotatedWith(Names.named("overwrite.contentMd5"))
        .to(this.config.overwrite.contentMd5);
    bindConstant().annotatedWith(Names.named("read.weight")).to(this.config.read.weight);
    bindConstant().annotatedWith(Names.named("read.sseCSource")).to(this.config.read.sseCSource);
    bindConstant().annotatedWith(Names.named("metadata.weight")).to(this.config.metadata.weight);
    bindConstant().annotatedWith(Names.named("metadata.sseCSource"))
        .to(this.config.metadata.sseCSource);
    bindConstant().annotatedWith(Names.named("delete.weight")).to(this.config.delete.weight);
    bindConstant().annotatedWith(Names.named("list.weight")).to(this.config.list.weight);
    bindConstant().annotatedWith(Names.named("containerList.weight"))
        .to(this.config.containerList.weight);
    bindConstant().annotatedWith(Names.named("containerCreate.weight"))
        .to(this.config.containerCreate.weight);
    bindConstant().annotatedWith(Names.named("multipartWrite.weight"))
        .to(this.config.multipartWrite.weight);
    bindConstant().annotatedWith(Names.named("multipartWrite.sseCDestination"))
        .to(this.config.multipartWrite.sseCDestination);
    bindConstant().annotatedWith(Names.named("multipartWrite.contentMd5")).to(this.config.multipartWrite.contentMd5);
    bindConstant().annotatedWith(Names.named("writeCopy.weight")).to(this.config.writeCopy.weight);
    bindConstant().annotatedWith(Names.named("writeCopy.sseCSource"))
        .to(this.config.writeCopy.sseCSource);
    bindConstant().annotatedWith(Names.named("writeCopy.sseCDestination"))
        .to(this.config.writeCopy.sseCDestination);
    bindConstant().annotatedWith(Names.named("write_legalhold.weight"))
        .to(this.config.writeLegalhold.weight);
    bindConstant().annotatedWith(Names.named("read_legalhold.weight"))
        .to(this.config.readLegalhold.weight);
    bindConstant().annotatedWith(Names.named("delete_legalhold.weight"))
        .to(this.config.deleteLegalhold.weight);
    bindConstant().annotatedWith(Names.named("extend_retention.weight"))
        .to(this.config.extendRetention.weight);
    bindConstant().annotatedWith(Names.named("virtualhost")).to(this.config.virtualHost);
    bindConstant().annotatedWith(Names.named("octalNamingMode")).to(this.config.octalNamingMode);
    bindConstant().annotatedWith(Names.named("multipartWrite.targetSessions"))
        .to(this.config.multipartWrite.upload.targetSessions);
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
    bindConstant().annotatedWith(Names.named("objectRestore.weight")).to(this.config.objectRestore.weight);
    bindConstant().annotatedWith(Names.named("putContainerLifecycle.weight")).to(this.config.putContainerLifecycle.weight);
    bindConstant().annotatedWith(Names.named("getContainerLifecycle.weight")).to(this.config.getContainerLifecycle.weight);
    bindConstant().annotatedWith(Names.named("putContainerProtection.weight")).to(this.config.putContainerProtection.weight);
    bindConstant().annotatedWith(Names.named("getContainerProtection.weight")).to(this.config.getContainerProtection.weight);


    // FIXME create something like MoreProviders.notNull as a variant of Providers.of which does a
    // null check at creation time, with a custom error message; replace all uses of this pattern
    bind(ConcurrencyConfig.class).toProvider(new Provider<ConcurrencyConfig>() {
      @Override
      public ConcurrencyConfig get() {
        return checkNotNull(OGModule.this.config.concurrency, "concurrency must not be null");
      }
    });
    bind(StoppingConditionsConfig.class).toInstance(this.config.stoppingConditions);
    bind(FailingConditionsConfig.class).toInstance(this.config.failingConditions);
    bindConstant().annotatedWith(Names.named("shutdownImmediate"))
        .to(this.config.shutdownImmediate);
    bindConstant().annotatedWith(Names.named("statsLogInterval"))
            .to(this.config.statsLogInterval);
    checkArgument((this.config.statsLogInterval == -1 || this.config.statsLogInterval >= 10),
            "Stats Log Interval must be greater than or equal to 10 seconds");

    final MapBinder<AuthType, HttpAuth> httpAuthBinder =
        MapBinder.newMapBinder(binder(), AuthType.class, HttpAuth.class);
    httpAuthBinder.addBinding(AuthType.NONE).to(NoneAuth.class);
    httpAuthBinder.addBinding(AuthType.BASIC).to(BasicAuth.class);
    httpAuthBinder.addBinding(AuthType.AWSV2).to(AWSV2Auth.class);
    httpAuthBinder.addBinding(AuthType.AWSV4).to(AWSV4Auth.class);
    httpAuthBinder.addBinding(AuthType.KEYSTONE).to(KeystoneAuth.class);
    httpAuthBinder.addBinding(AuthType.IAM).to(IAMTokenAuth.class);

    final MapBinder<String, ResponseBodyConsumer> responseBodyConsumers =
        MapBinder.newMapBinder(binder(), String.class, ResponseBodyConsumer.class);
    responseBodyConsumers.addBinding(SOH_PUT_OBJECT).to(SOHWriteResponseBodyConsumer.class);
    responseBodyConsumers.addBinding(S3_MULTIPART).to(S3MultipartWriteResponseBodyConsumer.class);
    responseBodyConsumers.addBinding(S3_LIST).to(S3ListResponseBodyConsumer.class);

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
      final StoppingConditionsConfig stoppingConditionsConfig,
      final FailingConditionsConfig failingConditionsConfig) {
    checkNotNull(test);
    checkNotNull(stats);
    // Stopping conditions
    checkNotNull(stoppingConditionsConfig);
    checkArgument(stoppingConditionsConfig.operations >= 0, "operations must be >= 0 [%s]",
        stoppingConditionsConfig.operations);
    checkArgument(stoppingConditionsConfig.runtime >= 0.0, "runtime must be >= 0.0 [%s]",
        stoppingConditionsConfig.runtime);
    checkNotNull(stoppingConditionsConfig.runtimeUnit);
    checkNotNull(stoppingConditionsConfig.statusCodes);
    for (final Entry<Integer, Integer> sc : stoppingConditionsConfig.statusCodes.entrySet()) {
      checkArgument(sc.getValue() >= 0.0, "status code [%s] value must be >= 0.0 [%s]", sc.getKey(),
          sc.getValue());
    }
    // Failing conditions
    checkNotNull(failingConditionsConfig);
    checkArgument(failingConditionsConfig.operations >= 0, "operations must be >= 0 [%s]",
        failingConditionsConfig.operations);
    checkArgument(failingConditionsConfig.runtime >= 0.0, "runtime must be >= 0.0 [%s]",
        failingConditionsConfig.runtime);
    checkNotNull(failingConditionsConfig.runtimeUnit);
    checkArgument(failingConditionsConfig.concurrentRequests >= 0,
        "concurrentRequests must be >= 0 [%s]", failingConditionsConfig.concurrentRequests);
    checkNotNull(failingConditionsConfig.statusCodes);
    for (final Entry<Integer, Integer> sc : failingConditionsConfig.statusCodes.entrySet()) {
      checkArgument(sc.getValue() >= 0.0, "status code [%s] value must be >= 0.0 [%s]", sc.getKey(),
          sc.getValue());
    }

    final List<TestCondition> conditions = Lists.newArrayList();

    // Stopping conditions
    if (stoppingConditionsConfig.operations > 0) {
      conditions.add(new CounterCondition(Operation.ALL, Counter.OPERATIONS,
          stoppingConditionsConfig.operations, test, stats, false));
    }

    final Map<Integer, Integer> stoppingSCMap = stoppingConditionsConfig.statusCodes;
    for (final Entry<Integer, Integer> sc : stoppingSCMap.entrySet()) {
      if (sc.getValue() > 0) {
        conditions.add(
            new StatusCodeCondition(Operation.ALL, sc.getKey(), sc.getValue(), test, stats, false));
      }
    }

    if (stoppingConditionsConfig.runtime > 0) {
      conditions.add(new RuntimeCondition(test, stoppingConditionsConfig.runtime,
          stoppingConditionsConfig.runtimeUnit, false));
    }

    // Failing conditions
    if (failingConditionsConfig.operations > 0) {
      conditions.add(new CounterCondition(Operation.ALL, Counter.OPERATIONS,
          failingConditionsConfig.operations, test, stats, true));
    }

    final Map<Integer, Integer> failingSCMap = failingConditionsConfig.statusCodes;
    for (final Entry<Integer, Integer> sc : failingSCMap.entrySet()) {
      if (sc.getValue() > 0) {
        conditions.add(
            new StatusCodeCondition(Operation.ALL, sc.getKey(), sc.getValue(), test, stats, true));
      }
    }

    if (failingConditionsConfig.runtime > 0) {
      conditions.add(new RuntimeCondition(test, failingConditionsConfig.runtime,
          failingConditionsConfig.runtimeUnit, true));
    }

    // maximum concurrent requests only makes sense in the context of an ops test, so check for that
    if (failingConditionsConfig.concurrentRequests > 0 && concurrency.type == ConcurrencyType.OPS) {
      conditions.add(new ConcurrentRequestCondition(Operation.ALL,
          failingConditionsConfig.concurrentRequests, test, stats, true));
    }

    for (final TestCondition condition : conditions) {
      eventBus.register(condition);
    }

    return conditions;
  }

  public Long provideTestRetentionConfig(final RetentionConfig rc) {

    final Map<String, String> context = Maps.newHashMap();
    if (rc.expiry != null) {
      final SelectionConfig<RetentionConfig> rcSelection = new SelectionConfig<RetentionConfig>();
      rcSelection.choices.add(new ChoiceConfig<RetentionConfig>(rc));
      final Function<Map<String, String>, Long> retentionFunction =
          this.provideRetention(rcSelection);
      return retentionFunction.apply(context);
    } else {
      return -1L;
    }
  }

  public Long provideTestRetentionExtensionConfig(final long currentRetention, final RetentionConfig rc) {

    final Map<String, String> context = Maps.newHashMap();
    context.put(Context.X_OG_OBJECT_RETENTION, Long.toString(currentRetention));
    if (rc.expiry != null) {
      final SelectionConfig<RetentionConfig> rcSelection = new SelectionConfig<RetentionConfig>();
      rcSelection.choices.add(new ChoiceConfig<RetentionConfig>(rc));
      final Function<Map<String, String>, Long> retentionFunction =
              this.provideRetentionExtension(rcSelection);
      return retentionFunction.apply(context);
    } else {
       return Long.parseLong(context.get(Context.X_OG_OBJECT_RETENTION));
    }
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

  @Provides
  @Singleton
  @ContainerListHost
  public Function<Map<String, String>, String> provideContainerListHost(
      @Named("host") final Function<Map<String, String>, String> host) {
    return provideHost(this.config.containerList, host);
  }

  @Provides
  @Singleton
  @ContainerCreateHost
  public Function<Map<String, String>, String> provideContainerCreateHost(
      @Named("host") final Function<Map<String, String>, String> host) {
    return provideHost(this.config.containerCreate, host);
  }

  @Provides
  @Singleton
  @MultipartWriteHost
  public Function<Map<String, String>, String> provideMultipartWriteHost(
      @Named("host") final Function<Map<String, String>, String> host) {
    return provideHost(this.config.multipartWrite, host);
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

  private void checkContainerObjectConfig(final OperationConfig operationConfig) throws Exception {
    if ((operationConfig.container.maxSuffix != -1 || operationConfig.container.minSuffix != -1)
        && operationConfig.object.choices.isEmpty()  && operationConfig.weight > 0.0) {
        //TODO: fix the object choices check
      throw new Exception(
          "Must specify ObjectConfig prefix if using min/max suffix in container config");
    }
  }

  @Provides
  @Singleton
  @Named("write.container")
  public Function<Map<String, String>, String> provideWriteContainer() {
    if (this.config.write.container.prefix != null) {
      return provideContainer(this.config.write.container);
    } else {
      return provideContainer(this.config.container);
    }
  }

  @Provides
  @Singleton
  @Named("writeCopy.container")
  public Function<Map<String, String>, String> provideWriteCopyContainer() {
    if (this.config.writeCopy.container.prefix != null) {
      return provideTargetContainer(this.config.writeCopy.container);
    } else {
      return provideTargetContainer(this.config.container);
    }
  }


  @Provides
  @Singleton
  @Named("writeCopy.sourceContainer")
  public Function<Map<String, String>, String> provideWriteCopySourceContainer() {
    if (this.config.writeCopy.sourceContainer.prefix != null) {
      return provideSourceContainer(this.config.writeCopy.sourceContainer);
    } else {
      return provideSourceContainer(this.config.container);
    }
  }

  @Provides
  @Singleton
  @Named("overwrite.container")
  public Function<Map<String, String>, String> provideOverwriteContainer() throws Exception {
    if (this.config.overwrite.container.prefix != null) {
      checkContainerObjectConfig(this.config.overwrite);
      return provideContainer(this.config.overwrite.container);
    } else {
      return provideContainer(this.config.container);
    }
  }

  @Provides
  @Singleton
  @Named("read.container")
  public Function<Map<String, String>, String> provideReadContainer() throws Exception {
    if (this.config.read.container.prefix != null) {
      checkContainerObjectConfig(this.config.read);
      return provideContainer(this.config.read.container);
    } else {
      return provideContainer(this.config.container);
    }
  }

  @Provides
  @Singleton
  @Named("metadata.container")
  public Function<Map<String, String>, String> provideMetadataContainer() throws Exception {
    if (this.config.metadata.container.prefix != null) {
      checkContainerObjectConfig(this.config.metadata);
      return provideContainer(this.config.metadata.container);
    } else {
      return provideContainer(this.config.container);
    }
  }

  @Provides
  @Singleton
  @Named("delete.container")
  public Function<Map<String, String>, String> provideDeleteContainer() throws Exception {
    if (this.config.delete.container.prefix != null) {
      checkContainerObjectConfig(this.config.delete);
      return provideContainer(this.config.delete.container);
    } else {
      return provideContainer(this.config.container);
    }
  }


  @Provides
  @Singleton
  @Named("containerCreate.container")
  public Function<Map<String, String>, String> provideContainerCreateContainer() {
    if (this.config.containerCreate.container.prefix != null) {
      return provideContainer(this.config.containerCreate.container);
    } else {
      return provideContainer(this.config.container);
    }
  }

  @Provides
  @Singleton
  @Named("multipartWrite.container")
  public Function<Map<String, String>, String> provideMultipartWriteContainer() {
    if (this.config.multipartWrite.container.prefix != null) {
      return provideContainer(this.config.multipartWrite.container);
    } else {
      return provideContainer(this.config.container);
    }
  }


  @Provides
  @Singleton
  @Named("objectRestore.container")
  public Function<Map<String, String>, String> provideObjectRestoreContainer() throws Exception {
    if (this.config.objectRestore.container.prefix != null) {
      checkContainerObjectConfig(this.config.objectRestore);
      return provideContainer(this.config.objectRestore.container);
    } else {
      return provideContainer(this.config.container);
    }
  }

  @Provides
  @Singleton
  @Named("putContainerLifecycle.container")
  public Function<Map<String, String>, String> providePutContainerLifecycleContainer() {
    if (this.config.putContainerLifecycle.container.prefix != null) {
      return provideContainer(this.config.putContainerLifecycle.container);
    } else {
      return provideContainer(this.config.container);
    }
  }

  @Provides
  @Singleton
  @Named("getContainerLifecycle.container")
  public Function<Map<String, String>, String> provideGetContainerLifecycleContainer() {
    if (this.config.getContainerLifecycle.container.prefix != null) {
      return provideContainer(this.config.getContainerLifecycle.container);
    } else {
      return provideContainer(this.config.container);
    }
  }

  @Provides
  @Singleton
  @Named("putContainerProtection.container")
  public Function<Map<String, String>, String> providePutContainerProtectionContainer() {
    if (this.config.putContainerProtection.container.prefix != null) {
      return provideContainer(this.config.putContainerProtection.container);
    } else {
      return provideContainer(this.config.container);
    }
  }

  @Provides
  @Singleton
  @Named("getContainerProtection.container")
  public Function<Map<String, String>, String> provideGetContainerProtectionContainer() {
    if (this.config.getContainerProtection.container.prefix != null) {
      return provideContainer(this.config.getContainerProtection.container);
    } else {
      return provideContainer(this.config.container);
    }
  }

  public Function<Map<String, String>, String> provideContainer(
      final ContainerConfig containerConfig) {
    final String container = checkNotNull(containerConfig.prefix);
    checkArgument(container.length() > 0, "container must not be empty string");

    final Supplier<Integer> suffixes = createContainerSuffixes(containerConfig);

    return new Function<Map<String, String>, String>() {

      @Override
      public String apply(final Map<String, String> input) {
        String suffix = input.get(Context.X_OG_CONTAINER_SUFFIX);
        if (suffix != null) {
          if (Integer.parseInt(suffix) == -1) {
            // use the container name provided without suffix
            input.put(Context.X_OG_CONTAINER_PREFIX, container);
            input.put(Context.X_OG_CONTAINER_NAME, container);
            return container;
          } else {
            final String containerName = container.concat(suffix);
            input.put(Context.X_OG_CONTAINER_PREFIX, container);
            input.put(Context.X_OG_CONTAINER_NAME, containerName);
            return container.concat(suffix);
          }
        } else {
          if (suffixes != null) {
            suffix = suffixes.get().toString();
            input.put(Context.X_OG_CONTAINER_SUFFIX, suffix);
            final String containerName = container.concat(suffix);
            input.put(Context.X_OG_CONTAINER_PREFIX, container);
            input.put(Context.X_OG_CONTAINER_NAME, containerName);
            return container.concat(suffix);
          } else {
            input.put(Context.X_OG_CONTAINER_SUFFIX, "-1");
            // use the container name provided without suffix
            input.put(Context.X_OG_CONTAINER_PREFIX, container);
            input.put(Context.X_OG_CONTAINER_NAME, container);
            return container;
          }
        }
      }
    };
  }

  public Function<Map<String, String>, String> provideTargetContainer(
          final ContainerConfig containerConfig) {
    final String container = checkNotNull(containerConfig.prefix);
    checkArgument(container.length() > 0, "container must not be empty string");

    final Supplier<Integer> suffixes = createContainerSuffixes(containerConfig);

    return new Function<Map<String, String>, String>() {

      @Override
      public String apply(final Map<String, String> input) {
        String suffix = input.get(Context.X_OG_SSE_SOURCE_OBJECT_CONTAINER_SUFFIX);
        if (suffix != null) {
          if (Integer.parseInt(suffix) == -1) {
            // use the container name provided without suffix
            input.put(Context.X_OG_CONTAINER_PREFIX, container);
            input.put(Context.X_OG_CONTAINER_NAME, container);
            //target container suffix
            input.put(Context.X_OG_CONTAINER_SUFFIX, suffix);
            return container;
          } else {
            final String containerName = container.concat(suffix);
            input.put(Context.X_OG_CONTAINER_PREFIX, container);
            input.put(Context.X_OG_CONTAINER_NAME, containerName);
            //target container suffix
            input.put(Context.X_OG_CONTAINER_SUFFIX, suffix);
            return container.concat(suffix);
          }
        } else {
          if (suffixes != null) {
            suffix = suffixes.get().toString();
            input.put(Context.X_OG_CONTAINER_SUFFIX, suffix);
            final String containerName = container.concat(suffix);
            input.put(Context.X_OG_CONTAINER_PREFIX, container);
            input.put(Context.X_OG_CONTAINER_NAME, containerName);
            return container.concat(suffix);
          } else {
            input.put(Context.X_OG_CONTAINER_SUFFIX, "-1");
            // use the container name provided without suffix
            input.put(Context.X_OG_CONTAINER_PREFIX, container);
            input.put(Context.X_OG_CONTAINER_NAME, container);
            return container;
          }
        }
      }
    };
  }

  public Function<Map<String, String>, String> provideSourceContainer(
          final ContainerConfig containerConfig) {
    final String container = checkNotNull(containerConfig.prefix);
    checkArgument(container.length() > 0, "container must not be empty string");

    final Supplier<Integer> suffixes = createContainerSuffixes(containerConfig);

    return new Function<Map<String, String>, String>() {

      @Override
      public String apply(final Map<String, String> input) {
        //source read object context function populates Context.X_OG_SSE_SOURCE_OBJECT_CONTAINER_SUFFIX
        String suffix = input.get(Context.X_OG_SSE_SOURCE_OBJECT_CONTAINER_SUFFIX);
        if (suffix != null) {
          if (Integer.parseInt(suffix) == -1) {
            // use the container name provided without suffix
            input.put(Context.X_OG_SOURCE_CONTAINER_PREFIX, container);
            input.put(Context.X_OG_SOURCE_CONTAINER_NAME, container);
            input.put(Context.X_OG_SSE_SOURCE_OBJECT_CONTAINER_SUFFIX, "-1");
            return container;
          } else {
            final String containerName = container.concat(suffix);
            input.put(Context.X_OG_SOURCE_CONTAINER_PREFIX, container);
            input.put(Context.X_OG_SOURCE_CONTAINER_NAME, containerName);
            input.put(Context.X_OG_SSE_SOURCE_OBJECT_CONTAINER_SUFFIX, suffix);
            return container.concat(suffix);
          }
        } else {
          if (suffixes != null) {
            suffix = suffixes.get().toString();
            input.put(Context.X_OG_SSE_SOURCE_OBJECT_CONTAINER_SUFFIX, suffix);
            final String containerName = container.concat(suffix);
            input.put(Context.X_OG_SOURCE_CONTAINER_PREFIX, container);
            input.put(Context.X_OG_SOURCE_CONTAINER_NAME, containerName);
            return container.concat(suffix);
          } else {
            input.put(Context.X_OG_SSE_SOURCE_OBJECT_CONTAINER_SUFFIX, "-1");
            // use the container name provided without suffix
            input.put(Context.X_OG_SOURCE_CONTAINER_PREFIX, container);
            input.put(Context.X_OG_SOURCE_CONTAINER_NAME, container);
            return container;
          }
        }
      }
    };
  }


  @Provides
  @Singleton
  @Named("objectRestore.objectRestorePeriod")
  public Function<Map<String, String>, String> provideObjectRestorePeriod(final OperationConfig config) throws Exception {
    return (new Function<Map<String, String>, String>() {
      OperationConfig restoreOperationConfig = config;
      @Override
      public String apply(final Map<String, String> input) {
        input.put(Context.X_OG_OBJECT_RESTORE_PERIOD, this.restoreOperationConfig.objectRestorePeriod.toString());
        return this.restoreOperationConfig.objectRestorePeriod.toString();
      }
    });
  }

  @Provides
  @Singleton
  @Named("putContainerLifecycle.archiveTransitionPeriod")
  public Function<Map<String, String>, String> provideArchiveTransitionPeriod(final OperationConfig config) throws Exception {
    return (new Function<Map<String, String>, String>() {
      OperationConfig archiveTransitionPeriodConfig = config;
      @Override
      public String apply(final Map<String, String> input) {
        input.put(Context.X_OG_ARCHIVE_TRANSITION_PERIOD, this.archiveTransitionPeriodConfig.archiveTransitionPeriod.toString());
        return this.archiveTransitionPeriodConfig.archiveTransitionPeriod.toString();
      }
    });
  }

  @Provides
  @Singleton
  @Named("multipartWrite.partSize")
  public Function<Map<String, String>, Long> provideMultipartWritePartSize() {
    return providePartSize(this.config.multipartWrite);
  }

  private Function<Map<String, String>, Long> providePartSize(
      final OperationConfig operationConfig) {
    checkNotNull(operationConfig);

    final SelectionConfig<Long> operationPartSize = operationConfig.upload.partSize;
    if (operationPartSize != null && !operationPartSize.choices.isEmpty()) {
      return createPartSize(operationConfig.upload.partSize);
    }

    // default to 5242880 bytes
    final List<Long> partSizeList = Lists.newArrayList();
    partSizeList.add(5242880L);
    final Supplier<Long> partSizeSupplier = Suppliers.cycle(partSizeList);
    return MoreFunctions.forSupplier(partSizeSupplier);
  }

  private Function<Map<String, String>, Long> createPartSize(final SelectionConfig<Long> partSize) {
    checkNotNull(partSize);
    checkNotNull(partSize.selection);
    checkNotNull(partSize.choices);
    checkArgument(!partSize.choices.isEmpty(), "must specify at least one part");
    for (final ChoiceConfig<Long> choice : partSize.choices) {
      checkNotNull(choice);
      checkNotNull(choice.choice);
      checkArgument(choice.choice >= 5242880,
          "partSize must be greater than or equal to 5242880 bytes (5 MiB)");
    }

    if (SelectionType.ROUNDROBIN == partSize.selection) {
      final List<Long> partSizeList = Lists.newArrayList();
      for (final ChoiceConfig<Long> choice : partSize.choices) {
        partSizeList.add(choice.choice);
      }
      final Supplier<Long> partSizeSupplier = Suppliers.cycle(partSizeList);
      return MoreFunctions.forSupplier(partSizeSupplier);
    }

    final RandomSupplier.Builder<Long> wrc = Suppliers.random();
    for (final ChoiceConfig<Long> choice : partSize.choices) {
      wrc.withChoice(choice.choice, choice.weight);
    }
    final Supplier<Long> partSizeSupplier = wrc.build();
    return MoreFunctions.forSupplier(partSizeSupplier);
  }

  @Provides
  @Singleton
  @Named("multipartWrite.partsPerSession")
  public Function<Map<String, String>, Integer> provideMultipartWritePartsPerSession() {
    return providePartsPerSession(this.config.multipartWrite);
  }

  private Function<Map<String, String>, Integer> providePartsPerSession(
      final OperationConfig operationConfig) {
    checkNotNull(operationConfig);

    final SelectionConfig<Integer> operationPartsPerSession =
        operationConfig.upload.partsPerSession;
    if (operationPartsPerSession != null && !operationPartsPerSession.choices.isEmpty()) {
      return createPartsPerSession(operationConfig.upload.partsPerSession);
    }

    // default to Integer.MAX_VALUE parts per session
    final List<Integer> partsPerSessionList = Lists.newArrayList();
    partsPerSessionList.add(Integer.MAX_VALUE);
    final Supplier<Integer> partSizeSupplier = Suppliers.cycle(partsPerSessionList);
    return MoreFunctions.forSupplier(partSizeSupplier);
  }

  private Function<Map<String, String>, Integer> createPartsPerSession(
      final SelectionConfig<Integer> partsPerSession) {
    checkNotNull(partsPerSession);
    checkNotNull(partsPerSession.selection);
    checkNotNull(partsPerSession.choices);
    checkArgument(!partsPerSession.choices.isEmpty(), "must specify at least one part per session");
    for (final ChoiceConfig<Integer> choice : partsPerSession.choices) {
      checkNotNull(choice);
      checkNotNull(choice.choice);
      checkArgument(choice.choice >= 1, "partsPerSession must be greater than or equal to 1");
    }

    if (SelectionType.ROUNDROBIN == partsPerSession.selection) {
      final List<Integer> partsPerSessionList = Lists.newArrayList();
      for (final ChoiceConfig<Integer> choice : partsPerSession.choices) {
        partsPerSessionList.add(choice.choice);
      }
      final Supplier<Integer> partSizeSupplier = Suppliers.cycle(partsPerSessionList);
      return MoreFunctions.forSupplier(partSizeSupplier);
    }

    final RandomSupplier.Builder<Integer> wrc = Suppliers.random();
    for (final ChoiceConfig<Integer> choice : partsPerSession.choices) {
      wrc.withChoice(choice.choice, choice.weight);
    }
    final Supplier<Integer> partSizeSupplier = wrc.build();
    return MoreFunctions.forSupplier(partSizeSupplier);
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
  @SourceReadObjectName
  public Function<Map<String, String>, String> provideSSEReadObjectName() {
    return MoreFunctions.keyLookup(Context.X_OG_SSE_SOURCE_OBJECT_NAME);
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

  @Provides
  @Singleton
  @MultipartWriteObjectName
  public Function<Map<String, String>, String> provideMultipartWriteObjectName() {
    return MoreFunctions.keyLookup(Context.X_OG_OBJECT_NAME);
  }


  private Function<Map<String, String>, String> provideSourceObject(
      final OperationConfig operationConfig) {
    checkNotNull(operationConfig);

    final ObjectConfig objectConfig = checkNotNull(operationConfig.sourceObject);
    final String prefix = checkNotNull(objectConfig.prefix);
    final Supplier<Long> suffixes = createObjectSuffixes(objectConfig);
    return new Function<Map<String, String>, String>() {
      @Override
      public String apply(final Map<String, String> context) {
        final String objectName = prefix + suffixes.get();
        context.put(Context.X_OG_SSE_SOURCE_OBJECT_NAME, objectName);
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

  private Supplier<Long> createLegalHoldSuffixes(final ObjectConfig config) {
    if (SelectionType.ROUNDROBIN == config.selection) {
      return Suppliers.cycle(LegalHold.MIN_SUFFIX, LegalHold.MAX_SUFFIX);
    } else {
      return Suppliers.random(LegalHold.MIN_SUFFIX, LegalHold.MAX_SUFFIX);
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
    consumers.add(new MultipartWriteObjectNameConsumer(objectManager, sc));
    consumers.add(new WriteCopyObjectNameConsumer(objectManager, sc));
    Set<Integer> deleteStatusCodes = HttpUtil.DELETE_HANDLING_STATUS_CODES;
    consumers.add(new DeleteObjectConsumer(objectManager, deleteStatusCodes));
    // add status code range (400, 451) for legalhold operations.
    // while doing legalhold operation object is temporarily removed and stored in
    // a separate cache. After the response is received object state is updated and the object is
    // added back in the object manager
    final Set<Integer> legalHoldsSc = Sets.newHashSet();
    legalHoldsSc.addAll(sc);
    legalHoldsSc.addAll(ContiguousSet.create(Range.closed(400, 451), DiscreteDomain.integers()));
    consumers.add(new WriteLegalHoldObjectNameConsumer(objectManager, legalHoldsSc));
    consumers.add(new ReadObjectLegalHoldConsumer(objectManager, legalHoldsSc));
    consumers.add(new DeleteObjectLegalHoldConsumer(objectManager, legalHoldsSc));
    // object retention extension consumer
    final Set<Integer> retentionExtensionSc = Sets.newHashSet();
    retentionExtensionSc.addAll(sc);
    retentionExtensionSc.addAll(ContiguousSet.create(Range.closed(400, 451), DiscreteDomain.integers()));
    consumers.add(new ExtendRetentionObjectNameConsumer(objectManager, legalHoldsSc));
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
  @WriteCopyHeaders
  public Map<String, Function<Map<String, String>, String>> provideWriteCopyHeaders() {
    return provideHeaders(this.config.writeCopy.headers);
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

  @Provides
  @Singleton
  @ContainerListHeaders
  public Map<String, Function<Map<String, String>, String>> provideContainerListHeaders() {
    return provideHeaders(this.config.containerList.headers);
  }

  @Provides
  @Singleton
  @ContainerCreateHeaders
  public Map<String, Function<Map<String, String>, String>> provideContainerCreateHeaders() {
    return provideHeaders(this.config.containerCreate.headers);
  }

  @Provides
  @Singleton
  @MultipartWriteHeaders
  public Map<String, Function<Map<String, String>, String>> provideMultipartWriteHeaders() {
    return provideHeaders(this.config.multipartWrite.headers);
  }

  @Provides
  @Singleton
  @ObjectRestoreHeaders
  public Map<String, Function<Map<String, String>, String>> provideObjectRestoreHeaders() {
    return provideHeaders(this.config.objectRestore.headers);
  }

  @Provides
  @Singleton
  @PutContainerLifecycleHeaders
  public Map<String, Function<Map<String, String>, String>> providePutContainerLifecycleHeaders() {
    return provideHeaders(this.config.putContainerLifecycle.headers);
  }

  @Provides
  @Singleton
  @GetContainerLifecycleHeaders
  public Map<String, Function<Map<String, String>, String>> provideGetBucketLifecycleHeaders() {
    return provideHeaders(this.config.getContainerLifecycle.headers);
  }

  @Provides
  @Singleton
  @PutContainerProtectionHeaders
  public Map<String, Function<Map<String, String>, String>> providePutContainerProtectionHeaders() {
    return provideHeaders(this.config.putContainerProtection.headers);
  }

  @Provides
  @Singleton
  @GetContainerProtectionHeaders
  public Map<String, Function<Map<String, String>, String>> provideGetContainerProtectionHeaders() {
    return provideHeaders(this.config.getContainerProtection.headers);
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
      headers.put(e.getKey(), createSelectionConfigSupplier(e.getValue()));
    }

    return headers;
  }

  @Provides
  @Singleton
  @Named("write.context")
  public List<Function<Map<String, String>, String>> provideWriteContext(final Api api) {
    final List<Function<Map<String, String>, String>> context = Lists.newArrayList();

    final OperationConfig operationConfig = checkNotNull(this.config.write);
    if (Api.SOH != api) {
      if (operationConfig.object.selection != null) {
        context.add(ModuleUtils.provideObject(operationConfig));
      } else {
        // default for writes
        context.add(new UUIDObjectNameFunction(this.config.octalNamingMode));
      }
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
  @Named("writeCopy.context")
  public List<Function<Map<String, String>, String>> provideWriteCopyContext(final Api api) {
    final List<Function<Map<String, String>, String>> context = Lists.newArrayList();

    final OperationConfig operationConfig = checkNotNull(this.config.writeCopy);
    if (Api.SOH != api) {
      if (operationConfig.object.selection != null) {
        context.add(ModuleUtils.provideObject(operationConfig));
      } else {
        // default for writes
        context.add(new UUIDObjectNameFunction(this.config.octalNamingMode));
      }
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
  @Named("writeCopy.delimiter")
  public Function<Map<String, String>, String> provideWriteCopyDelimiter(final SelectionConfig<ObjectDelimiterConfig> listDelimiterConfig) {
    if (this.config.write.delimiter == null) {
      return null;
    }
    final SelectionConfig<ObjectDelimiterConfig> delimiterConfig = this.config.write.delimiter;
    final List<ChoiceConfig<ObjectDelimiterConfig>> delimiters = checkNotNull(delimiterConfig.choices);
    checkArgument(!delimiters.isEmpty(), "delimiters must not be empty");


    for (final ChoiceConfig<ObjectDelimiterConfig> choice : delimiters) {
      checkNotNull(choice);
      checkNotNull(choice.choice);
    }
    return provideDelimiter(delimiterConfig);
  }

  @Provides
  @Singleton
  @Named("api.version")
  public String provideAPIVersion(final Api api) {

    if (Api.OPENSTACK == api) {
      return "v1";
    }
    return null;
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
      function = ModuleUtils.provideObject(operationConfig);
    } else {
      function = new ReadObjectNameFunction(objectManager);
    }

    return ImmutableList.of(function);
  }

  @Provides
  @Singleton
  @Named("writeCopySource.context")
  public List<Function<Map<String, String>, String>> provideSSeReadContext(
      final ObjectManager objectManager) {
    Function<Map<String, String>, String> function;
    Function<Map<String, String>, String> sourceContainerfunction = null;

    final OperationConfig operationConfig = checkNotNull(this.config.writeCopy);
    if (operationConfig.sourceObject.selection != null) {
      function = provideSourceObject(operationConfig);
    } else {
      function = new SourceReadObjectNameFunction(objectManager);
    }
    return ImmutableList.of(function);
  }

  @Provides
  @Named("write_legalhold.context")
  public List<Function<Map<String, String>, String>> provideWriteLegalholdContext(
      final ObjectManager objectManager) {
    Function<Map<String, String>, String> function;

    final OperationConfig operationConfig = checkNotNull(this.config.writeLegalhold);
    if (operationConfig.object.selection != null) {
      function = ModuleUtils.provideObject(operationConfig);
    } else {
      String legalHoldName;
      if (this.config.writeLegalhold.legalHold != null
          && this.config.writeLegalhold.legalHold.legalHoldPrefix != null) {
        legalHoldName = this.config.writeLegalhold.legalHold.legalHoldPrefix;
      } else {
        legalHoldName = "LegalHold";
      }
      function = new LegalholdObjectNameFunction(objectManager, legalHoldName);
    }

    return ImmutableList.of(function);
  }

  @Provides
  @Named("retention_extension.context")
  public List<Function<Map<String, String>, String>> provideRetentionExtensionContext(
          final ObjectManager objectManager) {

    Function<Map<String, String>, String> function;

    final OperationConfig operationConfig = checkNotNull(this.config.extendRetention);
    if (operationConfig.object.selection != null) {
      function = ModuleUtils.provideObject(operationConfig);
    } else {
      function = new ObjectRetentionExtensionFunction(objectManager);
    }

    return ImmutableList.of(function);
  }

  @Provides
  @Named("objectRestore.context")
  public List<Function<Map<String, String>, String>> provideObjectRestoreContext(
          final ObjectManager objectManager) {

    Function<Map<String, String>, String> function;

    final OperationConfig operationConfig = checkNotNull(this.config.objectRestore);
    if (operationConfig.object.selection != null) {
      function = ModuleUtils.provideObject(operationConfig);
    } else {
      function = new ReadObjectNameFunction(objectManager);
    }


    Function <Map<String, String>, String> functionObjectRestorePeriod = null;
    try {
      functionObjectRestorePeriod = provideObjectRestorePeriod(operationConfig);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return ImmutableList.of(function, functionObjectRestorePeriod);
  }

  @Provides
  @Singleton
  @Named("putContainerLifecycle.context")
  public List<Function<Map<String, String>, String>> providePutContainerLifecycleContext() {

    final OperationConfig operationConfig = checkNotNull(this.config.putContainerLifecycle);
    Function <Map<String, String>, String> function = null;
    try {
      function = provideArchiveTransitionPeriod(operationConfig);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // return an empty context
    return ImmutableList.of(function);
  }

  @Provides
  @Singleton
  @Named("getContainerLifecycle.context")
  public List<Function<Map<String, String>, String>> provideGetContainerLifecycleContext(
          final ObjectManager objectManager) {
    final List<Function<Map<String, String>, String>> context = Lists.newArrayList();

    // return an empty context
    return ImmutableList.copyOf(context);
  }

  @Provides
  @Singleton
  @Named("putContainerProtection.context")
  public List<Function<Map<String, String>, String>> providePutContainerProtectionContext() {

    final List<Function<Map<String, String>, String>> context = Lists.newArrayList();
    Function<Map<String, String>, String> f = provideContainerProtectionMinimumRetention();
    if (f != null) {
      context.add(f);
    }
    f = provideContainerProtectionMaximumRetention();
    if (f != null) {
      context.add(f);
    }
    f = provideContainerProtectionDefaultRetention();
    if (f != null) {
      context.add(f);
    }
    return ImmutableList.copyOf(context);
  }

  @Provides
  @Singleton
  @Named("getContainerProtection.context")
  public List<Function<Map<String, String>, String>> provideGetContainerProtectionContext(
          final ObjectManager objectManager) {
    final List<Function<Map<String, String>, String>> context = Lists.newArrayList();
    // return an empty context
    return ImmutableList.copyOf(context);
  }

  @Provides
  @Singleton
  @Named("metadata.context")
  public List<Function<Map<String, String>, String>> provideMetadataContext(
      final ObjectManager objectManager) {
    Function<Map<String, String>, String> function;

    final OperationConfig operationConfig = checkNotNull(this.config.metadata);
    if (operationConfig.object.selection != null) {
      function = ModuleUtils.provideObject(operationConfig);
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
      function = ModuleUtils.provideObject(operationConfig);
    } else {
      function = new DeleteObjectNameFunction(objectManager);
    }

    return ImmutableList.of(function);
  }



  @Provides
  @Singleton
  @Named("containerList.context")
  public List<Function<Map<String, String>, String>> provideContainerListContext(
      final ObjectManager objectManager) {
    final List<Function<Map<String, String>, String>> context = Lists.newArrayList();

    // return an empty context
    return ImmutableList.copyOf(context);
  }

  @Provides
  @Singleton
  @Named("containerCreate.context")
  public List<Function<Map<String, String>, String>> provideContainerCreateContext(
      final ObjectManager objectManager) {
    final List<Function<Map<String, String>, String>> context = Lists.newArrayList();

    // return an empty context
    return ImmutableList.copyOf(context);
  }

  @Provides
  @Singleton
  @Named("multipartWrite.context")
  public List<Function<Map<String, String>, String>> provideMultipartWriteContext(final Api api) {
    final List<Function<Map<String, String>, String>> context = Lists.newArrayList();

    final OperationConfig operationConfig = checkNotNull(this.config.multipartWrite);
    if (Api.SOH != api) {
      if (operationConfig.object.selection != null) {
        context.add(ModuleUtils.provideObject(operationConfig));
      } else {
        // default for writes
        context.add(new UUIDObjectNameFunction(this.config.octalNamingMode));
      }
    }

    return ImmutableList.copyOf(context);
  }

  private Function<Map<String, String>, String> provideDelimiter(final SelectionConfig<ObjectDelimiterConfig> delimiters) {
    final Supplier<ObjectDelimiterConfig> delimiterConfigSupplier;
    final SelectionType selection = checkNotNull(delimiters.selection);

    // if delimiters choices list is empty return null
    if (delimiters.choices.isEmpty()) {
      return null;
    }
    if (SelectionType.ROUNDROBIN == selection) {
      final List<ObjectDelimiterConfig> delimiterConfigList = Lists.newArrayList();
      for (final ChoiceConfig<ObjectDelimiterConfig> choice : delimiters.choices) {
        delimiterConfigList.add(choice.choice);
      }
      delimiterConfigSupplier = Suppliers.cycle(delimiterConfigList);
    } else {
      final RandomSupplier.Builder<ObjectDelimiterConfig> wrc = Suppliers.random();
      for (final ChoiceConfig<ObjectDelimiterConfig> choice : delimiters.choices) {
        wrc.withChoice(choice.choice, choice.weight);
      }
      delimiterConfigSupplier = wrc.build();
    }
    return new Function<Map<String, String>, String>() {

      @Override
      public String apply(final Map<String, String> input) {
        final ObjectDelimiterConfig config = delimiterConfigSupplier.get();
        String objectName = input.get(Context.X_OG_OBJECT_NAME);
        byte[] objectBytes = objectName.getBytes();
        ObjectDelimiterConfig.DelimChar[] delimChars = config.delimChars;
        for (ObjectDelimiterConfig.DelimChar c: delimChars) {
          String d = c.value;
          int[] positions = c.positions;
          for (int pos: positions) {
            //objectName[pos] = d;
            objectBytes[pos] = d.getBytes()[0];
          }
        }
        String newobjectName = new String(objectBytes);
        input.put(Context.X_OG_OBJECT_NAME, newobjectName);
        return newobjectName;
      }
    };

  }

  @Provides
  @Singleton
  @Named("write.delimiter")
  public Function<Map<String, String>, String> provideListDelimiter(final SelectionConfig<ObjectDelimiterConfig> listDelimiterConfig) {
    if (this.config.write.delimiter == null) {
      return null;
    }
    final SelectionConfig<ObjectDelimiterConfig> delimiterConfig = this.config.write.delimiter;
    final List<ChoiceConfig<ObjectDelimiterConfig>> delimiters = checkNotNull(delimiterConfig.choices);
    checkArgument(!delimiters.isEmpty(), "delimiters must not be empty");


    for (final ChoiceConfig<ObjectDelimiterConfig> choice : delimiters) {
      checkNotNull(choice);
      checkNotNull(choice.choice);
    }
    return provideDelimiter(delimiterConfig);
  }


  @Provides
  @Singleton
  @Named("write.retention")
  private Function<Map<String, String>, Long> provideWriteRetention() {
    if (this.config.write.retention == null) {
      return null;
    }
    final SelectionConfig<RetentionConfig> retentionConfig = this.config.write.retention;
    final List<ChoiceConfig<RetentionConfig>> retentions = checkNotNull(retentionConfig.choices);
    checkArgument(!retentions.isEmpty(), "retentions must not be empty");


    for (final ChoiceConfig<RetentionConfig> choice : retentions) {
      checkNotNull(choice);
      checkNotNull(choice.choice);
    }
    return provideRetention(retentionConfig);
  }

  @Provides
  @Singleton
  @Named("overwrite.retention")
  private Function<Map<String, String>, Long> provideOverwriteRetention() {
    // return null;
    if (this.config.overwrite.retention == null) {
      return null;
    }
    final SelectionConfig<RetentionConfig> retentionConfig = this.config.overwrite.retention;
    final List<ChoiceConfig<RetentionConfig>> retentions = checkNotNull(retentionConfig.choices);
    checkArgument(!retentions.isEmpty(), "retentions must not be empty");


    for (final ChoiceConfig<RetentionConfig> choice : retentions) {
      checkNotNull(choice);
      checkNotNull(choice.choice);
    }
    return provideRetention(retentionConfig);
  }

  @Provides
  @Singleton
  @Named("extend.retention")
  private Function<Map<String, String>, Long> provideRetentionExtension() {
    if (this.config.extendRetention.retention == null) {
      return null;
    }
    final SelectionConfig<RetentionConfig> retentionConfig = this.config.extendRetention.retention;
    final List<ChoiceConfig<RetentionConfig>> retentions = checkNotNull(retentionConfig.choices);
    checkArgument(!retentions.isEmpty(), "retention extensions must not be empty");


    for (final ChoiceConfig<RetentionConfig> choice : retentions) {
      checkNotNull(choice);
      checkNotNull(choice.choice);
      checkArgument(choice.choice.expiry >= -1, "Expiry must be greater than or equal to -1");
    }
    return provideRetentionExtension(retentionConfig);
  }

  @Provides
  @Singleton
  @Named("multipartWrite.retention")
  private Function<Map<String, String>, Long> provideMultipartWriteRetention() {
    // return null;
    if (this.config.multipartWrite.retention == null) {
      return null;
    }
    final SelectionConfig<RetentionConfig> retentionConfig = this.config.multipartWrite.retention;
    final List<ChoiceConfig<RetentionConfig>> retentions = checkNotNull(retentionConfig.choices);
    checkArgument(!retentions.isEmpty(), "retentions must not be empty");


    for (final ChoiceConfig<RetentionConfig> choice : retentions) {
      checkNotNull(choice);
      checkNotNull(choice.choice);
    }
    return provideRetention(retentionConfig);
  }

  @Provides
  @Singleton
  @Named("multipartWrite.delimiter")
  public Function<Map<String, String>, String> provideMultipartWritetDelimiter(final SelectionConfig<ObjectDelimiterConfig> listDelimiterConfig) {
    if (this.config.write.delimiter == null) {
      return null;
    }
    final SelectionConfig<ObjectDelimiterConfig> delimiterConfig = this.config.write.delimiter;
    final List<ChoiceConfig<ObjectDelimiterConfig>> delimiters = checkNotNull(delimiterConfig.choices);
    checkArgument(!delimiters.isEmpty(), "delimiters must not be empty");


    for (final ChoiceConfig<ObjectDelimiterConfig> choice : delimiters) {
      checkNotNull(choice);
      checkNotNull(choice.choice);
    }
    return provideDelimiter(delimiterConfig);
  }

  @Provides
  @Singleton
  @Named("containerCreate.retention")
  private Function<Map<String, String>, Long> provideContainerCreateRetention() {
    // return null;
    if (this.config.containerCreate.retention == null) {
      return null;
    }
    final SelectionConfig<RetentionConfig> retentionConfig = this.config.containerCreate.retention;
    final List<ChoiceConfig<RetentionConfig>> retentions = checkNotNull(retentionConfig.choices);
    checkArgument(!retentions.isEmpty(), "retentions must not be empty");


    for (final ChoiceConfig<RetentionConfig> choice : retentions) {
      checkNotNull(choice);
      checkNotNull(choice.choice);
      checkArgument(choice.choice.expiry >= -1, "Expiry must be greater than or equal to -1");
    }
    return provideRetention(retentionConfig);
  }

  @Named("containerProtectionMinimum.retention")
  private Function<Map<String, String>, String> provideContainerProtectionMinimumRetention() {
    if (this.config.putContainerProtection.containerMinimumRetention == null) {
      return null;
    }
    final SelectionConfig<RetentionConfig> retentionConfig =
            this.config.putContainerProtection.containerMinimumRetention;
    final List<ChoiceConfig<RetentionConfig>> retentions = checkNotNull(retentionConfig.choices);
    checkArgument(!retentions.isEmpty(), "container protection minimum retention must not be empty");

    for (final ChoiceConfig<RetentionConfig> choice : retentions) {
      checkNotNull(choice);
      checkNotNull(choice.choice);
      checkArgument(choice.choice.expiry >= 0, "Container Minimum Retention Value must be greater than or equal to 0");
    }
    return provideContainerRetention(retentionConfig, Context.X_OG_CONTAINER_MINIMUM_RETENTION_PERIOD);
  }

  @Named("containerProtectionMaximum.retention")
  private Function<Map<String, String>, String> provideContainerProtectionMaximumRetention() {
    if (this.config.putContainerProtection.containerMaximumRetention == null) {
      return null;
    }
    final SelectionConfig<RetentionConfig> retentionConfig =
            this.config.putContainerProtection.containerMaximumRetention;
    final List<ChoiceConfig<RetentionConfig>> retentions = checkNotNull(retentionConfig.choices);
    checkArgument(!retentions.isEmpty(), "container protection maximum retention must not be empty");

    for (final ChoiceConfig<RetentionConfig> choice : retentions) {
      checkNotNull(choice);
      checkNotNull(choice.choice);
      checkArgument(choice.choice.expiry >= 0, "Container Maximum Retention Value must be greater than or equal to 0");
    }
    return provideContainerRetention(retentionConfig, Context.X_OG_CONTAINER_MAXIMUM_RETENTION_PERIOD);
  }


  @Named("containerProtectionDefault.retention")
  private Function<Map<String, String>, String> provideContainerProtectionDefaultRetention() {
    if (this.config.putContainerProtection.containerDefaultRetention == null) {
      return null;
    }
    final SelectionConfig<RetentionConfig> retentionConfig =
            this.config.putContainerProtection.containerDefaultRetention;
    final List<ChoiceConfig<RetentionConfig>> retentions = checkNotNull(retentionConfig.choices);
    checkArgument(!retentions.isEmpty(), "container protection default retention must not be empty");

    for (final ChoiceConfig<RetentionConfig> choice : retentions) {
      checkNotNull(choice);
      checkNotNull(choice.choice);
      checkArgument(choice.choice.expiry >= 0, "Container Default Retention Value must be greater than or equal to 0");
    }
    return provideContainerRetention(retentionConfig, Context.X_OG_CONTAINER_DEFAULT_RETENTION_PERIOD);
  }

  private Function<Map<String, String>, Long> provideRetention(
    final SelectionConfig<RetentionConfig> retentions) {
    final Supplier<RetentionConfig> retentionConfigSupplier;
    final SelectionType selection = checkNotNull(retentions.selection);

    // if retentions list is empty return null
    if (retentions.choices.isEmpty()) {
      return null;
    }
    if (SelectionType.ROUNDROBIN == selection) {
      final List<RetentionConfig> retentionConfigList = Lists.newArrayList();
      for (final ChoiceConfig<RetentionConfig> choice : retentions.choices) {
        retentionConfigList.add(choice.choice);
      }
      retentionConfigSupplier = Suppliers.cycle(retentionConfigList);
    } else {
      final RandomSupplier.Builder<RetentionConfig> wrc = Suppliers.random();
      for (final ChoiceConfig<RetentionConfig> choice : retentions.choices) {
        wrc.withChoice(choice.choice, choice.weight);
      }
      retentionConfigSupplier = wrc.build();
    }
    return new Function<Map<String, String>, Long>() {

      @Override
      public Long apply(final Map<String, String> input) {
        final RetentionConfig retentionConfig = retentionConfigSupplier.get();
        if (retentionConfig.expiry == -255) {
          return 0L; // no retention
        }
        else if (retentionConfig.expiry == -1L ||  retentionConfig.expiry == -2L) {
          input.put(Context.X_OG_OBJECT_RETENTION, String.valueOf(retentionConfig.expiry));
          return retentionConfig.expiry;
        }
        else if (retentionConfig.expiry > 0) {
          final Long expiryTime = retentionConfig.timeUnit.toSeconds(retentionConfig.expiry);
          checkArgument(
              (expiryTime
                  + System.currentTimeMillis() / 1000) <= RetentionConfig.MAX_RETENTION_EXPIRY,
              "The expiry in [%s] seconds duration should be earlier than January 19, 2038 3:14:07 AM",
              expiryTime);
          input.put(Context.X_OG_OBJECT_RETENTION, String.valueOf(expiryTime));
          return expiryTime;
        } else {
          return 0L;
        }
      }
    };
  }

  private Function<Map<String, String>, Long> provideRetentionExtension(
          final SelectionConfig<RetentionConfig> retentions) {
    final Supplier<RetentionConfig> retentionConfigSupplier;
    final SelectionType selection = checkNotNull(retentions.selection);

    // if retentions list is empty return null
    if (retentions.choices.isEmpty()) {
      return null;
    }
    if (SelectionType.ROUNDROBIN == selection) {
      final List<RetentionConfig> retentionConfigList = Lists.newArrayList();
      for (final ChoiceConfig<RetentionConfig> choice : retentions.choices) {
        retentionConfigList.add(choice.choice);
      }
      retentionConfigSupplier = Suppliers.cycle(retentionConfigList);
    } else {
      final RandomSupplier.Builder<RetentionConfig> wrc = Suppliers.random();
      for (final ChoiceConfig<RetentionConfig> choice : retentions.choices) {
        wrc.withChoice(choice.choice, choice.weight);
      }
      retentionConfigSupplier = wrc.build();
    }
    return new Function<Map<String, String>, Long>() {
      @Override
      public Long apply(final Map<String, String> input) {
        final RetentionConfig retentionConfig = retentionConfigSupplier.get();
        checkArgument(retentionConfig.expiry > 0, "Retention extension must be positive");
        long retention;
        try {
          retention = Long.parseLong(input.get(Context.X_OG_OBJECT_RETENTION));
        } catch (NumberFormatException nfe) {
            // this can happen if an object name is generated with given prefix, suffix without
            // being
            retention = 0;
        }

        final Long extention = retentionConfig.timeUnit.toSeconds(retentionConfig.expiry);
          checkArgument(
                  (retention + extention +
                          + System.currentTimeMillis() / 1000) <= RetentionConfig.MAX_RETENTION_EXPIRY,
                  "The expiry in [%s] seconds duration should be earlier than January 19, 2038 3:14:07 AM",
                  retention);
          input.put(Context.X_OG_OBJECT_RETENTION, String.valueOf(retention));
          input.put(Context.X_OG_OBJECT_RETENTION_EXT, String.valueOf(extention));
          return retention;
      }
    };
  }

  private Function<Map<String, String>, String> provideContainerRetention(
          final SelectionConfig<RetentionConfig> retentions, final String retentionType) {
    final Supplier<RetentionConfig> retentionConfigSupplier;
    final SelectionType selection = checkNotNull(retentions.selection);

    if (SelectionType.ROUNDROBIN == selection) {
      final List<RetentionConfig> retentionConfigList = Lists.newArrayList();
      for (final ChoiceConfig<RetentionConfig> choice : retentions.choices) {
        retentionConfigList.add(choice.choice);
      }
      retentionConfigSupplier = Suppliers.cycle(retentionConfigList);
    } else {
      final RandomSupplier.Builder<RetentionConfig> wrc = Suppliers.random();
      for (final ChoiceConfig<RetentionConfig> choice : retentions.choices) {
        wrc.withChoice(choice.choice, choice.weight);
      }
      retentionConfigSupplier = wrc.build();
    }
    return new Function<Map<String, String>, String>() {

      @Override
      public String apply(final Map<String, String> input) {
        final RetentionConfig retentionConfig = retentionConfigSupplier.get();
        final Long expiryTime = retentionConfig.timeUnit.toDays(retentionConfig.expiry);
        input.put(retentionType, String.valueOf(expiryTime));
        return String.valueOf(expiryTime);
      }
    };
  }

  @Provides
  @Singleton
  @Named("write.legalHold")
  public Supplier<Function<Map<String, String>, String>> provideLegalHold() {
    return provideLegalHold(this.config.write.legalHold);
  }

  @Provides
  @Singleton
  @Named("multipartWrite.legalHold")
  public Supplier<Function<Map<String, String>, String>> provideMultipartLegalHold() {
    return provideLegalHold(this.config.multipartWrite.legalHold);
  }


  @Provides
  @Singleton
  @Named("add.legalHold")
  public Supplier<Function<Map<String, String>, String>> provideAddLegalHold() {
    if (config.writeLegalhold.weight > 0.0) {
      checkArgument(config.writeLegalhold.legalHold != null,
              "legalhold must be specificied for write_legalhold operation");
      checkArgument(config.writeLegalhold.legalHold.percentage == 100.00,
              "legalhold percentage must be set to 100.00 percentage for write_legalhold operation");
    }

    return provideLegalHold(this.config.writeLegalhold.legalHold);
  }

  @Provides
  @Singleton
  @Named("delete.legalHold")
  public Supplier<Function<Map<String, String>, String>> provideDeleteLegalHold() {
    if (config.deleteLegalhold.weight > 0.0) {
      checkArgument(config.deleteLegalhold.legalHold != null,
              "legalhold must be specificied for delete_legalhold operation");
      checkArgument(config.deleteLegalhold.legalHold.percentage == 100.00,
              "legalhold percentage must be set to 100.00 percentage for delete_legalhold operation");

    }
    return provideLegalHold(this.config.deleteLegalhold.legalHold);
  }

  @Provides
  @Singleton
  @Named("delete_legalhold.context")
  public List<Function<Map<String, String>, String>> provideDeleteLegalholdContext(
      final ObjectManager objectManager) {
    Function<Map<String, String>, String> function;

    final OperationConfig operationConfig = checkNotNull(this.config.deleteLegalhold);
    if (operationConfig.object.selection != null) {
      function = ModuleUtils.provideObject(operationConfig);
    } else {
      String legalHoldName;
      if (this.config.deleteLegalhold.legalHold != null
          && this.config.deleteLegalhold.legalHold.legalHoldPrefix != null) {
        legalHoldName = this.config.deleteLegalhold.legalHold.legalHoldPrefix;
      } else {
        legalHoldName = "LegalHold";
      }
      function = new LegalholdObjectNameFunction(objectManager, legalHoldName);
    }
    return ImmutableList.of(function);
  }

  @Provides
  @Singleton
  @Named("overwrite.legalHold")
  private Supplier<Function<Map<String, String>, String>> provideOverwriteLegalHold() {
    return provideLegalHold(config.overwrite.legalHold);
  }


  @Provides
  private Supplier<Function<Map<String, String>, String>> provideLegalHold(final LegalHold legalHold) {
    if (legalHold == null) {
      return null;
    }
    Function<Map<String, String>, String> f = new
      Function<Map<String, String>, String>() {
        @Nullable
        @Override
          public String apply(@Nullable Map<String, String> context) {
      // delete legalhold
      if (context.get(Context.X_OG_LEGAL_HOLD_SUFFIX) != null) {
        int suffix = Integer.parseInt(context.get(Context.X_OG_LEGAL_HOLD_SUFFIX));
        if (legalHold.legalHoldPrefix != null && !legalHold.legalHoldPrefix.isEmpty()) {
          context.put(Context.X_OG_LEGAL_HOLD_PREFIX, legalHold.legalHoldPrefix);
        } else {
          context.put(Context.X_OG_LEGAL_HOLD_PREFIX, "LegalHold");
        }
        String val = context.get(Context.X_OG_LEGAL_HOLD_PREFIX).concat(String.valueOf(suffix));
        context.put(Context.X_OG_LEGAL_HOLD, val);
        return val;
      } else {
        // add legalhold context
        if (legalHold.legalHoldPrefix != null && !legalHold.legalHoldPrefix.isEmpty()) {
          context.put(Context.X_OG_LEGAL_HOLD_PREFIX, legalHold.legalHoldPrefix);
        } else {
          context.put(Context.X_OG_LEGAL_HOLD_PREFIX, "LegalHold");
        }
        String val = context.get(Context.X_OG_LEGAL_HOLD_PREFIX).concat(String.valueOf(1));
        context.put(Context.X_OG_LEGAL_HOLD, val);
        context.put(Context.X_OG_LEGAL_HOLD_SUFFIX, String.valueOf(1));
        return val;
      }
    }
  };

    Supplier<Function<Map<String, String>, String>> s =
              new RandomPercentageSupplier.Builder<Function<Map<String, String>, String>>().
                      withChoice(f, legalHold.percentage).
                      withRandom(new Random()).build();
    return s;

  }

  @Provides
  @Singleton
  @Named("credentials")
  private Function<Map<String, String>, Credential> provideCredentials(final Api api)
      throws Exception {
    final List<Credential> credentialList = Lists.newArrayList();

    if (AuthType.NONE == this.config.authentication.type) {
      return null;
    } else { // BASIC, AWSV2, AWSV4
      if (CredentialSource.FILE == this.config.authentication.credentialSource) {
        final File credentialFile = new File(this.config.authentication.credentialFile);
        return new CredentialGetterFunction(this.config.authentication.type, credentialFile, api);

      } else if (CredentialSource.CONFIG == this.config.authentication.credentialSource) {

        final Credential credential =
            new Credential(this.config.authentication.username, this.config.authentication.password,
                this.config.authentication.keystoneToken, this.config.authentication.iamToken,
                this.config.authentication.account);
        credentialList.add(credential);

        if (credentialList.size() == 0) {
          throw new Exception("No credentials provided for " + this.config.authentication.type);
        }
        final Supplier<Credential> credentialSupplier = Suppliers.cycle(credentialList);
        return MoreFunctions.forSupplier(credentialSupplier);

      } else {
        throw new IllegalArgumentException(
            "Invalid CredentialSource: " + this.config.authentication.credentialSource);
      }
    }

  }


  private Function<Map<String, String>, String> createSelectionConfigSupplier(
          final SelectionConfig<String> selectionConfig) {

    if (SelectionType.ROUNDROBIN == selectionConfig.selection) {
      final List<String> choiceList = Lists.newArrayList();
      for (final ChoiceConfig<String> choice : selectionConfig.choices) {
        choiceList.add(choice.choice);
      }
      final Supplier<String> configSupplier = Suppliers.cycle(choiceList);
      return MoreFunctions.forSupplier(configSupplier);
    }

    final RandomSupplier.Builder<String> wrc = Suppliers.random();
    for (final ChoiceConfig<String> choice : selectionConfig.choices) {
      wrc.withChoice(choice.choice, choice.weight);
    }
    final Supplier<String> configSupplier = wrc.build();
    return MoreFunctions.forSupplier(configSupplier);
  }

  private Map<String, Function<Map<String, String>, String>> provideLegalHoldQueryParameters(
      final boolean remove) {
    final Map<String, Function<Map<String, String>, String>> queryParameters;
    queryParameters = Maps.newLinkedHashMap();
    queryParameters.put(QueryParameters.LEGALHOLD_PARAMETER,
        new Function<Map<String, String>, String>() {
          @Override
          public String apply(final Map<String, String> context) {
            return null;
          }
        });

    if (!remove) {
      queryParameters.put(QueryParameters.LEGALHOLD_ADD_PARAMETER,
          new Function<Map<String, String>, String>() {
            @Override
            public String apply(final Map<String, String> context) {
              // find the legal hold in the context
              final String suffix = context.get(Context.X_OG_LEGAL_HOLD_SUFFIX);
              checkArgument(suffix != null, "legal hold suffix cannot be null");
              final int newSuffix = Byte.valueOf(suffix) + 1;
              final String prefix = context.get(Context.X_OG_LEGAL_HOLD_PREFIX);
              return prefix.concat(String.valueOf(newSuffix));
            }
          });
    } else {
      queryParameters.put(QueryParameters.LEGALHOLD_REMOVE_PARAMETER,
          new Function<Map<String, String>, String>() {
            @Override
            public String apply(final Map<String, String> context) {
              // find the legal hold in the context
              final String suffix = context.get(Context.X_OG_LEGAL_HOLD_SUFFIX);
              checkArgument(suffix != null, "legal hold suffix cannot be null");
              final String prefix = context.get(Context.X_OG_LEGAL_HOLD_PREFIX);
              return prefix.concat(String.valueOf(suffix));
            }
          });
    }
    return queryParameters;
  }

  private Map<String, Function<Map<String, String>, String>> provideRetentionExtensionQueryParameters() {
    final Map<String, Function<Map<String, String>, String>> queryParameters;
    queryParameters = Maps.newLinkedHashMap();
    queryParameters.put(QueryParameters.OBJECT_RETENTION_EXTENSION_PARAMETER,
            new Function<Map<String, String>, String>() {
              @Override
              public String apply(final Map<String, String> context) {
                return null;
              }
            });

    return queryParameters;
  }

  @Provides
  @Singleton
  public Function<Map<String, String>, Body> provideBody() {
    return createBodySupplier(checkNotNull(this.config.filesize, "filesize must not be null"));
  }

  private Function<Map<String, String>, Body> createBodySupplier(
      final SelectionConfig<FilesizeConfig> filesizeConfig) {
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


  private Function<Map<String, String>, Body> createObjectRestoreBodySupplier() {
    return new Function<Map<String, String>, Body>() {
      public Body apply(Map<String, String> input) {
        String body;
        StringBuilder sb = new StringBuilder();
        sb.append("<RestoreRequest>");
        sb.append("<Days>");
        sb.append(input.get(Context.X_OG_OBJECT_RESTORE_PERIOD));
        sb.append("</Days>");
        sb.append("<GlacierJobParameters>");
        sb.append("<Tier>Bulk</Tier>");
        sb.append("</GlacierJobParameters>");
        sb.append("</RestoreRequest>");

        body = sb.toString();
        return(Bodies.custom(body.length(), body));
      }
    };
  }

  private Function<Map<String, String>, Body> createPutContainerLifecycleBodySupplier() {
    return new Function<Map<String, String>, Body>() {
      public Body apply(Map<String, String> input) {
        String body;
        StringBuilder sb = new StringBuilder();
        sb.append("<LifecycleConfiguration>");
        sb.append("<Rule>");
        sb.append("<ID>id1</ID>");
        sb.append("<Status>Enabled</Status>");
        sb.append("<Filter><Prefix/></Filter>");
        sb.append("<Transition>");
        sb.append("<Days>");
        sb.append(input.get(Context.X_OG_ARCHIVE_TRANSITION_PERIOD));
        sb.append("</Days>");
        sb.append("<StorageClass>GLACIER</StorageClass>");
        sb.append("</Transition>");
        sb.append("</Rule>");
        sb.append("</LifecycleConfiguration>");

        body = sb.toString();
        return(Bodies.custom(body.length(), body));
      }
    };
  }

  private Function<Map<String, String>, Body> createPutContainerProtectionSupplier() {
    return new Function<Map<String, String>, Body>() {
      public Body apply(Map<String, String> input) {
        String body;
        StringBuilder sb = new StringBuilder();
        sb.append("<ProtectionConfiguration>");
        sb.append("<Status>Compliance</Status>");
        sb.append("<MinimumRetention><Days>");
        sb.append(input.get(Context.X_OG_CONTAINER_MINIMUM_RETENTION_PERIOD));
        sb.append("</Days></MinimumRetention>");
        sb.append("<MaximumRetention><Days>");
        sb.append(input.get(Context.X_OG_CONTAINER_MAXIMUM_RETENTION_PERIOD));
        sb.append("</Days></MaximumRetention>");
        sb.append("<DefaultRetention><Days>");
        sb.append(input.get(Context.X_OG_CONTAINER_DEFAULT_RETENTION_PERIOD));
        sb.append("</Days></DefaultRetention>");
        sb.append("</ProtectionConfiguration>");

        body = sb.toString();
        return(Bodies.custom(body.length(), body));
      }
    };
  }

  @Provides
  @Singleton
  @WriteBody
  public Function<Map<String, String>, Body> provideWriteBody() {
    final SelectionConfig<FilesizeConfig> filesize = this.config.write.filesize;
    if (filesize != null) {
      return createBodySupplier(filesize);
    } else {
      return createBodySupplier(checkNotNull(this.config.filesize, "filesize must not be null"));
    }
  }

  @Provides
  @Singleton
  @OverwriteBody
  public Function<Map<String, String>, Body> provideOverwriteBody() {
    if (this.config.overwrite.body == BodySource.EXISTING) {
      return createBodySupplier();
    } else {
      final SelectionConfig<FilesizeConfig> filesize = this.config.overwrite.filesize;
      if (filesize != null) {
        return createBodySupplier(filesize);
      } else {
        return createBodySupplier(checkNotNull(this.config.filesize, "filesize must not be null"));
      }
    }
  }

  @Provides
  @Singleton
  @MultiPartWriteBody
  public Function<Map<String, String>, Body> provideMultiPartWriteBody() {
    final SelectionConfig<FilesizeConfig> filesize = this.config.multipartWrite.filesize;
    if (filesize != null) {
      return createBodySupplier(filesize);
    } else {
      return createBodySupplier(checkNotNull(this.config.filesize, "filesize must not be null"));
    }
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

  private Function<Map<String, String>, Body> createBodySupplier(
      final Supplier<Distribution> distributionSupplier) {
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

  private Function<Map<String, String>, Body> createBodySupplier() {
    final DataType data = checkNotNull(this.config.data);
    checkArgument(DataType.NONE != data, "Unacceptable data [%s]", data);

    final Function<Map<String, String>, Body> function = new Function<Map<String, String>, Body>() {
      @Override
      public Body apply(@Nullable final Map<String, String> input) {
        final String size = input.get(Context.X_OG_OBJECT_SIZE);
        switch (data) {
          case ZEROES:
            return Bodies.zeroes(Long.parseLong(size));
          default:
            return Bodies.random(Long.parseLong(size));
        }
      }
    };

    return function;
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
  public String provideObjectFileName(final Api api) {
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

  private byte[] SSECustomerKey() {
    final byte[] aesKey = new byte[32];
    for (int i = 0; i < 16; i++) {
      aesKey[i * 2] = (byte) 0xCA;
      aesKey[i * 2 + 1] = (byte) 0xFE;
    }
    return aesKey;
  }

  private Function<Map<String, String>, String> provideSSEEncryptionAlgorithm() {
    return new Function<Map<String, String>, String>() {
      @Override
      public String apply(@Nullable final Map<String, String> input) {
        return "AES256";
      }
    };
  }

  private Function<Map<String, String>, String> provideSSEEncryptionKey() {
    Function<Map<String, String>, String> encryptionKey;
    encryptionKey = new Function<Map<String, String>, String>() {
      @Override
      public String apply(@Nullable final Map<String, String> input) {
        final String b64Key = BaseEncoding.base64().encode(OGModule.this.aesKey);
        input.put("x-amz-server-side-encryption-customer-key", b64Key);
        return b64Key;
      }
    };
    return encryptionKey;
  }

  private Function<Map<String, String>, String> provideSSEKeyMD5() {
    final Function<Map<String, String>, String> customerKeyHash =
        new Function<Map<String, String>, String>() {
          @Override
          public String apply(@Nullable final Map<String, String> input) {
            return BaseEncoding.base64()
                .encode(Hashing.md5().newHasher().putBytes(OGModule.this.aesKey).hash().asBytes());
          }
        };
    return customerKeyHash;
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
        .usingRequestSentRetry(clientConfig.requestSentRetry).withProtocols(clientConfig.protocols)
        .withCipherSuites(clientConfig.cipherSuites).withKeyStore(clientConfig.keyStore)
        .withKeyStorePassword(clientConfig.keyStorePassword)
        .withKeyPassword(clientConfig.keyPassword).withTrustStore(clientConfig.trustStore)
        .withTrustStorePassword(clientConfig.trustStorePassword)
        .usingTrustSelfSignedCertificates(clientConfig.trustSelfSignedCertificates)
        .withDnsCacheTtl(clientConfig.dnsCacheTtl)
        .withDnsCacheNegativeTtl(clientConfig.dnsCacheNegativeTtl)
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
      @Named("write.container") final Function<Map<String, String>, String> container,
      @Nullable @Named("api.version") final String apiVersion,
      @Nullable @WriteObjectName final Function<Map<String, String>, String> object,
      @WriteHeaders final Map<String, Function<Map<String, String>, String>> headers,
      @Named("write.context") final List<Function<Map<String, String>, String>> context,
      @WriteBody final Function<Map<String, String>, Body> body,
      @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
      @Named("virtualhost") final boolean virtualHost,
      @Named("write.sseCDestination") final boolean encryptDestinationObject,
      @Nullable @Named("write.retention") final Function<Map<String, String>, Long> retention,
      @Nullable @Named("write.legalHold") final Supplier<Function<Map<String, String>, String>> legalHold,
      @Nullable @Named("write.contentMd5") final boolean contentMd5,
      @Nullable @Named("write.delimiter") final Function<Map<String, String>, String> delimiter) {

    if (encryptDestinationObject) {
      checkArgument(this.config.data == DataType.ZEROES,
          "If SSE-C is enabled, data must be ZEROES [%s]", this.config.data);
    }

    if (contentMd5) {
      checkArgument(this.config.data == DataType.ZEROES,
          "If contentMD5 is set, data must be ZEROES [%s]", this.config.data);
    }
    final Map<String, Function<Map<String, String>, String>> queryParameters = Collections.emptyMap();

    if (encryptDestinationObject) {
      if (!headers.containsKey("x-amz-server-side-encryption-customer-algorithm")) {
        headers.put("x-amz-server-side-encryption-customer-algorithm",
            provideSSEEncryptionAlgorithm());
      }
      if (!headers.containsKey("x-amz-server-side-encryption-customer-key")) {
        headers.put("x-amz-server-side-encryption-customer-key", provideSSEEncryptionKey());
      }
      if (!headers.containsKey("x-amz-server-side-encryption-customer-key-MD5")) {
        headers.put("x-amz-server-side-encryption-customer-key-MD5", provideSSEKeyMD5());
      }
    }

    return createRequestSupplier(Operation.WRITE, id, Method.PUT, scheme, host, port, uriRoot,
        container, apiVersion, object, queryParameters, headers, context, null, body, credentials,
        virtualHost, retention, legalHold, contentMd5, delimiter);
  }


  @Provides
  @Singleton
  @Named("extend_retention")
  public Supplier<Request> provideExtendRetention(
          @Named("request.id") final Function<Map<String, String>, String> id, final Scheme scheme,
          @ReadHost final Function<Map<String, String>, String> host,
          @Nullable @Named("port") final Integer port,
          @Nullable @Named("uri.root") final String uriRoot,
          @Named("read.container") final Function<Map<String, String>, String> container,
          @Nullable @Named("api.version") final String apiVersion,
          @Nullable @ReadObjectName final Function<Map<String, String>, String> object,
          @WriteHeaders final Map<String, Function<Map<String, String>, String>> headers,
          @Named("retention_extension.context") final List<Function<Map<String, String>, String>> context,
          @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
          @Nullable @Named("extend.retention") final Function<Map<String, String>, Long> retentionExtension,
          @Named("virtualhost") final boolean virtualHost) {

    final Map<String, Function<Map<String, String>, String>> queryParameters =
            provideRetentionExtensionQueryParameters();

    final Supplier<Body> bodySupplier = Suppliers.of(Bodies.none());
    final Function<Map<String, String>, Body> body = MoreFunctions.forSupplier(bodySupplier);

    return createRequestSupplier(Operation.EXTEND_RETENTION, id, Method.POST, scheme, host, port,
            uriRoot, container, apiVersion, object, queryParameters, headers, context, null, body,
            credentials, virtualHost, retentionExtension, null, false, null);
  }

  @Provides
  @Singleton
  @Named("objectRestore")
  public Supplier<Request> provideObjectRestore(
          @Named("request.id") final Function<Map<String, String>, String> id, final Scheme scheme,
          @ReadHost final Function<Map<String, String>, String> host,
          @Nullable @Named("port") final Integer port,
          @Nullable @Named("uri.root") final String uriRoot,
          @Named("objectRestore.container") final Function<Map<String, String>, String> container,
          @Nullable @Named("api.version") final String apiVersion,
          @Nullable @ReadObjectName final Function<Map<String, String>, String> object,
          @ObjectRestoreHeaders final Map<String, Function<Map<String, String>, String>> headers,
          @Named("objectRestore.context") final List<Function<Map<String, String>, String>> context,
          @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
          @Named("virtualhost") final boolean virtualHost) {

    final Function<Map<String, String>, Body> body = createObjectRestoreBodySupplier();

    final Map<String, Function<Map<String, String>, String>> queryParameters = Maps.newLinkedHashMap();
    queryParameters.put(QueryParameters.OBJECT_RESTORE_PARAMETER,
            new Function<Map<String, String>, String>() {
              @Override
              public String apply(final Map<String, String> context) {
                return null;
              }
            });

    return createRequestSupplier(Operation.OBJECT_RESTORE, id, Method.POST, scheme, host, port,
            uriRoot, container, apiVersion, object, queryParameters, headers, context, null, body,
            credentials, virtualHost, null, null, false, null);
  }

  @Provides
  @Singleton
  @Named("putContainerLifecycle")
  public Supplier<Request> providePutContainerLifecycle(
          @Named("request.id") final Function<Map<String, String>, String> id, final Scheme scheme,
          @ReadHost final Function<Map<String, String>, String> host,
          @Nullable @Named("port") final Integer port,
          @Nullable @Named("uri.root") final String uriRoot,
          @Named("putContainerLifecycle.container") final Function<Map<String, String>, String> container,
          @Nullable @Named("api.version") final String apiVersion,
          @PutContainerLifecycleHeaders final Map<String, Function<Map<String, String>, String>> headers,
          @Named("putContainerLifecycle.context") final List<Function<Map<String, String>, String>> context,
          @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
          @Named("virtualhost") final boolean virtualHost) {

    final Function<Map<String, String>, Body> body = createPutContainerLifecycleBodySupplier();

    final Map<String, Function<Map<String, String>, String>> queryParameters = Maps.newLinkedHashMap();
    queryParameters.put(QueryParameters.BUCKET_LIFECYCLE_PARAMETER,
            new Function<Map<String, String>, String>() {
              @Override
              public String apply(final Map<String, String> context) {
                return null;
              }
            });

    return createRequestSupplier(Operation.PUT_CONTAINER_LIFECYCLE, id, Method.PUT, scheme, host, port,
            uriRoot, container, apiVersion, null, queryParameters, headers, context, null, body,
            credentials, virtualHost, null, null, true, null);
  }

  @Provides
  @Singleton
  @Named("putContainerProtection")
  public Supplier<Request> providePutContainerProtection(
          @Named("request.id") final Function<Map<String, String>, String> id, final Scheme scheme,
          @ReadHost final Function<Map<String, String>, String> host,
          @Nullable @Named("port") final Integer port,
          @Nullable @Named("uri.root") final String uriRoot,
          @Named("putContainerProtection.container") final Function<Map<String, String>, String> container,
          @Nullable @Named("api.version") final String apiVersion,
          @PutContainerProtectionHeaders final Map<String, Function<Map<String, String>, String>> headers,
          @Named("putContainerProtection.context") final List<Function<Map<String, String>, String>> context,
          @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
          @Named("virtualhost") final boolean virtualHost) {

    final Function<Map<String, String>, Body> body = createPutContainerProtectionSupplier();

    final Map<String, Function<Map<String, String>, String>> queryParameters = Maps.newLinkedHashMap();

    queryParameters.put(QueryParameters.BUCKET_PROTECTION_PARAMETER,
            new Function<Map<String, String>, String>() {
              @Override
              public String apply(final Map<String, String> context) {
                return null;
              }
            });

    return createRequestSupplier(Operation.PUT_CONTAINER_PROTECTION, id, Method.PUT, scheme, host, port,
            uriRoot, container, apiVersion, null, queryParameters, headers, context, null, body,
            credentials, virtualHost, null, null, true, null);
  }

  @Provides
  @Singleton
  @Named("getContainerLifecycle")
  public Supplier<Request> provideGetContainerLifecycle(
          @Named("request.id") final Function<Map<String, String>, String> id, final Scheme scheme,
          @ReadHost final Function<Map<String, String>, String> host,
          @Nullable @Named("port") final Integer port,
          @Nullable @Named("uri.root") final String uriRoot,
          @Named("getContainerLifecycle.container") final Function<Map<String, String>, String> container,
          @Nullable @Named("api.version") final String apiVersion,
          @GetContainerLifecycleHeaders final Map<String, Function<Map<String, String>, String>> headers,
          @Named("getContainerLifecycle.context") final List<Function<Map<String, String>, String>> context,
          @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
          @Named("virtualhost") final boolean virtualHost) {


    final Map<String, Function<Map<String, String>, String>> queryParameters = Maps.newLinkedHashMap();
    queryParameters.put(QueryParameters.BUCKET_LIFECYCLE_PARAMETER,
            new Function<Map<String, String>, String>() {
              @Override
              public String apply(final Map<String, String> context) {
                return null;
              }
            });

    return createRequestSupplier(Operation.GET_CONTAINER_LIFECYCLE, id, Method.GET, scheme, host, port,
            uriRoot, container, apiVersion, null, queryParameters, headers, context, null,
            null, credentials, virtualHost, null, null, false, null);
  }

  @Provides
  @Singleton
  @Named("getContainerProtection")
  public Supplier<Request> provideGetContainerProtection(
          @Named("request.id") final Function<Map<String, String>, String> id, final Scheme scheme,
          @ReadHost final Function<Map<String, String>, String> host,
          @Nullable @Named("port") final Integer port,
          @Nullable @Named("uri.root") final String uriRoot,
          @Named("getContainerProtection.container") final Function<Map<String, String>, String> container,
          @Nullable @Named("api.version") final String apiVersion,
          @GetContainerProtectionHeaders final Map<String, Function<Map<String, String>, String>> headers,
          @Named("getContainerProtection.context") final List<Function<Map<String, String>, String>> context,
          @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
          @Named("virtualhost") final boolean virtualHost) {


    final Map<String, Function<Map<String, String>, String>> queryParameters = Maps.newLinkedHashMap();

    queryParameters.put(QueryParameters.BUCKET_PROTECTION_PARAMETER,
            new Function<Map<String, String>, String>() {
              @Override
              public String apply(final Map<String, String> context) {
                return null;
              }
            });

    return createRequestSupplier(Operation.GET_CONTAINER_PROTECTION, id, Method.GET, scheme, host, port,
            uriRoot, container, apiVersion, null, queryParameters, headers, context, null,
            null, credentials, virtualHost, null, null, false, null);
  }
  @Provides
  @Singleton
  @Named("writeCopy")
  public Supplier<Request> provideWriteCopy(
      @Named("request.id") final Function<Map<String, String>, String> id, final Api api,
      final Scheme scheme, @WriteHost final Function<Map<String, String>, String> host,
      @Nullable @Named("port") final Integer port,
      @Nullable @Named("uri.root") final String uriRoot,
      @Named("writeCopy.container") final Function<Map<String, String>, String> container,
      @Named("writeCopy.sourceContainer") final Function<Map<String, String>, String> sourceContainer,
      @Nullable @Named("api.version") final String apiVersion,
      @Nullable @WriteObjectName final Function<Map<String, String>, String> writeObject,
      @Named("writeCopySource.context") final List<Function<Map<String, String>, String>> sseReadContext,
      @Nullable @SourceReadObjectName final Function<Map<String, String>, String> sseSourceReadObject,
      @WriteCopyHeaders final Map<String, Function<Map<String, String>, String>> headers,
      @Named("writeCopy.context") final List<Function<Map<String, String>, String>> context,
      @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
      @Named("virtualhost") final boolean virtualHost,
      @Named("writeCopy.sseCSource") final boolean encryptedSourceObject,
      @Named("writeCopy.sseCDestination") final boolean encryptDestinationObject,
      @Nullable @Named("writeCopy.delimiter") final Function<Map<String, String>, String> delimiter) {
    if (encryptedSourceObject || encryptDestinationObject) {
      checkArgument(this.config.data == DataType.ZEROES,
          "If SSE-C is enabled, data must be ZEROES [%s]", this.config.data);
    }

    final Map<String, Function<Map<String, String>, String>> queryParameters =
        Collections.emptyMap();

    final Function<Map<String, String>, String> copySource =
        new Function<Map<String, String>, String>() {
          @Override
          public String apply(@Nullable final Map<String, String> input) {

            final String objectName = sseSourceReadObject.apply(input);
            sourceContainer.apply(input);
            final String containerSuffix =
                input.get(Context.X_OG_SSE_SOURCE_OBJECT_CONTAINER_SUFFIX);
            final String containerPrefix = input.get(Context.X_OG_SOURCE_CONTAINER_PREFIX);
            // todo: update this to handle copy object API for openstack. Currently, only support s3
            // API
            checkArgument(api == Api.S3,
                "WriteCopy operation is only supported for S3 API. Request API [%s]", api);
            final String sourceUri;
            if (containerSuffix != null && Integer.parseInt(containerSuffix) != -1) {
              sourceUri = "/" + containerPrefix + containerSuffix + "/" + objectName;
            } else {
              sourceUri = "/" + containerPrefix + "/" + objectName;
            }
            input.put(Context.X_OG_SSE_SOURCE_URI, sourceUri);
            return sourceUri;
          }
        };

    if (encryptDestinationObject) {

      checkArgument(api == Api.S3,
          "WriteCopy operation is only supported for S3 API. Request API [%s]", api);
      if (!headers.containsKey("x-amz-server-side-encryption-customer-algorithm")) {
        headers.put("x-amz-server-side-encryption-customer-algorithm",
            provideSSEEncryptionAlgorithm());
      }
      if (!headers.containsKey("x-amz-server-side-encryption-customer-key")) {
        headers.put("x-amz-server-side-encryption-customer-key", provideSSEEncryptionKey());
      }
      if (!headers.containsKey("x-amz-server-side-encryption-customer-key-MD5")) {
        headers.put("x-amz-server-side-encryption-customer-key-MD5", provideSSEKeyMD5());
      }
    }
    if (encryptedSourceObject) {
      if (!headers.containsKey("x-amz-copy-source-server-side-encryption-customer-algorithm")) {
        headers.put("x-amz-copy-source-server-side-encryption-customer-algorithm",
            provideSSEEncryptionAlgorithm());
      }
      if (!headers.containsKey("x-amz-copy-source-server-side-encryption-customer-key")) {
        headers.put("x-amz-copy-source-server-side-encryption-customer-key",
            provideSSEEncryptionKey());
      }
      if (!headers.containsKey("x-amz-copy-source-server-side-encryption-customer-key-MD5")) {
        headers.put("x-amz-copy-source-server-side-encryption-customer-key-MD5",
            provideSSEKeyMD5());
      }
    }
    headers.put("x-amz-copy-source", copySource);

    return createRequestSupplier(Operation.WRITE_COPY, id, Method.PUT, scheme, host, port, uriRoot,
        container, apiVersion, writeObject, queryParameters, headers, context, sseReadContext, null,
        credentials, virtualHost, null, null, false, delimiter);
  }

  @Provides
  @Singleton
  @Named("overwrite")
  public Supplier<Request> provideOverwrite(
      @Named("request.id") final Function<Map<String, String>, String> id, final Api api,
      final Scheme scheme, @OverwriteHost final Function<Map<String, String>, String> host,
      @Nullable @Named("port") final Integer port,
      @Nullable @Named("uri.root") final String uriRoot,
      @Named("overwrite.container") final Function<Map<String, String>, String> container,
      @Nullable @Named("api.version") final String apiVersion,
      @Nullable @OverwriteObjectName final Function<Map<String, String>, String> object,
      @OverwriteHeaders final Map<String, Function<Map<String, String>, String>> headers,
      @Named("overwrite.context") final List<Function<Map<String, String>, String>> context,
      @OverwriteBody final Function<Map<String, String>, Body> body,
      @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
      @Named("virtualhost") final boolean virtualHost,
      @Named("overwrite.weight") final double overwriteWeight,
      @Named("overwrite.sseCDestination") final boolean encryptDestinationObject,
      @Nullable @Named("overwrite.retention") final Function<Map<String, String>, Long> retention,
      @Nullable @Named("overwrite.legalHold") final Supplier<Function<Map<String, String>, String>> legalHold,
      @Nullable @Named("overwrite.contentMd5") final boolean contentMd5) throws Exception {

    if (encryptDestinationObject) {
      checkArgument(this.config.data == DataType.ZEROES,
          "If SSE-C is enabled, data must be ZEROES [%s]", this.config.data);
    }

    // SOH needs to use a special response consumer to extract the returned object id
    if (Api.SOH == api && overwriteWeight > 0.0) {
      throw new Exception("Overwrites are not compatible with SOH");
    }

    if (contentMd5) {
      checkArgument(this.config.data == DataType.ZEROES,
          "If contentMD5 is set, data must be ZEROES [%s]", this.config.data);
    }

    final Map<String, Function<Map<String, String>, String>> queryParameters =
        Collections.emptyMap();

    if (encryptDestinationObject) {
      if (!headers.containsKey("x-amz-server-side-encryption-customer-algorithm")) {
        headers.put("x-amz-server-side-encryption-customer-algorithm",
            provideSSEEncryptionAlgorithm());
      }
      if (!headers.containsKey("x-amz-server-side-encryption-customer-key")) {
        headers.put("x-amz-server-side-encryption-customer-key", provideSSEEncryptionKey());
      }
      if (!headers.containsKey("x-amz-server-side-encryption-customer-key-MD5")) {
        headers.put("x-amz-server-side-encryption-customer-key-MD5", provideSSEKeyMD5());
      }
    }
    return createRequestSupplier(Operation.OVERWRITE, id, Method.PUT, scheme, host, port, uriRoot,
        container, apiVersion, object, queryParameters, headers, context, null, body, credentials,
        virtualHost, retention, legalHold, contentMd5, null);
  }

  @Provides
  @Singleton
  @Named("read")
  public Supplier<Request> provideRead(
      @Named("request.id") final Function<Map<String, String>, String> id, final Scheme scheme,
      @ReadHost final Function<Map<String, String>, String> host,
      @Nullable @Named("port") final Integer port,
      @Nullable @Named("uri.root") final String uriRoot,
      @Named("read.container") final Function<Map<String, String>, String> container,
      @Nullable @Named("api.version") final String apiVersion,
      @Nullable @ReadObjectName final Function<Map<String, String>, String> object,
      @ReadHeaders final Map<String, Function<Map<String, String>, String>> headers,
      @Named("read.context") final List<Function<Map<String, String>, String>> context,
      @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
      @Named("virtualhost") final boolean virtualHost,
      @Named("read.sseCSource") final boolean encryptedSourceObject) {

    final Map<String, Function<Map<String, String>, String>> queryParameters =
        Collections.emptyMap();

    if (encryptedSourceObject) {
      if (!headers.containsKey("x-amz-server-side-encryption-customer-algorithm")) {
        headers.put("x-amz-server-side-encryption-customer-algorithm",
            provideSSEEncryptionAlgorithm());
      }
      if (!headers.containsKey("x-amz-server-side-encryption-customer-key")) {
        headers.put("x-amz-server-side-encryption-customer-key", provideSSEEncryptionKey());
      }
      if (!headers.containsKey("x-amz-server-side-encryption-customer-key-MD5")) {
        headers.put("x-amz-server-side-encryption-customer-key-MD5", provideSSEKeyMD5());
      }
    }

    final Supplier<Body> bodySupplier = Suppliers.of(Bodies.none());
    final Function<Map<String, String>, Body> body = MoreFunctions.forSupplier(bodySupplier);

    return createRequestSupplier(Operation.READ, id, Method.GET, scheme, host, port, uriRoot,
        container, apiVersion, object, queryParameters, headers, context, null, body, credentials,
        virtualHost, null, null, false, null);
  }

  @Provides
  @Singleton
  @Named("write_legalhold")
  public Supplier<Request> provideWriteLegalhold(
          @Named("request.id") final Function<Map<String, String>, String> id, final Scheme scheme,
          @ReadHost final Function<Map<String, String>, String> host,
          @Nullable @Named("port") final Integer port,
          @Nullable @Named("uri.root") final String uriRoot,
          @Named("read.container") final Function<Map<String, String>, String> container,
          @Nullable @Named("api.version") final String apiVersion,
          @Nullable @ReadObjectName final Function<Map<String, String>, String> object,
          @ReadHeaders final Map<String, Function<Map<String, String>, String>> headers,
          @Named("write_legalhold.context") final List<Function<Map<String, String>, String>> context,
          @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
          @Nullable @Named("add.legalHold") final Supplier<Function<Map<String, String>, String>> legalhold,
          @Named("virtualhost") final boolean virtualHost) {

    final Map<String, Function<Map<String, String>, String>> queryParameters =
            provideLegalHoldQueryParameters(false);

    final Supplier<Body> bodySupplier = Suppliers.of(Bodies.none());
    final Function<Map<String, String>, Body> body = MoreFunctions.forSupplier(bodySupplier);

    return createRequestSupplier(Operation.WRITE_LEGAL_HOLD, id, Method.POST, scheme, host, port,
            uriRoot, container, apiVersion, object, queryParameters, headers, context, null, body,
            credentials, virtualHost, null, legalhold, false, null);
  }



  @Provides
  @Singleton
  @Named("delete_legalhold")
  public Supplier<Request> provideDeleteLegalhold(
          @Named("request.id") final Function<Map<String, String>, String> id, final Scheme scheme,
          @ReadHost final Function<Map<String, String>, String> host,
          @Nullable @Named("port") final Integer port,
          @Nullable @Named("uri.root") final String uriRoot,
          @Named("read.container") final Function<Map<String, String>, String> container,
          @Nullable @Named("api.version") final String apiVersion,
          @Nullable @ReadObjectName final Function<Map<String, String>, String> object,
          @ReadHeaders final Map<String, Function<Map<String, String>, String>> headers,
          @Named("delete_legalhold.context") final List<Function<Map<String, String>, String>> context,
          @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
          @Nullable @Named("delete.legalHold") final Supplier<Function<Map<String, String>, String>> legalhold,
          @Named("virtualhost") final boolean virtualHost) {
    final Map<String, Function<Map<String, String>, String>> queryParameters =
        provideLegalHoldQueryParameters(true);

    final Supplier<Body> bodySupplier = Suppliers.of(Bodies.none());
    final Function<Map<String, String>, Body> body = MoreFunctions.forSupplier(bodySupplier);

    return createRequestSupplier(Operation.DELETE_LEGAL_HOLD, id, Method.POST, scheme, host, port,
        uriRoot, container, apiVersion, object, queryParameters, headers, context, null, body,
        credentials, virtualHost, null, legalhold, false, null);
  }

  @Provides
  @Singleton
  @Named("read_legalhold")
  public Supplier<Request> provideReadLegalholds(
      @Named("request.id") final Function<Map<String, String>, String> id, final Scheme scheme,
      @ReadHost final Function<Map<String, String>, String> host,
      @Nullable @Named("port") final Integer port,
      @Nullable @Named("uri.root") final String uriRoot,
      @Named("read.container") final Function<Map<String, String>, String> container,
      @Nullable @Named("api.version") final String apiVersion,
      @Nullable @ReadObjectName final Function<Map<String, String>, String> object,
      @ReadHeaders final Map<String, Function<Map<String, String>, String>> headers,
      @Named("read.context") final List<Function<Map<String, String>, String>> context,
      @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
      @Named("virtualhost") final boolean virtualHost) {

    final Map<String, Function<Map<String, String>, String>> queryParameters = Maps.newHashMap();
    final Supplier<Body> bodySupplier = Suppliers.of(Bodies.none());
    final Function<Map<String, String>, Body> body = MoreFunctions.forSupplier(bodySupplier);

    queryParameters.put("legalHold", new Function<Map<String, String>, String>() {
      @Override
      public String apply(final Map<String, String> context) {
        return null;
      }
    });

    return createRequestSupplier(Operation.READ_LEGAL_HOLD, id, Method.GET, scheme, host, port,
        uriRoot, container, apiVersion, object, queryParameters, headers, context, null, body,
        credentials, virtualHost, null, null, false, null);
  }



  @Provides
  @Singleton
  @Named("metadata")
  public Supplier<Request> provideMetadata(
      @Named("request.id") final Function<Map<String, String>, String> id, final Scheme scheme,
      @MetadataHost final Function<Map<String, String>, String> host,
      @Nullable @Named("port") final Integer port,
      @Nullable @Named("uri.root") final String uriRoot,
      @Named("metadata.container") final Function<Map<String, String>, String> container,
      @Nullable @Named("api.version") final String apiVersion,
      @Nullable @MetadataObjectName final Function<Map<String, String>, String> object,
      @MetadataHeaders final Map<String, Function<Map<String, String>, String>> headers,
      @Named("metadata.context") final List<Function<Map<String, String>, String>> context,
      @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
      @Named("virtualhost") final boolean virtualHost,
      @Named("metadata.sseCSource") final boolean encryptedSourceObject) {

    final Map<String, Function<Map<String, String>, String>> queryParameters =
        Collections.emptyMap();

    final Supplier<Body> bodySupplier = Suppliers.of(Bodies.none());
    final Function<Map<String, String>, Body> body = MoreFunctions.forSupplier(bodySupplier);

    if (encryptedSourceObject) {
      if (!headers.containsKey("x-amz-server-side-encryption-customer-algorithm")) {
        headers.put("x-amz-server-side-encryption-customer-algorithm",
            provideSSEEncryptionAlgorithm());
      }
      if (!headers.containsKey("x-amz-server-side-encryption-customer-key")) {
        headers.put("x-amz-server-side-encryption-customer-key", provideSSEEncryptionKey());
      }
      if (!headers.containsKey("x-amz-server-side-encryption-customer-key-MD5")) {
        headers.put("x-amz-server-side-encryption-customer-key-MD5", provideSSEKeyMD5());
      }
    }
    return createRequestSupplier(Operation.METADATA, id, Method.HEAD, scheme, host, port, uriRoot,
        container, apiVersion, object, queryParameters, headers, context, null, body, credentials,
        virtualHost, null, null, false, null);
  }

  @Provides
  @Singleton
  @Named("delete")
  public Supplier<Request> provideDelete(
      @Named("request.id") final Function<Map<String, String>, String> id, final Scheme scheme,
      @DeleteHost final Function<Map<String, String>, String> host,
      @Nullable @Named("port") final Integer port,
      @Nullable @Named("uri.root") final String uriRoot,
      @Named("delete.container") final Function<Map<String, String>, String> container,
      @Nullable @Named("api.version") final String apiVersion,
      @Nullable @DeleteObjectName final Function<Map<String, String>, String> object,
      @DeleteHeaders final Map<String, Function<Map<String, String>, String>> headers,
      @Named("delete.context") final List<Function<Map<String, String>, String>> context,
      @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
      @Named("virtualhost") final boolean virtualHost) {

    final Map<String, Function<Map<String, String>, String>> queryParameters =
        Collections.emptyMap();

    final Supplier<Body> bodySupplier = Suppliers.of(Bodies.none());
    final Function<Map<String, String>, Body> body = MoreFunctions.forSupplier(bodySupplier);

    return createRequestSupplier(Operation.DELETE, id, Method.DELETE, scheme, host, port, uriRoot,
        container, apiVersion, object, queryParameters, headers, context, null, body, credentials,
        virtualHost, null, null, false, null);
  }


  @Provides
  @Singleton
  @Named("containerList")
  public Supplier<Request> provideContainerList(
      @Named("request.id") final Function<Map<String, String>, String> id, final Api api,
      final Scheme scheme, @ContainerListHost final Function<Map<String, String>, String> host,
      @Nullable @Named("port") final Integer port,
      @Nullable @Named("uri.root") final String uriRoot,
      @ContainerListHeaders final Map<String, Function<Map<String, String>, String>> headers,
      @Nullable @Named("api.version") final String apiVersion,
      @Named("containerList.context") final List<Function<Map<String, String>, String>> context,
      @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
      @Named("virtualhost") final boolean virtualHost) throws Exception {

    final Map<String, Function<Map<String, String>, String>> queryParameters =
        Collections.emptyMap();

    final Supplier<Body> bodySupplier = Suppliers.of(Bodies.none());
    final Function<Map<String, String>, Body> body = MoreFunctions.forSupplier(bodySupplier);

    // null container since request is on the service http://<accesser ip>/
    return createRequestSupplier(Operation.CONTAINER_LIST, id, Method.GET, scheme, host, port,
        uriRoot, null, apiVersion, null, queryParameters, headers, context, null, body, credentials,
        virtualHost, null, null, false, null);
  }

  @Provides
  @Singleton
  @Named("containerCreate")
  public Supplier<Request> provideContainerCreate(
      @Named("request.id") final Function<Map<String, String>, String> id, final Api api,
      final Scheme scheme, @ContainerListHost final Function<Map<String, String>, String> host,
      @Nullable @Named("port") final Integer port,
      @Nullable @Named("uri.root") final String uriRoot,
      @Named("containerCreate.container") final Function<Map<String, String>, String> container,
      @Nullable @Named("api.version") final String apiVersion,
      @ContainerCreateHeaders final Map<String, Function<Map<String, String>, String>> headers,
      @Named("containerCreate.context") final List<Function<Map<String, String>, String>> context,
      @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
      @Named("virtualhost") final boolean virtualHost,
      @Nullable @Named("containerCreate.retention") final Function<Map<String, String>, Long> retention)
      throws Exception {

    final Map<String, Function<Map<String, String>, String>> queryParameters =
        Collections.emptyMap();

    final Supplier<Body> bodySupplier = Suppliers.of(Bodies.none());
    final Function<Map<String, String>, Body> body = MoreFunctions.forSupplier(bodySupplier);

    // todo: container creation operation only need the retention. Do we need to support
    // the vault or container creation with retention in OG when worm feature is supported in
    // container mode
    return createRequestSupplier(Operation.CONTAINER_CREATE, id, Method.PUT, scheme, host, port,
        uriRoot, container, apiVersion, null, queryParameters, headers, context, null, body,
        credentials, virtualHost, retention, null, false, null);

  }

  private Supplier<Request> createRequestSupplier(final Operation operation,
      @Named("request.id") final Function<Map<String, String>, String> id, final Method method,
      final Scheme scheme, final Function<Map<String, String>, String> host, final Integer port,
      final String uriRoot, final Function<Map<String, String>, String> container,
      final String apiVersion, final Function<Map<String, String>, String> object,
      final Map<String, Function<Map<String, String>, String>> queryParameters,
      final Map<String, Function<Map<String, String>, String>> headers,
      final List<Function<Map<String, String>, String>> context,
      final List<Function<Map<String, String>, String>> sseSourceContext,
      final Function<Map<String, String>, Body> body,
      final Function<Map<String, String>, Credential> credentials, final Boolean virtualHost,
      final Function<Map<String, String>, Long> retention, final Supplier<Function<Map<String, String>, String>> legalHold,
      final boolean contentMd5, final Function<Map<String, String>, String> delimiter) {

    return new RequestSupplier(operation, id, method, scheme, host, port, uriRoot, container,
        apiVersion, object, queryParameters, false, headers, context, sseSourceContext, credentials,
        body, virtualHost, retention, legalHold, contentMd5, delimiter);
  }





  @Provides
  @Singleton
  @Named("multipartWrite")
  public Supplier<Request> provideMultipartWrite(
      @Named("request.id") final Function<Map<String, String>, String> id, final Api api,
      final Scheme scheme, @MultipartWriteHost final Function<Map<String, String>, String> host,
      @Nullable @Named("port") final Integer port,
      @Nullable @Named("uri.root") final String uriRoot,
      @Named("multipartWrite.container") final Function<Map<String, String>, String> container,
      @Nullable @Named("api.version") final String apiVersion,
      @Nullable @MultipartWriteObjectName final Function<Map<String, String>, String> object,
      @Named("multipartWrite.partSize") final Function<Map<String, String>, Long> partSize,
      @Named("multipartWrite.partsPerSession") final Function<Map<String, String>, Integer> partsPerSession,
      @Named("multipartWrite.targetSessions") final int targetSessions,
      @MultipartWriteHeaders final Map<String, Function<Map<String, String>, String>> headers,
      @Named("multipartWrite.context") final List<Function<Map<String, String>, String>> context,
      @MultiPartWriteBody final Function<Map<String, String>, Body> body,
      @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
      @Named("virtualhost") final boolean virtualHost,
      @Nullable @Named("multipartWrite.retention") final Function<Map<String, String>, Long> retention,
      @Nullable @Named("multipartWrite.legalHold") final Supplier<Function<Map<String, String>, String>> legalHold,
      @Named("multipartWrite.sseCDestination") final boolean encryptDestinationObject,
      @Nullable @Named("multipartWrite.contentMd5") final boolean contentMd5,
      @Nullable @Named("multipartWrite.delimiter") final Function<Map<String, String>, String> delimiter) {

    if (encryptDestinationObject) {
      checkArgument(this.config.data == DataType.ZEROES,
              "If SSE-C is enabled, data must be ZEROES [%s]", this.config.data);
    }

    if (contentMd5) {
      checkArgument(this.config.data == DataType.ZEROES,
              "If contentMD5 is set, data must be ZEROES [%s]", this.config.data);
    }

    final Map<String, Function<Map<String, String>, String>> queryParameters =
        Collections.emptyMap();

    if (encryptDestinationObject) {
      if (!headers.containsKey("x-amz-server-side-encryption-customer-algorithm")) {
        headers.put("x-amz-server-side-encryption-customer-algorithm",
            provideSSEEncryptionAlgorithm());
      }
      if (!headers.containsKey("x-amz-server-side-encryption-customer-key")) {
        headers.put("x-amz-server-side-encryption-customer-key", provideSSEEncryptionKey());
      }
      if (!headers.containsKey("x-amz-server-side-encryption-customer-key-MD5")) {
        headers.put("x-amz-server-side-encryption-customer-key-MD5", provideSSEKeyMD5());
      }
    }
    // todo: Not sure if sending the above headers with multipart complete request will cause
    // problems. As per the
    // AWS s3 API guide they are not required for complete request.

    return createMultipartRequestSupplier(id, scheme, host, port, uriRoot, container, apiVersion,
        object, partSize, partsPerSession, targetSessions, queryParameters, headers, context, body,
        retention, legalHold, credentials, virtualHost, contentMd5, delimiter);
  }

  private Supplier<Request> createMultipartRequestSupplier(
      @Named("request.id") final Function<Map<String, String>, String> id, final Scheme scheme,
      final Function<Map<String, String>, String> host, final Integer port, final String uriRoot,
      final Function<Map<String, String>, String> container, final String apiVersion,
      final Function<Map<String, String>, String> object,
      final Function<Map<String, String>, Long> partSize,
      final Function<Map<String, String>, Integer> partsPerSession, final int targetSessions,
      final Map<String, Function<Map<String, String>, String>> queryParameters,
      final Map<String, Function<Map<String, String>, String>> headers,
      final List<Function<Map<String, String>, String>> context,
      final Function<Map<String, String>, Body> body,
      @Nullable @Named("write.retention") final Function<Map<String, String>, Long> retention,
      @Nullable @Named("write.legalHold") final Supplier<Function<Map<String, String>, String>> legalHold,
      final Function<Map<String, String>, Credential> credentials, final boolean virtualHost,
      final boolean contentMd5, final Function<Map<String, String>, String> delimiter) {

    return new MultipartRequestSupplier(id, scheme, host, port, uriRoot, container, object,
        partSize, partsPerSession, targetSessions, queryParameters, false, headers, context,
        credentials, body, virtualHost, retention, legalHold, contentMd5, delimiter);
  }
}
