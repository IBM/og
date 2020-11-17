/*
 * Copyright (c) IBM Corporation 2018. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.guice;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ibm.og.json.ChoiceConfig;
import com.ibm.og.json.ContainerConfig;
import com.ibm.og.json.LegalHold;
import com.ibm.og.json.ObjectConfig;
import com.ibm.og.json.OperationConfig;
import com.ibm.og.json.SelectionConfig;
import com.ibm.og.json.SelectionType;
import com.ibm.og.supplier.RandomSupplier;
import com.ibm.og.supplier.Suppliers;
import com.ibm.og.util.Context;
import com.ibm.og.util.MoreFunctions;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A class for common methods used in Guice Modules
 *
 * @since 1.8.4
 */

public class ModuleUtils {

  public static Function<Map<String, String>, String> provideObject(
          final OperationConfig operationConfig) {
    checkNotNull(operationConfig);
    final ObjectConfig objectConfig = checkNotNull(operationConfig.object);
    final String prefix = checkNotNull(objectConfig.prefix);
    final String nameSuffix = objectConfig.osuffix;
    final Supplier<Long> suffixes = createObjectSuffixes(objectConfig);
    final Supplier<Long> legalHoldSuffixes = createLegalHoldSuffixes(operationConfig.object);
    return new Function<Map<String, String>, String>() {
      @Override
      public String apply(final Map<String, String> context) {
        final String objectName = prefix + suffixes.get() + nameSuffix;
        context.put(Context.X_OG_OBJECT_NAME, objectName);
        context.put(Context.X_OG_SEQUENTIAL_OBJECT_NAME, "true");
        if (operationConfig.legalHold != null) {
          context.put(Context.X_OG_LEGAL_HOLD_SUFFIX, legalHoldSuffixes.get().toString());
        }
        return objectName;
      }
    };
  }

  private static Supplier<Long> createObjectSuffixes(final ObjectConfig config) {
    checkArgument(config.minSuffix >= 0, "minSuffix must be > 0 [%s]", config.minSuffix);
    checkArgument(config.maxSuffix >= config.minSuffix,
            "maxSuffix must be greater than or equal to minSuffix");

    if (SelectionType.ROUNDROBIN == config.selection) {
      return Suppliers.cycle(config.minSuffix, config.maxSuffix);
    } else {
      return Suppliers.random(config.minSuffix, config.maxSuffix);
    }
  }

  private static Supplier<Long> createLegalHoldSuffixes(final ObjectConfig config) {
    if (SelectionType.ROUNDROBIN == config.selection) {
      return Suppliers.cycle(LegalHold.MIN_SUFFIX, LegalHold.MAX_SUFFIX);
    } else {
      return Suppliers.random(LegalHold.MIN_SUFFIX, LegalHold.MAX_SUFFIX);
    }
  }

  public static Function<Map<String, String>, String> provideContainer(
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

  private static Supplier<Integer> createContainerSuffixes(final ContainerConfig config) {
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

  public static Map<String, Function<Map<String, String>, String>> provideQueryParameters(
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

  public static void checkContainerObjectConfig(final OperationConfig operationConfig) throws Exception {
    if ((operationConfig.container.maxSuffix != -1 || operationConfig.container.minSuffix != -1)
            && operationConfig.object.prefix == "" && operationConfig.weight > 0.0) {
      throw new Exception(
              "Must specify ObjectConfig prefix if using min/max suffix in container config");
    }
  }

  public static Map<String, Function<Map<String, String>, String>> provideHeaders(
          final Map<String, SelectionConfig<String>> globalHeaders,
          final Map<String, SelectionConfig<String>> operationHeaders) {
    checkNotNull(operationHeaders);
    Map<String, SelectionConfig<String>> configHeaders = globalHeaders;
    if (operationHeaders.size() > 0) {
      configHeaders = operationHeaders;
    }

    final Map<String, Function<Map<String, String>, String>> headers = Maps.newLinkedHashMap();
    for (final Map.Entry<String, SelectionConfig<String>> e : configHeaders.entrySet()) {
      headers.put(e.getKey(), createSelectionConfigSupplier(e.getValue()));
    }
    return headers;
  }

  private static Function<Map<String, String>, String> createSelectionConfigSupplier(
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

  public static Function<Map<String, String>, String> provideHost(final OperationConfig operationConfig,
                                                            final Function<Map<String, String>, String> testHost) {
    checkNotNull(operationConfig);
    checkNotNull(testHost);

    final SelectionConfig<String> operationHost = operationConfig.host;
    if (operationHost != null && !operationHost.choices.isEmpty()) {
      return createHost(operationConfig.host);
    }

    return testHost;
  }

  private static Function<Map<String, String>, String> createHost(final SelectionConfig<String> host) {
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





}
