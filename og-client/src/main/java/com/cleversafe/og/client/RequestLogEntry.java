/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.client;

import java.net.URI;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.cleversafe.og.api.DataType;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.http.Headers;

/**
 * A class for assisting in the serialization of a request / response pair
 * 
 * @since 1.0
 */
public class RequestLogEntry {
  private static final String HTTP_TYPE = "http";
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
  public final String objectId;
  public final int status;
  public final Long requestLength;
  public final Long responseLength;
  public final String userAgent;
  public final long requestLatency;
  public final String clientRequestId;
  public final String requestId;
  public final RequestStats stat;
  public final Long objectLength;
  public final String objectName;

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
    this.type = HTTP_TYPE;
    final URI uri = request.getUri();
    // FIXME reliably get localaddress? Name should be clientName? Do we even need this field?
    this.serverName = null;
    this.remoteAddress = uri.getHost();
    this.user = request.headers().get(Headers.X_OG_USERNAME);
    this.timestampStart = timestamps.startMillis;
    this.timestampFinish = timestamps.finishMillis;
    this.timeStart = RequestLogEntry.FORMATTER.print(this.timestampStart);
    this.timeFinish = RequestLogEntry.FORMATTER.print(this.timestampFinish);
    this.requestMethod = request.getMethod();

    this.requestUri = uri.getPath() + (uri.getQuery() != null ? uri.getQuery() : "");

    String operationObjectName = request.headers().get(Headers.X_OG_OBJECT_NAME);
    // SOH writes
    if (operationObjectName == null) {
      operationObjectName = response.headers().get(Headers.X_OG_OBJECT_NAME);
    }
    this.objectId = operationObjectName;

    Long objectSize = null;
    if (DataType.NONE != request.getBody().getDataType()) {
      objectSize = request.getBody().getSize();
    }

    this.status = response.getStatusCode();
    // TODO requestLength will not equal objectLength with AWSv4 request overhead
    this.requestLength = objectSize;
    if (response.getBody().getDataType() != DataType.NONE) {
      this.responseLength = response.getBody().getSize();
    } else {
      this.responseLength = null;
    }
    this.userAgent = userAgent;
    this.requestLatency = this.timestampFinish - this.timestampStart;

    // custom
    this.clientRequestId = request.headers().get(Headers.X_OG_REQUEST_ID);
    this.requestId = response.headers().get(X_CLV_REQUEST_ID);
    this.stat = new RequestStats(timestamps);
    this.objectLength = objectSize;
    this.objectName = operationObjectName;
  }

  public static class RequestTimestamps {
    public long startMillis;
    public long start;
    public long requestContentStart;
    public long requestContentFinish;
    public long responseContentStart;
    public long responseContentFirstBytes;
    public long responseContentFinish;
    public long finish;
    public long finishMillis;
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
