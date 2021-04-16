/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.client;

import java.net.URI;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.ibm.og.api.*;
import com.ibm.og.util.Context;
import com.ibm.og.api.RequestTimestamps;
import org.apache.http.HttpHeaders;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * A class for assisting in the serialization of a request / response pair
 * 
 * @since 1.0
 */
public class RequestLogEntry {
  private static final String HTTP_TYPE = "http";
  public final String operation;
  public final String type;
  public final String serverName;
  public final String remoteAddress;
  public final String user;
  public final long timestampStart;
  public final long timestampFinish;
  public final String timeStart;
  public final String timeFinish;
  public final Method requestMethod;
  public final String requestUri;
  public final String sourceUri;
  public final String objectId;
  public final String sourceObjectId;
  public final int status;
  public final Long requestLength;
  public final Long responseLength;
  public final String userAgent;
  public final long requestLatency;
  public final String clientRequestId;
  public final String requestId;
  public final RequestStats stat;
  public final Long originalObjectLength;
  public final Long objectLength;
  public final String objectName;
  public final String retention;
  public final String legalHold;
  public String deletedObjectLength;
  public String maxKeys;
  public String listSessionId;
  public String listRequestNum;
  public String listMaxRequests;
  public String listPrefix;
  public String listDelimiter;
  public String listContentSize;
  public String listCommonPrefixesSize;
  public String multideleteReqObjects;
  public String multideleteDeletedObjects;
  public String multideleteFailedObjects;
  public String objectVersionId;
  public String newObjectVersionId;

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormat.forPattern("dd/MMM/yyyy:HH:mm:ss Z").withLocale(Locale.US);
  private static final String X_CLV_REQUEST_ID = "X-Clv-Request-Id";

  /**
   * Constructs an instance
   * 
   * @param request the request for this operation
   * @param response the response for this operation
   * @param userAgent the http user agent for this operation
   * @param timestamps a collection of timestamps gathered during execution
   */
  public RequestLogEntry(final Request request, final Response response, final String userAgent,
                         final RequestTimestamps timestamps) {
    this.operation = request.getOperation().toString();
    this.type = request.getUri().getScheme();
    final URI uri = request.getUri();
    // FIXME reliably get localaddress? Name should be clientName? Do we even need this field?
    this.serverName = null;
    this.remoteAddress = uri.getHost();
    this.user = request.getContext().get(Context.X_OG_USERNAME);
    this.timestampStart = timestamps.startMillis;
    this.timestampFinish = timestamps.finishMillis;
    this.timeStart = RequestLogEntry.FORMATTER.print(this.timestampStart);
    this.timeFinish = RequestLogEntry.FORMATTER.print(this.timestampFinish);
    this.requestMethod = request.getMethod();

    this.requestUri = uri.toString();

    String operationObjectName = request.getContext().get(Context.X_OG_OBJECT_NAME);
    // SOH writes
    if (operationObjectName == null) {
      operationObjectName = response.getContext().get(Context.X_OG_OBJECT_NAME);
    }
    this.objectId = operationObjectName;

    Long objectSize = null;
    if (DataType.NONE != request.getBody().getDataType()) {
      objectSize = request.getBody().getSize();
    }
    if (request.getOperation() == Operation.DELETE) {
      if (request.getContext().get(Context.X_OG_OBJECT_SIZE) != null) {
        this.deletedObjectLength = request.getContext().get(Context.X_OG_OBJECT_SIZE);
      }
    }

    this.status = response.getStatusCode();
    // TODO requestLength will not equal objectLength with AWSv4 request overhead
    this.requestLength = objectSize;

    if (request.getOperation() == Operation.METADATA) {
      if (request.getContext().get(Context.X_OG_OBJECT_SIZE) != null) {
        objectSize = Long.parseLong(request.getContext().get(Context.X_OG_OBJECT_SIZE));
      }
      else {
        objectSize = Long.parseLong(response.headers().get(HttpHeaders.CONTENT_LENGTH));
      }
    }

    if (response.getBody().getDataType() != DataType.NONE) {
      this.responseLength = response.getBody().getSize();
    } else {
      this.responseLength = null;
    }
    this.userAgent = userAgent;
    this.requestLatency = this.timestampFinish - this.timestampStart;

    // custom
    this.clientRequestId = request.getContext().get(Context.X_OG_REQUEST_ID);
    this.requestId = response.headers().get(X_CLV_REQUEST_ID);
    this.stat = new RequestStats(timestamps);
    // On overwrite, log the original size of the object before overwrite
    if(request.getOperation() == Operation.OVERWRITE) {
      this.originalObjectLength = Long.parseLong(request.getContext().get(Context.X_OG_OBJECT_SIZE));
    } else {
      this.originalObjectLength = null;
    }

    if (request.getOperation() == Operation.LIST || request.getOperation() == Operation.LIST_OBJECT_VERSIONS) {
      this.maxKeys = request.getContext().get(Context.X_OG_LIST_MAX_KEYS);
      this.listSessionId = request.getContext().get(Context.X_OG_LIST_SESSION_ID);
      this.listRequestNum = request.getContext().get(Context.X_OG_LIST_REQ_NUM);
      this.listMaxRequests = request.getContext().get(Context.X_OG_LIST_MAX_REQS);
      this.listPrefix = request.getContext().get(Context.X_OG_LIST_PREFIX);
      this.listDelimiter = request.getContext().get(Context.X_OG_LIST_DELIMITER);
      this.listContentSize = response.getContext().get(Context.X_OG_NUM_LIST_CONTENTS);
      this.listCommonPrefixesSize = response.getContext().get(Context.X_OG_NUM_LIST_COMMON_PREFIXES);
    }

    if (request.getOperation() == Operation.MULTI_DELETE) {
      this.multideleteReqObjects = request.getContext().get(Context.X_OG_MULTI_DELETE_REQUEST_OBJECTS_COUNT);
      this.multideleteFailedObjects = response.getContext().get(Context.X_OG_MULTI_DELETE_FAILED_OBJECTS_COUNT);
      this.multideleteDeletedObjects = response.getContext().get(Context.X_OG_MULTI_DELETE_SUCCESS_OBJECTS_COUNT);

      int deletedObjects = 0;
      if (this.multideleteDeletedObjects == null) {
        if (this.multideleteFailedObjects != null) {
          deletedObjects = Integer.parseInt(this.multideleteReqObjects) - Integer.parseInt(this.multideleteFailedObjects);
        } else {
          deletedObjects = Integer.parseInt(this.multideleteReqObjects);
        }
        this.multideleteDeletedObjects = String.valueOf(deletedObjects);
      }
    }

    this.objectLength = objectSize;
    this.objectName = operationObjectName;
    this.sourceObjectId = request.getContext().get(Context.X_OG_SSE_SOURCE_OBJECT_NAME);
    this.sourceUri = request.getContext().get(Context.X_OG_SSE_SOURCE_URI);
    this.retention = request.getContext().get(Context.X_OG_OBJECT_RETENTION);
    this.legalHold = request.getContext().get(Context.X_OG_LEGAL_HOLD);
    if (request.getOperation() == Operation.WRITE || request.getOperation() == Operation.OVERWRITE) {
      this.newObjectVersionId = response.headers().get("x-amz-version-id");
    }
    this.objectVersionId = request.getContext().get(Context.X_OG_OBJECT_VERSION);

  }

  public static class RequestStats {
    final Double requestContent;
    final Double closeLatency;
    final Double ttfb;
    final Double responseContent;
    final Double total;

    public RequestStats(final RequestTimestamps t) {
      this.requestContent = duration(t.requestContentStart, t.requestContentFinish);
      this.closeLatency = duration(t.requestContentFinish, t.finish);
      this.ttfb = duration(t.start, t.responseContentFirstBytes);
      this.responseContent = duration(t.responseContentStart, t.responseContentFinish);
      this.total = duration(t.start, t.finish);
    }

    private Double duration(final long start, final long finish) {
      if (start > 0 && finish > start) {
        return ((double) finish - start) / TimeUnit.MILLISECONDS.toNanos(1);
      }
      return null;
    }
  }
}
