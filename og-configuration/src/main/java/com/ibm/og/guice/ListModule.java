/*
 * Copyright (c) IBM Corporation 2018. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.guice;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.ibm.og.api.Body;
import com.ibm.og.api.Method;
import com.ibm.og.api.Operation;
import com.ibm.og.api.Request;
import com.ibm.og.guice.annotation.ListHeaders;
import com.ibm.og.guice.annotation.ListHost;
import com.ibm.og.http.Api;
import com.ibm.og.http.Bodies;
import com.ibm.og.http.Credential;
import com.ibm.og.http.Scheme;
import com.ibm.og.json.ChoiceConfig;
import com.ibm.og.json.ListDelimiterConfig;
import com.ibm.og.json.ListSessionConfig;
import com.ibm.og.json.ObjectDelimiterConfig;
import com.ibm.og.json.OGConfig;
import com.ibm.og.json.OperationConfig;
import com.ibm.og.json.PrefixConfig;
import com.ibm.og.json.SelectionConfig;
import com.ibm.og.json.SelectionType;
import com.ibm.og.object.ObjectManager;
import com.ibm.og.s3.ListOperationsSupplier;
import com.ibm.og.supplier.RandomSupplier;
import com.ibm.og.supplier.ReadObjectNameFunction;
import com.ibm.og.supplier.Suppliers;
import com.ibm.og.test.LoadTestSubscriberExceptionHandler;
import com.ibm.og.util.Context;
import com.ibm.og.util.MoreFunctions;
import com.ibm.og.guice.annotation.ListQueryParameters;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndex;


/**
 * A guice configuration module for wiring up list operation components
 *
 * @since 1.8.4
 */

public class ListModule extends AbstractModule {

  private final OGConfig config;

  private final LoadTestSubscriberExceptionHandler handler;
  private final EventBus eventBus;

  /**
   * Creates an instance
   *
   * @param config json source configuration
   * @throws NullPointerException if config is null
   */
  public ListModule(final OGConfig config) {
    this.config = checkNotNull(config);
    this.handler = new LoadTestSubscriberExceptionHandler();
    this.eventBus = new EventBus(this.handler);
  }
  @Override
  protected void configure() {
    // nothing to bind here
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


  Map<String, Function<Map<String, String>, String>> provideWeightedQueryParameters(
          final Map<String, SelectionConfig<String>> operationQueryParameters) {
    final Map<String, Function<Map<String, String>, String>> queryParameters = Maps.newHashMap();

    for (final Map.Entry<String, SelectionConfig<String>> e : operationQueryParameters.entrySet()) {

      final SelectionConfig<String> queryParamValue = e.getValue();
      Function<Map<String, String>, String> queryParameterSupplier = createSelectionConfigSupplier(queryParamValue);
      queryParameters.put(e.getKey(), queryParameterSupplier);
    };
    return queryParameters;
  }

  private Function<Map<String, String>, String> providePrefix(final SelectionConfig<PrefixConfig> prefixes) {
    final Supplier<PrefixConfig> prefixConfigSupplier;
    final SelectionType selection = checkNotNull(prefixes.selection);

    // if retentions list is empty return null
    if (prefixes.choices.isEmpty()) {
      return null;
    }
    if (SelectionType.ROUNDROBIN == selection) {
      final List<PrefixConfig> prefixConfigList = Lists.newArrayList();
      prefixConfigSupplier = Suppliers.cycle(prefixConfigList);
    } else {
      final RandomSupplier.Builder<PrefixConfig> wrc = Suppliers.random();
      for (final ChoiceConfig<PrefixConfig> choice : prefixes.choices) {
        wrc.withChoice(choice.choice, choice.weight);
      }
      prefixConfigSupplier = wrc.build();
    }
    return new Function<Map<String, String>, String>() {

      @Override
      public String apply(final Map<String, String> input) {
        final PrefixConfig config = prefixConfigSupplier.get();
        final String objectName = input.get(Context.X_OG_OBJECT_NAME);
        String prefix = null;
        if (config.numChars > 0) {
          if (config.useMarker) {
            prefix = objectName.substring(0, config.numChars);
            input.put(Context.X_OG_LIST_PREFIX, prefix);
          } else {
            prefix = config.prefixString.substring(0, config.numChars);
            input.put(Context.X_OG_LIST_PREFIX, prefix);
          }
          checkArgument(prefix.length() <= 16, "prefix length cannot be greater than 16 characters");
        }
        return prefix;
       }
    };
  }


  private Function<Map<String, String>, String> provideDelimiter(final SelectionConfig<ListDelimiterConfig> delimiters) {
    final Supplier<ListDelimiterConfig> delimiterSupplier;
    final SelectionType selection = checkNotNull(delimiters.selection);

    // if retentions list is empty return null
    if (delimiters.choices.isEmpty()) {
      return null;
    }
    if (SelectionType.ROUNDROBIN == selection) {
      final List<ListDelimiterConfig> delimiterList = Lists.newArrayList();
      delimiterSupplier = Suppliers.cycle(delimiterList);
    } else {
      final RandomSupplier.Builder<ListDelimiterConfig> wrc = Suppliers.random();
      for (final ChoiceConfig<ListDelimiterConfig> choice : delimiters.choices) {
        wrc.withChoice(choice.choice, choice.weight);
      }
      delimiterSupplier = wrc.build();
    }
    return new Function<Map<String, String>, String>() {

      @Override
      public String apply(final Map<String, String> input) {
        final ListDelimiterConfig delimiter = delimiterSupplier.get();
        if (!delimiter.delimiterCharacter.equals("-1")) {
            input.put(Context.X_OG_LIST_DELIMITER, delimiter.delimiterCharacter);
            checkArgument(delimiter.delimiterCharacter.length() <= 1, "Delimiter length cannot be greater than 1 character");
      }
        return delimiter.delimiterCharacter;
      }
    };
  }



  @Provides
  @Singleton
  @ListQueryParameters
  public Map<String, Function<Map<String, String>, String>> provideListQueryParameters(
          final Api api) {
    final Map<String, Function<Map<String, String>, String>> queryParameters;

    queryParameters = provideQueryParameters(this.config.list.parameters);

    final Map<String, Function<Map<String, String>, String>> weightedQueryParameters =
            provideWeightedQueryParameters(this.config.list.weightedParameters);

    for (final Map.Entry<String, Function<Map<String, String>, String>> qp : weightedQueryParameters
            .entrySet()) {
      queryParameters.put(qp.getKey(), qp.getValue());
    }

    return queryParameters;
  }

  Map<String, Function<Map<String, String>, String>> provideQueryParameters(
          final Map<String, String> operationQueryParameters) {
    final Map<String, Function<Map<String, String>, String>> queryParameters = Maps.newHashMap();

    for (final Map.Entry<String, String> e : operationQueryParameters.entrySet()) {
      final Supplier<String> queryParameterSupplier = new Supplier<String>() {
        final private String queryParamValue = e.getValue();

        @Override
        public String get() {
          return this.queryParamValue;
        }
      };
      final Function<Map<String, String>, String> queryParameterFunction =
              MoreFunctions.forSupplier(queryParameterSupplier);
      queryParameters.put(e.getKey(), queryParameterFunction);
    }

    return queryParameters;
  }


  @Provides
  @Singleton
  @Named("list.context")
  public List<Function<Map<String, String>, String>> provideListObject(
          final ObjectManager objectManager) {
    Function<Map<String, String>, String> function;

    final OperationConfig operationConfig = checkNotNull(this.config.list);
    if (operationConfig.object.selection != null) {
      function = ModuleUtils.provideObject(operationConfig);
    } else {
      function = new ReadObjectNameFunction(objectManager);
    }

    return ImmutableList.of(function);
  }

  @Provides
  @Singleton
  @Named("list.container")
  public Function<Map<String, String>, String> provideListContainer() {
    if (this.config.list.container.prefix != null) {
      return ModuleUtils.provideContainer(this.config.list.container);
    } else {
      return ModuleUtils.provideContainer(this.config.container);
    }
  }

  private Function<Map<String, String>, ListSessionConfig> createListSessionConfigSupplier(
          final SelectionConfig<ListSessionConfig> selectionConfig) {

    if (SelectionType.ROUNDROBIN == selectionConfig.selection) {
      final List<ListSessionConfig> choiceList = Lists.newArrayList();
      for (final ChoiceConfig<ListSessionConfig> choice : selectionConfig.choices) {
        choiceList.add(choice.choice);
      }
      final Supplier<ListSessionConfig> configSupplier = Suppliers.cycle(choiceList);
      return MoreFunctions.forSupplier(configSupplier);
    }

    final RandomSupplier.Builder<ListSessionConfig> wrc = Suppliers.random();
    for (final ChoiceConfig<ListSessionConfig> choice : selectionConfig.choices) {
      wrc.withChoice(choice.choice, choice.weight);
    }
    final Supplier<ListSessionConfig> configSupplier = wrc.build();
    return MoreFunctions.forSupplier(configSupplier);
  }

  @Provides
  @Singleton
  @Named("listSession.Supplier")
  public Function<Map<String, String>, ListSessionConfig> provideListType(final SelectionConfig<String> listTypeConfig) {

    if (this.config.list.listSessionConfig == null) {

    }

    Function<Map<String, String>, ListSessionConfig> listTypeSupplier = createListSessionConfigSupplier(
            this.config.list.listSessionConfig);
    return listTypeSupplier;
  }


  @Provides
  @Singleton
  @Named("list.prefix")
  public Function<Map<String, String>, String> provideListPrefix(final SelectionConfig<PrefixConfig> listPrefixConfig) {
    if (this.config.list.prefix == null) {
      return null;
    }
    final SelectionConfig<PrefixConfig> prefixConfig = this.config.list.prefix;
    final List<ChoiceConfig<PrefixConfig>> prefixes = checkNotNull(prefixConfig.choices);
    checkArgument(!prefixes.isEmpty(), "List prefixes must not be empty");


    for (final ChoiceConfig<PrefixConfig> choice : prefixes) {
      checkNotNull(choice);
      checkNotNull(choice.choice);
    }
    return providePrefix(prefixConfig);
  }

  @Provides
  @Singleton
  @Named("list.delimiter")
  public Function<Map<String, String>, String> provideListDelimiter(final SelectionConfig<ListDelimiterConfig> listDelimiters) {
    if (this.config.list.listDelimiter == null) {
      return null;
    }
    final SelectionConfig<ListDelimiterConfig> delimiterConfig = this.config.list.listDelimiter;
    final List<ChoiceConfig<ListDelimiterConfig>> delimiters = checkNotNull(delimiterConfig.choices);
    checkArgument(!delimiters.isEmpty(), "List delimiters must not be empty");


    for (final ChoiceConfig<ListDelimiterConfig> choice : delimiters) {
      checkNotNull(choice);
      checkNotNull(choice.choice);
    }
    return provideDelimiter(delimiterConfig);
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
          @Named("list.container") final Function<Map<String, String>, String> container,
          @Nullable @Named("api.version") final String apiVersion,
          @ListHeaders final Map<String, Function<Map<String, String>, String>> headers,
          @Named("list.context") final List<Function<Map<String, String>, String>> context,
          @Nullable @Named("listSession.Supplier") final Function<Map<String, String>, ListSessionConfig> listTypeSupplier,
          @Nullable @Named("list.prefix") final Function<Map<String, String>, String> prefixSupplier,
          @Nullable @Named("list.delimiter") final Function<Map<String, String>, String> delimiterSupplier,
          @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
          @Named("virtualhost") final boolean virtualHost,
          final ObjectManager objectManager) throws Exception {

    final Supplier<Body> bodySupplier = Suppliers.of(Bodies.none());
    final Function<Map<String, String>, Body> body = MoreFunctions.forSupplier(bodySupplier);

    return createListRequestSupplier(api, this.config.list, Operation.LIST, id, Method.GET, scheme, host, port, uriRoot,
            container, apiVersion, null, queryParameters, headers, context, listTypeSupplier, prefixSupplier,
            delimiterSupplier, credentials, virtualHost, objectManager);
  }

  private Supplier<Request> createListRequestSupplier(final Api api, final OperationConfig config, final Operation operation,
    @Named("request.id") final Function<Map<String, String>, String> id, final Method method,
    final Scheme scheme, final Function<Map<String, String>, String> host, final Integer port,
    final String uriRoot, final Function<Map<String, String>, String> container,
    final String apiVersion, final Function<Map<String, String>, String> object,
    final Map<String, Function<Map<String, String>, String>> queryParameters,
    final Map<String, Function<Map<String, String>, String>> headers,
    final List<Function<Map<String, String>, String>> context,
    final Function<Map<String, String>, ListSessionConfig> listSessionConfigSupplier,
    final Function<Map<String, String>, String> prefixSupplier,
    final Function<Map<String, String>, String> delimiterSupplier,
    final Function<Map<String, String>, Credential> credentials, final Boolean virtualHost,
    final ObjectManager objectManager
    ) {

    return new ListOperationsSupplier(api, config, operation, id, method, scheme, host, port, uriRoot, container,
            apiVersion, object, queryParameters, false, headers, context, listSessionConfigSupplier, prefixSupplier,
            delimiterSupplier, credentials, virtualHost, objectManager);
  }

}
