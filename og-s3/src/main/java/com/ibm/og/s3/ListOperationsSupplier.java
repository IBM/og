/* Copyright (c) IBM Corporation 2018. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.s3;


import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import com.ibm.og.api.Method;
import com.ibm.og.api.Operation;
import com.ibm.og.api.Request;
import com.ibm.og.api.Response;
import com.ibm.og.http.Api;
import com.ibm.og.http.Credential;
import com.ibm.og.http.HttpRequest;
import com.ibm.og.http.HttpUtil;
import com.ibm.og.http.QueryParameters;
import com.ibm.og.http.Scheme;
import com.ibm.og.json.ChoiceConfig;
import com.ibm.og.json.ListSessionConfig;
import com.ibm.og.json.OperationConfig;
import com.ibm.og.json.SelectionConfig;
import com.ibm.og.object.LegacyObjectMetadata;
import com.ibm.og.object.ListObjectNameConsumer;
import com.ibm.og.object.ObjectManager;
import com.ibm.og.object.ObjectMetadata;
import com.ibm.og.supplier.RequestSupplier;
import com.google.common.base.Supplier;
import com.ibm.og.util.Context;
import com.ibm.og.util.MoreFunctions;
import com.ibm.og.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * A supplier of List requests
 *
 * @since 1.8.4
 */
public class ListOperationsSupplier implements Supplier<Request>{

  private static final Logger _logger = LoggerFactory.getLogger(ListOperationsSupplier.class);

  private static final Joiner.MapJoiner PARAM_JOINER = Joiner.on('&').withKeyValueSeparator("=").useForNull("");
  private final Function<Map<String, String>, String> id;
  private final Api api;
  private final Method method;
  private final Scheme scheme;
  private final Function<Map<String, String>, String> host;
  private final Integer port;
  private final String uriRoot;
  private final Function<Map<String, String>, String> container;
  private final String apiVersion;
  private final Function<Map<String, String>, String> object;
  private final Map<String, Function<Map<String, String>, String>> queryParameters;
  private final boolean trailingSlash;
  private final Map<String, Function<Map<String, String>, String>> headers;
  private final List<Function<Map<String, String>, String>> context;
  private Function<Map<String, String>, ListSessionConfig> listSessionConfigSupplier;
  private Function<Map<String, String>, String> prefixSupplier;
  private Function<Map<String, String>, String> delimiterSupplier;
  private final Function<Map<String, String>, Credential> credentials;
  private final boolean virtualHost;
  private final Operation operation;
  private final ObjectManager objectManager;
  private ListObjectNameConsumer consumer;

  public static int LIST_REQ_TYPE_CHAINED = 0;
  public static int LIST_REQ_TYPE_UNCHAINED = 1;

  private RequestSupplier requestSupplier;
  private ListSessionsManager sessionsManager;

  private OperationConfig config;


  public ListOperationsSupplier(final Api api, final OperationConfig config, final Operation operation,
                                final Function<Map<String, String>, String> id, final Method method,
                                final Scheme scheme, final Function<Map<String, String>, String> host, final Integer port,
                                final String uriRoot, final Function<Map<String, String>, String> container,
                                final String apiVersion, final Function<Map<String, String>, String> object,
                                final Map<String, Function<Map<String, String>, String>> queryParameters,
                                final boolean trailingSlash,
                                final Map<String, Function<Map<String, String>, String>> headers,
                                final List<Function<Map<String, String>, String>> context,
                                final Function<Map<String, String>, ListSessionConfig> listSessionConfigSupplier,
                                final Function<Map<String, String>, String> prefixSupplier,
                                final Function<Map<String, String>, String> delimiterSupplier,
                                final Function<Map<String, String>, Credential> credentials,
                                final Boolean virtualHost, final ObjectManager objectManager) {

    this.id = id;
    this.api = api;
    this.method = checkNotNull(method);
    this.scheme = checkNotNull(scheme);
    this.host = checkNotNull(host);
    this.port = port;
    this.uriRoot = uriRoot;
    this.container = container;
    this.apiVersion = apiVersion;
    this.object = object;
    this.queryParameters = queryParameters;
    this.trailingSlash = trailingSlash;
    this.headers = ImmutableMap.copyOf(headers);
    this.context = ImmutableList.copyOf(context);
    this.listSessionConfigSupplier = listSessionConfigSupplier;
    this.prefixSupplier = prefixSupplier;
    this.delimiterSupplier = delimiterSupplier;
    this.credentials = credentials;
    this.virtualHost = virtualHost;
    this.operation = operation;
    this.objectManager = objectManager;
    final Set<Integer> sc = HttpUtil.SUCCESS_STATUS_CODES;
    this.consumer = new ListObjectNameConsumer(objectManager, sc);
    this.config = config;
    this.sessionsManager = new ListSessionsManager(this.config, objectManager);

  }

  @Subscribe
  public void update(final Pair<Request, Response> result) {
    Request request = result.getKey();
    Response response = result.getValue();
    if (request.getOperation() != this.operation) {
      return;
    }
    Map<String, String> requestContext = request.getContext();
    Map<String, String> responseContext = response.getContext();

    // release the marker object back to object manager.
    String object = requestContext.get(Context.X_OG_OBJECT_NAME);
    if (object != null) {
      updateObject(result);
    }

    String sid = requestContext.get(Context.X_OG_LIST_SESSION_ID);
    String type = requestContext.get(Context.X_OG_LIST_SESSION_TYPE);
    int sessionId = Integer.parseInt(sid);
    ListSession s;
    int sessionType;
    if (type.equals("CHAINED")) {
      sessionType = LIST_REQ_TYPE_CHAINED;
     s = sessionsManager.getSession(sessionId, LIST_REQ_TYPE_CHAINED);
    } else {
      sessionType = LIST_REQ_TYPE_UNCHAINED;
      s = sessionsManager.getSession(sessionId, LIST_REQ_TYPE_UNCHAINED);
    }

    boolean truncated = false;
    if (response.getStatusCode() == 200) {

      String isTruncated = responseContext.get(Context.X_OG_LIST_IS_TRUNCATED);
      if (isTruncated != null && isTruncated.equals("true") && sessionType == LIST_REQ_TYPE_CHAINED) {
        truncated = true;
        //TODO: per aws v1 doc - next marker is only sent if the request has prefix query
        String nextMarker = responseContext.get(Context.X_OG_LIST_NEXT_MARKER);
        if (nextMarker != null) {
          s.setMarker(nextMarker);
        }

        String nextContToken = responseContext.get(Context.X_OG_LIST_NEXT_CONTINUATION_TOKEN);
        if (nextContToken != null) {
          s.setContinuationToken(nextContToken);
        }

        String nextVersionIdMarker = responseContext.get(Context.X_OG_LIST_OBJECT_VERSIONS_NEXT_VERSION_ID_MARKER);
        if (nextVersionIdMarker != null) {
          s.setNextVersionIdMarker(nextVersionIdMarker);
        }

        String nextVersionMarker = responseContext.get(Context.X_OG_LIST_OBJECT_VERSIONS_NEXT_KEY_MARKER);
        if (nextVersionMarker != null) {
          s.setNextKeyMarker(nextVersionMarker);
        }

      } else {
        // free up session
        _logger.debug("close non-truncated / unchained list session [{}]", sessionId);
        truncated = false;
      }
    }

    if (s.getNumRequestSent() == 1) {
      s.removeObjectNameFromRequestContext();
    }

    if (s.getNumRequestSent() >= s.getMaxRequests() || !truncated || sessionType == LIST_REQ_TYPE_UNCHAINED) {
      // close session
      _logger.debug("Max chained requests done. close list session [{}]", sessionId);
      sessionsManager.removeSession(s);
      sessionsManager.totalActiveSessions.decrementAndGet();
    } else {
      sessionsManager.addFreeSession(s);
    }
  }

  public void updateObject(final Pair<Request, Response> result) {
    this.consumer.consume(result);
  }


  private void appendQueryParams(final StringBuilder s, final Map<String, String> context) {
    final Map<String, String> queryParamsMap = Maps.newHashMap();

    for (final Map.Entry<String, Function<Map<String, String>, String>> queryParam : this.queryParameters
            .entrySet()) {
      String key = queryParam.getKey();
      String value = queryParam.getValue().apply(context);
      queryParamsMap.put(key, value);
      if (key.equals("max-keys")) {
        context.put(Context.X_OG_LIST_MAX_KEYS, value);
      }
    }
    // if the context has next continuation token add it to the parameter
    String contToken = context.get(Context.X_OG_LIST_NEXT_CONTINUATION_TOKEN);
    if (contToken != null) {
      queryParamsMap.put("continuation-token", contToken);
    }

    String marker = context.get(Context.X_OG_LIST_NEXT_MARKER);
    if (marker != null) {
      queryParamsMap.put("marker", marker);
    }

    String startAfter = context.get(Context.X_OG_LIST_START_AFTER);
    if (startAfter != null) {
      queryParamsMap.put("start-after", startAfter);
    }

    String prefix = context.get(Context.X_OG_LIST_PREFIX);
    if (prefix != null) {
      queryParamsMap.put("prefix", prefix);
    }

    String delimiter = context.get(Context.X_OG_LIST_DELIMITER);
    if (delimiter != null) {
      queryParamsMap.put("delimiter", delimiter);
    }

    String keyMarker = context.get(Context.X_OG_LIST_OBJECT_VERSIONS_KEY_MARKER);
    if (keyMarker != null) {
      queryParamsMap.put("key-marker", keyMarker);
      //queryParamsMap.put("prefix", keyMarker);
    }

    String versionId = context.get(Context.X_OG_LIST_OBJECT_VERSIONS_VERSION_ID);
    if (versionId != null) {
      queryParamsMap.put("version-id-marker", versionId);
    }

    //String qpString = PARAM_JOINER.join(queryParamsMap);
    StringBuilder sb = new StringBuilder();
    sb.append("?");
    for (final Map.Entry<String, String> e: queryParamsMap.entrySet()) {
      String key = e.getKey();
      String val = e.getValue();
      if (key != null && !key.isEmpty()) {
        sb.append(key);
        if (val != null && !val.isEmpty()) {
         sb.append("=").append(val);
        }
        sb.append("&");
      }
    }
    //delete the & in the end
    sb.deleteCharAt(sb.length()-1);
//    if (qpString.length() > 0) {
//      s.append("?").append(qpString);
//    }
    s.append(sb.toString());
  }

  @Override
  public Request get() {
    Map<String, String> requestContext = Maps.newHashMap();
    ListSessionConfig listSessionConfig = this.listSessionConfigSupplier.apply(requestContext);

    ListSession s = this.sessionsManager.getNextSession(listSessionConfig);
    requestContext = s.getRequestContext();
    URI uri = getUrl(requestContext, s);

    // Build HTTPRequest
    final HttpRequest.Builder builder =
            new HttpRequest.Builder(this.method, uri, this.operation);

    for (final Map.Entry<String, Function<Map<String, String>, String>> header : this.headers
            .entrySet()) {
      builder.withHeader(header.getKey(), header.getValue().apply(requestContext));
    }

    for (final Map.Entry<String, String> entry : requestContext.entrySet()) {
      builder.withContext(entry.getKey(), entry.getValue());
    }

    String queryParameters = uri.getQuery();
    if (queryParameters != null) {
      String[] params = queryParameters.split("&");
      for (String str: params) {
        String[] keyValue = str.split("=");
        String key = str.split("=")[0];
        if (keyValue.length > 1) {
          String value = str.split("=")[1];
          builder.withQueryParameter(keyValue[0], keyValue[1]);
        } else {
          builder.withQueryParameter(keyValue[0], "");
        }
      }
    }
   builder.withContext(Context.X_OG_RESPONSE_BODY_CONSUMER, "s3.list");
   return builder.build();
  }

  private URI getUrl(final Map<String, String> context, ListSession listSession) {

    final StringBuilder s = new StringBuilder().append(this.scheme).append("://");
    appendHost(s, context);
    appendPort(s);
    appendPath(s, context, apiVersion);
    appendTrailingSlash(s);
    StringBuilder sb = new StringBuilder();
    appendQueryParams(sb, listSession.getRequestContext());
    listSession.setParamString(sb.toString());

    s.append(listSession.getParamString());
    //s.append(listSession.getQueryParamString());

    try {
      return new URI(s.toString());
    } catch (final URISyntaxException e) {
      // Wrapping checked exception as unchecked because most callers will not be able to handle
      // it and I don't want to include URISyntaxException in the entire signature chain
      throw new IllegalArgumentException(e);
    }
  }

  private void appendHost(final StringBuilder s, final Map<String, String> context) {
    if (this.virtualHost) {
      String containerName = context.get(Context.X_OG_CONTAINER_NAME);
      if (containerName != null) {
        s.append(containerName).append(".");
      }
    }

    s.append(this.host.apply(context));
  }

  private void appendPort(final StringBuilder s) {
    if (this.port != null) {
      s.append(":").append(this.port);
    }
  }

  private void appendPath(final StringBuilder s, final Map<String, String> context, final String apiVersion) {
    if (!this.virtualHost) {
      s.append("/");
      if (this.uriRoot != null) {
        s.append(this.uriRoot).append("/");
      }

      if (apiVersion != null) {
        s.append(apiVersion).append("/");
      }
      String storageAccount = getStorageAccountPath(context, apiVersion);
      if (storageAccount != null) {
        s.append(getStorageAccountPath(context, apiVersion));
      }

      String containerName = context.get(Context.X_OG_CONTAINER_NAME);
      if (containerName != null) {
        s.append(containerName);
      }
    }

    if (this.object != null) {
      s.append("/").append(this.object.apply(context));
    }
  }

  private void appendTrailingSlash(final StringBuilder s) {
    if (this.trailingSlash) {
      s.append("/");
    }
  }

  private String getStorageAccountPath(final Map<String, String> context, final String apiVersion) {
    String storageAccountName = context.get(Context.X_OG_STORAGE_ACCOUNT_NAME);
    StringBuilder s = new StringBuilder();
    if(storageAccountName != null) {
      s.append(storageAccountName).append("/");
    } else if (apiVersion != null && storageAccountName == null) {
      // FIXME - this is a case to accomodate vault mode swift account. If the api version is present,
      // the dsnet expects a storage account name. so pass a dummy account name when there is no authentication
      s.append("dummyaccount").append("/");
    } else {
      return null;
    }
    return s.toString();
  }

  public static class ListSession {

    int type;
    int version = 1;
    Integer id;
    private String container;
    private String marker;
    private String continuationToken;
    private String startAfter;
    private String nextVersionIdMarker;
    private String nextKeyMarker;
    private int maxRequests;
    private int numRequestSent;
    private Map<String, String> parametersMap = new LinkedHashMap<String, String>();
    private String paramString;
    private Map<String, String> requestContext = new LinkedHashMap<String, String>();
    private boolean startFromBeginning = false;
    // list object versions
    private String keyMarker;
    private String versionId;



    private Integer configId;


    public int getType() {
      return type;
    }

    public void setType(int type) {
      this.type = type;
    }

    public Integer getId() {
      return id;
    }

    public void setId(Integer id) {
      this.id = id;
    }



    public ListSession(final ListSessionConfig config, int configId) {
      int listSessionType;
      if (config.requestType.equals("CHAINED")) {
        listSessionType = 0;
      } else {
        listSessionType = 1;
      }
      this.type = listSessionType;
      if (listSessionType == LIST_REQ_TYPE_CHAINED) {
        this.maxRequests = config.maxChainedRequests;
      } else {
        this.maxRequests = 1;
      }
      this.startFromBeginning = config.startFromBeginning;
      this.configId = configId;

    }

    public String getMarker() {
      return marker;
    }

    public void setMarker(String marker) {
      checkNotNull(marker);
      this.marker = marker;
      requestContext.put(Context.X_OG_LIST_NEXT_MARKER, marker);
      requestContext.put(Context.X_OG_OBJECT_NAME, marker);
    }

    public void setKeyMarker(String keyMarker) {
      checkNotNull(keyMarker);
      this.keyMarker = keyMarker;
      requestContext.put(Context.X_OG_LIST_OBJECT_VERSIONS_KEY_MARKER, keyMarker);
      requestContext.put(Context.X_OG_OBJECT_NAME, keyMarker);
    }

    public void setVersionId(String versionId) {
      checkNotNull(versionId);
      this.versionId = versionId;
      requestContext.put(Context.X_OG_LIST_OBJECT_VERSIONS_VERSION_ID, versionId);
    }


    public void setContinuationToken(String continuationToken) {
      checkNotNull(continuationToken);
      this.continuationToken = continuationToken;
      requestContext.put(Context.X_OG_LIST_NEXT_CONTINUATION_TOKEN, continuationToken);
    }

    public void setNextVersionIdMarker(String nextVersionIdMarker) {
      checkNotNull(nextVersionIdMarker);
      this.nextVersionIdMarker = nextVersionIdMarker;
      requestContext.put(Context.X_OG_LIST_OBJECT_VERSIONS_VERSION_ID, nextVersionIdMarker);
    }

    public void setNextKeyMarker(String nextKeyMarker) {
      checkNotNull(nextKeyMarker);
      this.nextKeyMarker = nextKeyMarker;
      requestContext.put(Context.X_OG_LIST_OBJECT_VERSIONS_KEY_MARKER, this.nextKeyMarker);
      requestContext.put(Context.X_OG_OBJECT_NAME, nextKeyMarker);
    }


    public void setStartAfter(String startAfter) {
      checkNotNull(startAfter);
      this.startAfter = startAfter;
      requestContext.put(Context.X_OG_LIST_START_AFTER, startAfter);
    }

    public int getMaxRequests() {
      return maxRequests;
    }

    public int getNumRequestSent() {
      return numRequestSent;
    }

    public void incrementRequestSent() {
      this.numRequestSent++;
    }

    public int getVersion() {
      return version;
    }

    public void setVersion(int version) {
      this.version = version;
    }

    public String getContainer() {
      return container;
    }

    public void setContainer(String container) {
      this.container = container;
    }

    public String getParamString() {

      return paramString;
    }

    public void setParamString(String paramString) {
      this.paramString = paramString;
    }

    public Map<String, String> getRequestContext() {
      return requestContext;
    }

    public boolean isStartFromBeginning() {
      return startFromBeginning;
    }

    public Integer getConfigId() {
      return configId;
    }

    public void removeObjectNameFromRequestContext() {
      requestContext.remove(Context.X_OG_OBJECT_NAME);
    }

  }
  private class ListSessionsManager {

    private ConcurrentHashMap<Integer, ListSession> chainedSessions;
    private ConcurrentHashMap<Integer, ListSession> unchainedSessions;

    private Map<Integer, List<ListSession>> chainedSessionsFree;
    private Map<Integer, List<ListSession>> unchainedSessionsFree;
    private AtomicInteger sid;
    private AtomicInteger totalActiveSessions;
    private final OperationConfig config;
    private final ObjectManager objectManager;


    ListSessionsManager(final OperationConfig config, ObjectManager objectManager) {
      sid = new AtomicInteger(0);
      this.chainedSessions = new ConcurrentHashMap<Integer, ListSession>();
      this.unchainedSessions = new ConcurrentHashMap<Integer, ListSession>();
      this.chainedSessionsFree = new LinkedHashMap(); //Collections.synchronizedList(new ArrayList<ListSession>()));
      this.unchainedSessionsFree = new LinkedHashMap(); //Collections.synchronizedList(new ArrayList<ListSession>());
      this.config = config;
      this.objectManager = objectManager;
      totalActiveSessions = new AtomicInteger();
    }


    public ListSession getSession(final int sessionId, int type) {
      if (LIST_REQ_TYPE_CHAINED == type) {
        return this.chainedSessions.get(sessionId);
      } else {
        return this.unchainedSessions.get(sessionId);
      }
    }

    private ListSession getFreeSession(final int type, final Integer hashCode) {
      ListSession s = null;
      if (LIST_REQ_TYPE_CHAINED == type) {
        if (this.chainedSessionsFree.size() > 0) {
          List<ListSession> freeList = this.chainedSessionsFree.get(hashCode);
          if ( freeList != null && freeList.size() > 0) {
            s = freeList.remove(0);
          }
        }
      } else {
        if (this.unchainedSessionsFree.size() > 0) {
          List<ListSession> freeList = this.unchainedSessionsFree.get(hashCode);
          if ( freeList != null && freeList.size() > 0) {
            s = freeList.remove(0);
          }
        }
      }
      return s;
    }

    public ListSession getNextSession(final ListSessionConfig listSessionConfig) {
      int listSessionType;
      if (listSessionConfig.requestType.equals("CHAINED")) {
        listSessionType = 0;
      } else {
        listSessionType = 1;
      }
      // listSessionConfig represents the selected list session configuration
      ListSession s;
      if (totalActiveSessions.get() < this.config.minimumListSessions)
      {
        s = newSession(listSessionConfig);
      } else {
        s = getFreeSession(listSessionType, listSessionConfig.hashCode());
        if (s == null) {
          s = newSession(listSessionConfig);
        } else {
          s.getRequestContext().remove(Context.X_OG_OBJECT_NAME);
          s.incrementRequestSent();
        }
      }
      final Map<String, String> requestContext = s.getRequestContext();
      requestContext.put(Context.X_OG_LIST_SESSION_ID, String.valueOf(s.getId()));

      if (s.getType() == LIST_REQ_TYPE_CHAINED) {
        this.chainedSessions.put(s.getId(), s);
      } else {
        this.unchainedSessions.put(s.getId(), s);
      }
      requestContext.put(Context.X_OG_LIST_REQ_NUM, String.valueOf(s.getNumRequestSent()));
      requestContext.put(Context.X_OG_LIST_MAX_REQS, String.valueOf(s.getMaxRequests()));

      return s;
    }

    public ListSession newSession(final ListSessionConfig listSessionConfig) {

      ListSession s = new ListSession(listSessionConfig, listSessionConfig.hashCode());
      s.setId(sid.getAndIncrement());
      this.totalActiveSessions.incrementAndGet();
      _logger.info("total active sessions: {}", this.totalActiveSessions);
      final Map<String, String> requestContext = s.getRequestContext();
      int listSessionType;
      if (listSessionConfig.requestType.equals("CHAINED")) {
        listSessionType = 0;
      } else {
        listSessionType = 1;
      }
      if (LIST_REQ_TYPE_CHAINED == listSessionType) {
        requestContext.put(Context.X_OG_LIST_SESSION_TYPE, "CHAINED");
      } else {
        requestContext.put(Context.X_OG_LIST_SESSION_TYPE, "UNCHAINED");
      }
      s.incrementRequestSent();

      // select initial object to start with for selecting container, prefix etc
      for (final Function<Map<String, String>, String> function : context) {
        // return value for context functions is ignored
        function.apply(requestContext);
      }

      // prefix
      if (prefixSupplier != null) {
        String prefix = prefixSupplier.apply(requestContext);
      }
      //delimiter
      if (delimiterSupplier != null) {
        delimiterSupplier.apply(requestContext);
      }
      if (api == Api.S3) {
        if ((this.config.parameters != null && this.config.parameters.containsKey("list-type"))) {
          String version = this.config.parameters.get("list-type");
          if (version.equals("2")) {
            if (!s.isStartFromBeginning()) {
              String startAfter = requestContext.get(Context.X_OG_OBJECT_NAME);
              s.setStartAfter(startAfter);
            }
          } else if (version.equals("1")) {
            if (!s.isStartFromBeginning()) {
              String marker = requestContext.get(Context.X_OG_OBJECT_NAME);
              s.setMarker(marker);
            }
          } else if (operation == Operation.LIST_OBJECT_VERSIONS) {
            if (!s.isStartFromBeginning()) {
              String marker = requestContext.get(Context.X_OG_OBJECT_NAME);
              s.setMarker(marker);
            }
          } else {
            throw new IllegalArgumentException(
                    String.format("unacceptable listing api version [%s]", version));
          }
        } else if ((this.config.weightedParameters != null && this.config.weightedParameters.containsKey("list-type"))) {
          SelectionConfig<String> versions = config.weightedParameters.get("list-type");
          ChoiceConfig<String> choice = versions.choices.get(0);
          String version = choice.choice;
          if (version.equals("2")) {
            if (!s.isStartFromBeginning()) {
              String startAfter = requestContext.get(Context.X_OG_OBJECT_NAME);
              s.setStartAfter(startAfter);
            }
          } else if (version.equals("1")) {
            if (!s.isStartFromBeginning()) {
              String marker = requestContext.get(Context.X_OG_OBJECT_NAME);
              s.setKeyMarker(marker);
            }
          } else {
            throw new IllegalArgumentException(
                    String.format("unacceptable listing api version [%s]", version));
          }
        } else if (operation == Operation.LIST_OBJECT_VERSIONS) {
            queryParameters.put("versions",
                    new Function<Map<String, String>, String>() {
                      @Override
                      public String apply(final Map<String, String> context) {
                        return null;
                      }
                    });
            if (!s.isStartFromBeginning()) {
              String keyMarker = requestContext.get(Context.X_OG_OBJECT_NAME);
              String versionId = requestContext.get(Context.X_OG_OBJECT_VERSION);
              s.setKeyMarker(keyMarker);
              if (versionId != null) {
                s.setVersionId(versionId);
              }
            }
        } else {
          //assume v1
          if (!s.isStartFromBeginning()) {
            String marker = requestContext.get(Context.X_OG_OBJECT_NAME);
            s.setMarker(marker);
          }
        }
      } else if (api == Api.OPENSTACK) {
        //TODO: FIX ME
        queryParameters.put(QueryParameters.OPENSTACK_MARKER,
                MoreFunctions.keyLookup(Context.X_OG_OBJECT_NAME));
      }

      if (container != null) {
        // container-name is populated in the context now if it is available
        // populate container name in context because Credential needs that to lookup
        // storage account
        // todo: Fix me. need to refactor this and handle ordering in the context
        container.apply(requestContext);
        s.setContainer(requestContext.get(Context.X_OG_CONTAINER_NAME));
      }

      if (credentials != null) {
        Credential credential = credentials.apply(requestContext);
        String username = credential.getUsername();
        String password = credential.getPassword();
        String keystoneToken = credential.getKeystoneToken();
        String IAMToken = credential.getIAMToken();
        String storageAccountName = credential.getStorageAccountName();

        if(username != null)
          requestContext.put(Context.X_OG_USERNAME, username);
        if(password != null)
          requestContext.put(Context.X_OG_PASSWORD, password);
        if(keystoneToken != null)
          requestContext.put(Context.X_OG_KEYSTONE_TOKEN, keystoneToken);
        if(IAMToken != null)
          requestContext.put(Context.X_OG_IAM_TOKEN, IAMToken);
        if(storageAccountName != null) {
          requestContext.put(Context.X_OG_STORAGE_ACCOUNT_NAME, storageAccountName);
        }
      }


      return s;
    }


    public void removeSession(ListSession session) {
      if (session.type == LIST_REQ_TYPE_CHAINED) {
        this.chainedSessions.remove(session.getId());
      } else if (session.type == LIST_REQ_TYPE_UNCHAINED) {
        this.unchainedSessions.remove(session.getId());
      }
    }


    public void addFreeChainSession(ListSession session) {
      List<ListSession> freeList = this.chainedSessionsFree.get(session.getConfigId());
      if (freeList == null) {
        freeList = Collections.synchronizedList(new ArrayList<ListSession>());
      }
      freeList.add(session);
      this.chainedSessionsFree.put(session.getConfigId(), freeList);


    }

    public void addFreeUnChainedSession(ListSession session) {
      List<ListSession> freeList = this.unchainedSessionsFree.get(session.getConfigId());
      if (freeList == null) {
        freeList = Collections.synchronizedList(new ArrayList<ListSession>());
      }
      freeList.add(session);
      this.unchainedSessionsFree.put(session.getConfigId(), freeList);
    }

    public void addFreeSession(ListSession session) {
      if (session.getType() == LIST_REQ_TYPE_CHAINED) {
        addFreeChainSession(session);
      } else {
        addFreeUnChainedSession(session);
      }
    }

  }

}
