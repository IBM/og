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
  final String type = "http";
  final String serverName;
  final String remoteAddress;
  final String user;
  final long timestampStart;
  final long timestampFinish;
  final String timeStart;
  final String timeFinish;
  final Method requestMethod;
  final String requestUri;
  final String objectId;
  final int status;
  final Long requestLength;
  final long responseLength;
  final String userAgent;
  final long requestLatency;

  final String clientRequestId;
  final String requestId;
  final RequestStats stat;
  final Long objectLength;
  final String objectName;

  private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern(
      "dd/MMM/yyyy:HH:mm:ss Z").withLocale(Locale.US);
  private static final String X_CLV_REQUEST_ID = "X-Clv-Request-Id";

  /**
   * Constructs an instance
   * 
   * @param request the request for this operation
   * @param response the response for this operation
   * @param timestampStart the timestamp for the start of this request, in milliseconds
   * @param timestampFinish the timestamp for the end of this request, in milliseconds
   */
  public RequestLogEntry(final Request request, final Response response, final String userAgent,
      final RequestTimestamps timestamps) {
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

    String objectName = request.headers().get(Headers.X_OG_OBJECT_NAME);
    // SOH writes
    if (objectName == null) {
      objectName = response.headers().get(Headers.X_OG_OBJECT_NAME);
    }
    this.objectId = objectName;

    Long objectSize = null;
    if (DataType.NONE != request.getBody().getDataType()) {
      objectSize = request.getBody().getSize();
    }

    this.status = response.getStatusCode();
    // TODO requestLength will not equal objectLength with AWSv4 request overhead
    this.requestLength = objectSize;
    // TODO is this correct?
    this.responseLength = response.getBody().getSize();
    this.userAgent = userAgent;
    // TODO ask: dsnet access.log uses System.currentTimeMillis() - request.getTimeStamp();
    this.requestLatency = this.timestampFinish - this.timestampStart;

    // custom
    this.clientRequestId = request.headers().get(Headers.X_OG_REQUEST_ID);
    this.requestId = response.headers().get(X_CLV_REQUEST_ID);
    this.stat = new RequestStats(timestamps);
    this.objectLength = objectSize;
    this.objectName = objectName;
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
