/*
 * Copyright (c) IBM Corporation 2020. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.guice;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.ibm.og.api.Body;
import com.ibm.og.api.Method;
import com.ibm.og.api.Operation;
import com.ibm.og.api.Request;
import com.ibm.og.guice.annotation.DeleteTagsHeaders;
import com.ibm.og.guice.annotation.DeleteTagsHost;
import com.ibm.og.guice.annotation.DeleteTagsObjectName;
import com.ibm.og.guice.annotation.GetTagsHeaders;
import com.ibm.og.guice.annotation.GetTagsQueryParameters;
import com.ibm.og.guice.annotation.WriteTagsHeaders;
import com.ibm.og.guice.annotation.WriteTagsHost;
import com.ibm.og.guice.annotation.WriteTagsQueryParameters;
import com.ibm.og.guice.annotation.WriteTagsObjectName;
import com.ibm.og.http.Api;
import com.ibm.og.http.Bodies;
import com.ibm.og.http.Credential;
import com.ibm.og.http.QueryParameters;
import com.ibm.og.http.Scheme;
import com.ibm.og.json.OGConfig;
import com.ibm.og.json.ObjectTagsConfig;
import com.ibm.og.json.OperationConfig;
import com.ibm.og.object.ObjectManager;
import com.ibm.og.supplier.DeleteTagsObjectNameFunction;
import com.ibm.og.supplier.RequestSupplier;
import com.ibm.og.supplier.WriteTagObjectNameFunction;
import com.ibm.og.test.LoadTestSubscriberExceptionHandler;
import com.ibm.og.util.Context;
import com.ibm.og.util.MoreFunctions;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.ibm.og.guice.ModuleUtils.provideContainer;
import static com.ibm.og.guice.ModuleUtils.provideObject;

/**
 *  A guice configuration module for ObjectTags operations
 *
 * @since 1.11.0
 */
public class ObjectTagsModule extends AbstractModule {

  private final OGConfig config;

  private final LoadTestSubscriberExceptionHandler handler;

  public ObjectTagsModule(final OGConfig config) {
    this.config = checkNotNull(config);
    this.handler = new LoadTestSubscriberExceptionHandler();
  }

  @Override
  protected void configure() {
    bindConstant().annotatedWith(Names.named("writeTags.weight")).to(this.config.writeTags.weight);
    bindConstant().annotatedWith(Names.named("writeTags.contentMd5")).to(this.config.writeTags.contentMd5);
    bindConstant().annotatedWith(Names.named("deleteTags.weight")).to(this.config.deleteTags.weight);
    bindConstant().annotatedWith(Names.named("getTags.weight")).to(this.config.getTags.weight);
  }


  @Provides
  @Singleton
  @WriteTagsHost
  public Function<Map<String, String>, String> provideWriteTagsHost(
          @Named("host") final Function<Map<String, String>, String> host) {
    return ModuleUtils.provideHost(this.config.writeTags, host);
  }

  @Provides
  @Singleton
  @DeleteTagsHost
  public Function<Map<String, String>, String> provideDeleteTagsHost(
          @Named("host") final Function<Map<String, String>, String> host) {
    return ModuleUtils.provideHost(this.config.deleteTags, host);
  }


  @Provides
  @Named("write_tags.context")
  public List<Function<Map<String, String>, String>> provideWriteTagsContext(
          final ObjectManager objectManager) {
    Function<Map<String, String>, String> function;

    final OperationConfig operationConfig = checkNotNull(this.config.writeTags);
    if (operationConfig.object.selection != null) {
      function = provideObject(operationConfig);
    } else {
      function = new WriteTagObjectNameFunction(objectManager);
    }

    return ImmutableList.of(function);
  }

  @Provides
  @Singleton
  @WriteTagsObjectName
  public Function<Map<String, String>, String> provideObjectWriteTags(final Api api) {
    if (Api.SOH == api) {
      return null;
    }
    return MoreFunctions.keyLookup(Context.X_OG_OBJECT_NAME);
  }

  @Provides
  @Singleton
  @DeleteTagsObjectName
  public Function<Map<String, String>, String> provideDeleteObjectTags(final Api api) {
    if (Api.SOH == api) {
      return null;
    }
    return MoreFunctions.keyLookup(Context.X_OG_OBJECT_NAME);
  }



  @Provides
  @Singleton
  @Named("delete_tags.context")
  public List<Function<Map<String, String>, String>> provideDeleteTagsContext(
          final ObjectManager objectManager) {
    Function<Map<String, String>, String> function;

    final OperationConfig operationConfig = checkNotNull(this.config.writeTags);
    if (operationConfig.object.selection != null) {
      function = provideObject(operationConfig);
    } else {
      function = new DeleteTagsObjectNameFunction(objectManager);
    }

    return ImmutableList.of(function);
  }


  @Provides
  @Singleton
  @Named("write_tags.container")
  public Function<Map<String, String>, String> provideWriteTagsContainer() {
    if (this.config.writeTags.container.prefix != null) {
      return provideContainer(this.config.writeTags.container);
    } else {
      return provideContainer(this.config.container);
    }
  }

  @Provides
  @Singleton
  @Named("delete_tags.container")
  public Function<Map<String, String>, String> provideDeleteContainer() throws Exception {
    if (this.config.deleteTags.container.prefix != null) {
      ModuleUtils.checkContainerObjectConfig(this.config.writeTags);
      return provideContainer(this.config.deleteTags.container);
    } else {
      return provideContainer(this.config.container);
    }
  }

  @Provides
  @Singleton
  @WriteTagsHeaders
  public Map<String, Function<Map<String, String>, String>> provideWriteTagsHeaders() {
    return ModuleUtils.provideHeaders(this.config.headers, this.config.writeTags.headers);
  }

  @Provides
  @Singleton
  @DeleteTagsHeaders
  public Map<String, Function<Map<String, String>, String>> provideDeleteTagsHeaders() {
    return ModuleUtils.provideHeaders(this.config.headers, this.config.deleteTags.headers);
  }

  @Provides
  @Singleton
  @GetTagsHeaders
  public Map<String, Function<Map<String, String>, String>> provideGetTagsHeaders() {
    return ModuleUtils.provideHeaders(this.config.headers, this.config.getTags.headers);
  }


  @Provides
  @Singleton
  @WriteTagsQueryParameters
  private Map<String, Function<Map<String, String>, String>> provideWriteTagsQueryParameters() {
    final Map<String, Function<Map<String, String>, String>> queryParameters;
    queryParameters = Maps.newLinkedHashMap();
    queryParameters.put(QueryParameters.OBJECT_TAGGING_PARAMETER,
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
  @GetTagsQueryParameters
  private Map<String, Function<Map<String, String>, String>> provideGetTagsQueryParameters() {
    final Map<String, Function<Map<String, String>, String>> queryParameters;
    queryParameters = Maps.newLinkedHashMap();
    queryParameters.put(QueryParameters.OBJECT_TAGGING_PARAMETER,
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
  @Named("write_tags.body")
  private Function<Map<String, String>, Body> provideWriteTagsBody() {
    final ObjectTagsConfig config = this.config.writeTags.tagsConfiguration;
    return new Function<Map<String, String>, Body>() {
      Body tagBody;

      public Body apply(Map<String, String> input) {
        String body;
        if (tagBody == null) {
          StringBuilder sb = new StringBuilder();
          sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
          sb.append("<Tagging>");
          sb.append("<TagSet>");
          for (ObjectTagsConfig.ObjectTag tag : config.tags) {
            if (tag != null) {
                sb.append("<Tag>");
                String key = tag.getKey();
                String val = tag.getValue();
                sb.append("<Key>").append(key).append("</Key>");
                sb.append("<Value>").append(val).append("</Value>");
                sb.append("</Tag>");
            }
          }
          sb.append("</TagSet>");
          sb.append("</Tagging>");
            body = new String(sb.toString().getBytes(), Charset.forName("UTF-8"));
            // body may contain utf-8 characters if the tag character contains multi-byte utf-8 character
            // so use the actual length
            tagBody = Bodies.custom(body.getBytes().length, body);
            return tagBody;
        } else {
          return tagBody;
        }
      }
    };
  }

  @Provides
  @Singleton
  @Named("writeTags")
  public Supplier<Request> provideTagsWrite(
          @Named("request.id") final Function<Map<String, String>, String> id, final Api api,
          final Scheme scheme, @WriteTagsHost final Function<Map<String, String>, String> host,
          @Nullable @Named("port") final Integer port,
          @Nullable @Named("uri.root") final String uriRoot,
          @WriteTagsQueryParameters final Map<String, Function<Map<String, String>, String>> queryParameters,
          @Named("write_tags.container") final Function<Map<String, String>, String> container,
          @Nullable @Named("api.version") final String apiVersion,
          @WriteTagsObjectName final Function<Map<String, String>, String> object,
          @Named("write_tags.body") final Function<Map<String, String>, Body> body,
          @WriteTagsHeaders final Map<String, Function<Map<String, String>, String>> headers,
          @Named("write_tags.context") final List<Function<Map<String, String>, String>> context,
          @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
          @Nullable @Named("writeTags.contentMd5") final boolean contentMd5,
          @Named("virtualhost") final boolean virtualHost,
          final ObjectManager objectManager) throws Exception {


    return new RequestSupplier(Operation.PUT_TAGS, id, Method.PUT, scheme, host, port, uriRoot,
            container, apiVersion,  object, queryParameters, false, headers, context,
            null, credentials, body, virtualHost, null, null,
            contentMd5, null, null);
  }



  @Provides
  @Singleton
  @Named("deleteTags")
  public Supplier<Request> provideDeleteTags(
          @Named("request.id") final Function<Map<String, String>, String> id, final Api api,
          final Scheme scheme, @DeleteTagsHost final Function<Map<String, String>, String> host,
          @Nullable @Named("port") final Integer port,
          @Nullable @Named("uri.root") final String uriRoot,
          @WriteTagsQueryParameters final Map<String, Function<Map<String, String>, String>> queryParameters,
          @Named("delete_tags.container") final Function<Map<String, String>, String> container,
          @Nullable @Named("api.version") final String apiVersion,
          @DeleteTagsObjectName final Function<Map<String, String>, String> object,
          @DeleteTagsHeaders final Map<String, Function<Map<String, String>, String>> headers,
          @Named("delete_tags.context") final List<Function<Map<String, String>, String>> context,
          @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
          @Named("virtualhost") final boolean virtualHost,
          final ObjectManager objectManager) throws Exception {


    return new RequestSupplier(Operation.DELETE_TAGS, id, Method.DELETE, scheme, host, port, uriRoot,
            container, apiVersion,  object, queryParameters, false, headers, context,
            null, credentials, null, virtualHost, null, null,
            false, null, null);
  }

  @Provides
  @Singleton
  @Named("getTags")
  public Supplier<Request> provideGetTags(
          @Named("request.id") final Function<Map<String, String>, String> id, final Api api,
          final Scheme scheme, @DeleteTagsHost final Function<Map<String, String>, String> host,
          @Nullable @Named("port") final Integer port,
          @Nullable @Named("uri.root") final String uriRoot,
          @GetTagsQueryParameters final Map<String, Function<Map<String, String>, String>> queryParameters,
          @Named("read.container") final Function<Map<String, String>, String> container,
          @Nullable @Named("api.version") final String apiVersion,
          @DeleteTagsObjectName final Function<Map<String, String>, String> object,
          @DeleteTagsHeaders final Map<String, Function<Map<String, String>, String>> headers,
          @Named("read.context") final List<Function<Map<String, String>, String>> context,
          @Nullable @Named("credentials") final Function<Map<String, String>, Credential> credentials,
          @Named("virtualhost") final boolean virtualHost,
          final ObjectManager objectManager) throws Exception {


    return new RequestSupplier(Operation.GET_TAGS, id, Method.GET, scheme, host, port, uriRoot,
            container, apiVersion,  object, queryParameters, false, headers, context,
            null, credentials, null, virtualHost, null, null,
            false, null, null);
  }

}
