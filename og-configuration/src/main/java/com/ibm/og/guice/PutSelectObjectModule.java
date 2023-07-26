/* Copyright (c) IBM Corporation 2023. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.guice;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.ibm.og.api.*;
import com.ibm.og.guice.annotation.*;
import com.ibm.og.http.Api;
import com.ibm.og.http.Bodies;
import com.ibm.og.http.Credential;
import com.ibm.og.http.Scheme;
import com.ibm.og.json.*;
import com.ibm.og.object.ObjectManager;
import com.ibm.og.supplier.RandomSupplier;
import com.ibm.og.supplier.RequestSupplier;
import com.ibm.og.supplier.Suppliers;
import com.ibm.og.supplier.UUIDObjectNameFunction;
import com.ibm.og.test.LoadTestSubscriberExceptionHandler;
import com.ibm.og.util.Context;
import com.ibm.og.util.MoreFunctions;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A guice configuration module for wiring up list operation components
 *
 * @since 1.15.0
 */


public class PutSelectObjectModule extends AbstractModule {

    private final OGConfig config;
    private Gson gson;

    //private LinkedHashMap<String, Double> selectObjectSuffixMap = new LinkedHashMap<>();

    private final LoadTestSubscriberExceptionHandler handler;
    private final EventBus eventBus;

//    @Provides
//    @SelectSuffixMap
//    public LinkedHashMap<String, Double> provideSuffixMap() {
//        return this.selectObjectSuffixMap;
//    }


    @Provides
    @Singleton
    @Named("selectOperationObjectSuffixMapper")
    public Function<Map<String, String>, String> createSelectObjectSuffixMapper(@SelectSuffixMap SelectOperationSharedDataModule.SuffixManager manager) {
        final SelectOperationSharedDataModule.SuffixManager suffixManager = manager;

        Function<Map<String, String>, String> selectOperationObjectSuffixMapper = new Function<Map<String, String>, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Map<String, String> input) {
                int suffix;
                final ConcurrentHashMap<String, Integer> selectObjectSuffixMap = suffixManager.selectObjectSuffixMap;
                final String filepath = input.get(Context.X_OG_SELECT_OBJECT_FILENAME);
                if (selectObjectSuffixMap.containsKey(filepath)) {
                    suffix = selectObjectSuffixMap.get(filepath);
                } else {
                    suffix = selectObjectSuffixMap.size() + 1;
                    selectObjectSuffixMap.put(filepath, suffix);

                }
                return String.valueOf(suffix);

            }
        };
        return selectOperationObjectSuffixMapper;
    }






    public PutSelectObjectModule(final OGConfig config) {
        checkNotNull(config);
        this.config = config;
        this.handler = new LoadTestSubscriberExceptionHandler();
        this.eventBus = new EventBus(this.handler);
        this.gson = new GsonBuilder().create();
    }

    @Override
    protected void configure() {
        // nothing to bind here
        bindConstant().annotatedWith(Names.named("writeSelectObject.weight")).to(this.config.writeSelectObject.weight);
//        bind(LinkedHashMap.class)
//                .annotatedWith(Names.named("xyz")).toProvider()
    }

    private Function<Map<String, String>, WriteSelectBodyConfig> createSelectionConfigSupplier() {
        final SelectionConfig<WriteSelectBodyConfig> selectionConfig = this.config.writeSelectObject.writeSelectBodyConfig;
        if (SelectionType.ROUNDROBIN == selectionConfig.selection) {
            final List<WriteSelectBodyConfig> choiceList = Lists.newArrayList();
            for (final ChoiceConfig<WriteSelectBodyConfig> choice : selectionConfig.choices) {
                choiceList.add(choice.choice);
            }
            final Supplier<WriteSelectBodyConfig> configSupplier = Suppliers.cycle(choiceList);
            return MoreFunctions.forSupplier(configSupplier);
        }

        final RandomSupplier.Builder<WriteSelectBodyConfig> wrc = Suppliers.random();
        for (final ChoiceConfig<WriteSelectBodyConfig> choice : selectionConfig.choices) {
            wrc.withChoice(choice.choice, choice.weight);
        }
        final Supplier<WriteSelectBodyConfig> configSupplier = wrc.build();
        return MoreFunctions.forSupplier(configSupplier);
    }

    @Provides
    @Singleton
    @Named("writeSelectObjectFilename")
    private Function<Map<String, String>, String> filenameProvider() {
        if (!this.config.writeSelectObject.writeSelectBodyConfig.choices.isEmpty()) {
            final Function<Map<String, String>, WriteSelectBodyConfig> fileSupplier = createSelectionConfigSupplier();
            Function<Map<String, String>, String> f = new Function<Map<String, String>, String>() {
                @Nullable
                @Override
                public String apply(@Nullable Map<String, String> input) {
                    final WriteSelectBodyConfig bodyConfig = fileSupplier.apply(input);
                    final String filename = bodyConfig.filepath;
                    input.put(Context.X_OG_SELECT_OBJECT_FILENAME, filename);
                    return filename;
                }
            };
            return f;
        } else {
            return null;
        }
    }

    private Function<Map<String, String>, Body> createFileBodySupplier() {

        Function<Map<String, String>, Body> f = new Function<Map<String, String>, Body>() {
            @Nullable
            @Override
            public Body apply(@Nullable Map<String, String> input) {
                final String filename = input.get(Context.X_OG_SELECT_OBJECT_FILENAME);
                return Bodies.file(filename);
            }
        };
        return f;
    }

    @Provides
    @Singleton
    @WriteSelectBody
    public Function<Map<String, String>, Body> provideWriteBody() {
        return createFileBodySupplier();
    }

//    @Provides
//    @Singleton
//    @WriteHost
//    public Function<Map<String, String>, String> provideWriteHost(
//            @Named("host") final Function<Map<String, String>, String> host) {
//        return ModuleUtils.provideHost(this.config.writeSelectObject, host);
//    }

    @Provides
    @Singleton
    @Named("writeSelect.container")
    public Function<Map<String, String>, String> provideWriteContainer() {
        if (this.config.writeSelectObject.container.prefix != null) {
            return ModuleUtils.provideContainer(this.config.writeSelectObject.container);
        } else {
            return ModuleUtils.provideContainer(this.config.container);
        }
    }

    @Provides
    @Singleton
    @WriteSelectHeaders
    public Map<String, Function<Map<String, String>, String>> provideWriteHeaders() {
        Map<String, Function<Map<String, String>, String>> headers =
            ModuleUtils.provideHeaders(this.config.headers, this.config.writeSelectObject.headers);
        return headers;
    }

    @Provides
    @Singleton
    @Named("writeSelect.context")
    public List<Function<Map<String, String>, String>> provideWriteContext(
            final Api api,
            @Nullable @Named("writeSelectObjectFilename") final Function<Map<String, String>, String> filenameProvider,
            @Named("writeSelectObjectSuffixMap") final Function<Map<String, String>, String> suffixProvider) {

        final List<Function<Map<String, String>, String>> context = Lists.newArrayList();

        final OperationConfig operationConfig = checkNotNull(this.config.writeSelectObject);
        if (operationConfig.object.selection != null) {
            context.add(ModuleUtils.provideObject(operationConfig));
        } else {
            // default for writes
            //TODO: track the type of object / filename / query
            // call filenameprovider.apply
            // call suffixprovider.apply
            // create uuid object name with suffix
            if (filenameProvider != null) {
                context.add(filenameProvider);
            }
            context.add(suffixProvider);
            context.add(new UUIDObjectNameFunction(config.octalNamingMode, -1));
        }
        return ImmutableList.copyOf(context);
    }

    @Provides
    @Singleton
    @Named("writeSelectObjectSuffixMap")
    public final Function<Map<String, String>, String> provideSelectObjectSuffixMap(final String filepath,
                                    @Named("selectOperationObjectSuffixMapper") final Function<Map<String, String>, String> mapper) {
        // load the mappings from selectobjectsuffix.json if present

        Function<Map<String, String>, String> f = new Function<Map<String, String>, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Map<String, String> input) {
                final String suffix = mapper.apply(input);
                input.put(Context.X_OG_SELECT_OBJECT_SUFFIX, String.valueOf(suffix));
                return String.valueOf(suffix);
            }
        };

        return f;
    }


    @Provides
    @Singleton
    @Named("writeSelectObject")
    public Supplier<Request> provideWriteSelectObject(
            @Named("request.id") final Function<Map<String, String>, String> id, final Api api,
            final Scheme scheme, @WriteHost final Function<Map<String, String>, String> host,
            @Nullable @Named("port") final Integer port,
            @Nullable @Named("uri.root") final String uriRoot,
            @Named("writeSelect.container") final Function<Map<String, String>, String> container,
            @Nullable @Named("api.version") final String apiVersion,
            @WriteSelectHeaders final Map<String, Function<Map<String, String>, String>> headers,
            @Named("writeSelect.context") final List<Function<Map<String, String>, String>> context,
            @Nullable @WriteObjectName final Function<Map<String, String>, String> object,
            @WriteSelectBody final Function<Map<String, String>, Body> body,
            @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
            @Named("virtualhost") final boolean virtualHost,
            final ObjectManager objectManager) throws Exception {

        final Map<String, Function<Map<String, String>, String>> queryParameters = Collections.emptyMap();

        return createRequestSupplier(Operation.WRITE_SELECT_OBJECT, id, Method.PUT, scheme, host, port, uriRoot,
                container, apiVersion, object, queryParameters, headers, context, null, body,
                credentials, virtualHost, null, null, false, null, null);
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
                                                    final boolean contentMd5, final Function<Map<String, String>, String> delimiter,
                                                    final Function<Map<String, String>, String> staticWebsiteVirtualHostSuffix) {

        return new RequestSupplier(operation, id, method, scheme, host, port, uriRoot, container,
                apiVersion, object, queryParameters, false, headers, context, sseSourceContext, credentials,
                body, virtualHost, retention, legalHold, contentMd5, delimiter, staticWebsiteVirtualHostSuffix);
    }



}
