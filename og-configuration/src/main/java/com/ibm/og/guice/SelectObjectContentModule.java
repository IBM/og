/* Copyright (c) IBM Corporation 2023. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.guice;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.ibm.og.api.Body;
import com.ibm.og.api.Method;
import com.ibm.og.api.Operation;
import com.ibm.og.api.Request;
import com.ibm.og.guice.annotation.*;
import com.ibm.og.http.*;
import com.ibm.og.json.OGConfig;
import com.ibm.og.object.ObjectManager;
import com.ibm.og.supplier.RequestSupplier;
import com.ibm.og.test.LoadTestSubscriberExceptionHandler;
import com.ibm.og.util.Context;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.gson.internal.$Gson$Preconditions.checkNotNull;

/**
 * A guice configuration module for wiring up list operation components
 *
 * @since 1.15.0
 */
public class SelectObjectContentModule extends AbstractModule {


    private final OGConfig config;
    private Gson gson;

    private LinkedHashMap<String, String> selectQueryMap;
    //@SelectSuffixMap
    //SelectOperationSharedDataModule.SuffixManager sharedData;


    private final LoadTestSubscriberExceptionHandler handler;
    private final EventBus eventBus;


    public SelectObjectContentModule(final OGConfig config) {
        checkNotNull(config);
        this.config = config;
        this.handler = new LoadTestSubscriberExceptionHandler();
        this.eventBus = new EventBus(this.handler);
        this.gson = new GsonBuilder().create();
    }

    @Override
    protected void configure() {
        bindConstant().annotatedWith(Names.named("querySelectObject.weight")).to(this.config.querySelectObject.weight);

        int i = 0;
        System.out.println("test" + i);
    }

    @Provides
    @Singleton
    @Named("querySelect.container")
    public Function<Map<String, String>, String> provideWriteContainer() {
        if (this.config.querySelectObject.container.prefix != null) {
            return ModuleUtils.provideContainer(this.config.querySelectObject.container);
        } else {
            return ModuleUtils.provideContainer(this.config.container);
        }
    }

    @Provides
    @Singleton
    @QuerySelectHeaders
    public Map<String, Function<Map<String, String>, String>> provideQuerySelectHeaders() {
        Map<String, Function<Map<String, String>, String>> headers =
                ModuleUtils.provideHeaders(this.config.headers, this.config.querySelectObject.headers);
        return headers;
    }

    @Provides
    @Singleton
    @QuerySelectBody
    public Function<Map<String, String>, Body> provideSelectQueryBody(@SelectSuffixMap SelectOperationSharedDataModule.SuffixManager manager) {
        final SelectOperationSharedDataModule.SuffixManager sharedData = manager;
        Function<Map<String, String>, Body> f = new Function<Map<String, String>, Body>() {

            @Nullable
            @Override
            public Body apply(@Nullable final Map<String, String> input) {
                String objectName = input.get(Context.X_OG_OBJECT_NAME);
                // get suffix from object name
                String suffix = objectName.substring(objectName.length()-4);
                int i = Integer.parseInt(suffix);
                String fp = sharedData.getFileNameForSuffix(i);
                String body = sharedData.getBody(fp);
                return Bodies.custom(body.length(), body);

            }

        };
        return  f;
    }



    @Provides
    @Singleton
    @Named("querySelectObject")
    public Supplier<Request> provideQuerySelectObject(
            @javax.inject.Named("request.id") final Function<Map<String, String>, String> id, final Api api,
            final Scheme scheme, @WriteHost final Function<Map<String, String>, String> host,
            @Nullable @javax.inject.Named("port") final Integer port,
            @Nullable @javax.inject.Named("uri.root") final String uriRoot,
            @javax.inject.Named("querySelect.container") final Function<Map<String, String>, String> container,
            @Nullable @javax.inject.Named("api.version") final String apiVersion,
            @QuerySelectHeaders final Map<String, Function<Map<String, String>, String>> headers,
            @javax.inject.Named("read.context") final List<Function<Map<String, String>, String>> context,
            @Nullable @ReadObjectName final Function<Map<String, String>, String> object,
            @QuerySelectBody final Function<Map<String, String>, Body> body,
            @Nullable @javax.inject.Named("credentials") final Function<Map<String, String>, Credential> credentials,
            @javax.inject.Named("virtualhost") final boolean virtualHost,
            final ObjectManager objectManager) throws Exception {

        Map<String, Function<Map<String, String>, String>> queryParameters = Maps.newLinkedHashMap();
        queryParameters.put(QueryParameters.SELECT_OPERATION_PARAMETER,
                new Function<Map<String, String>, String>() {
                    @Override
                    public String apply(final Map<String, String> context) {
                        return null;
                    }
                });
        queryParameters.put(QueryParameters.SELECT_OPERATION_TYPE_PARAMETER,
                new Function<Map<String, String>, String>() {
                    @Override
                    public String apply(final Map<String, String> context) {
                        return "2";
                    }
                });

        return createRequestSupplier(Operation.QUERY_SELECT_OBJECT, id, Method.POST, scheme, host, port, uriRoot,
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
