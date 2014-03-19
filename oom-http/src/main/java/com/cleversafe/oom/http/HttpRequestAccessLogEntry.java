//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: shoran
//
// Date: Aug 19, 2013
// ---------------------

package com.cleversafe.oom.http;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.operation.Response;
import com.google.common.net.HttpHeaders;

public class HttpRequestAccessLogEntry
{
   final String serverName;
   final String remoteAddress;
   final String forwardedFor;
   final String user;
   final long timestampStart;
   final long timestampFinish;
   final String timeStart;
   final String timeFinish;
   final String requestMethod;
   final String requestUri;
   final String object_id;
   final String protocol;
   final int status;
   final Long requestLength;
   final long responseLength;
   final String referer;
   final String userAgent;
   final long requestLatency;

   final String requestId;
   final String stat;
   final String midstreamError;
   final Long objectLength;
   final String interfaceType;
   final String versionName;
   final Boolean versionTransient;
   final Boolean deleteMarker;

   final Map<String, String> principals;
   final String type = "http";

   private static final DateTimeFormatter formatter =
         DateTimeFormat.forPattern("dd/MMM/yyyy:HH:mm:ss Z").withZone(DateTimeZone.UTC).withLocale(
               Locale.US);

   public HttpRequestAccessLogEntry(final Request request, final Response response)
   {
      String serverName = null;
      try
      {
         serverName = InetAddress.getLocalHost().getHostAddress();
      }
      catch (final UnknownHostException e)
      {
      }
      this.serverName = serverName;
      this.remoteAddress = request.getURL().getHost();
      this.forwardedFor = request.getHeader(HttpHeaders.X_FORWARDED_FOR);
      this.user = null;
      this.timestampStart = 0;
      this.timestampFinish = 0;
      this.timeStart = HttpRequestAccessLogEntry.formatter.print(this.timestampStart);
      this.timeFinish = HttpRequestAccessLogEntry.formatter.print(this.timestampFinish);
      this.requestMethod = request.getMethod().toString();
      this.requestUri = request.getURL().getFile();
      this.object_id = null;
      this.protocol = request.getURL().getProtocol();
      this.status = response.getStatusCode();
      this.requestLength = null;
      this.responseLength = 0;
      this.referer = request.getHeader(HttpHeaders.REFERER);
      this.userAgent = request.getHeader(HttpHeaders.USER_AGENT);
      this.requestLatency = 0;

      this.requestId = null;
      this.stat = null;
      this.midstreamError = null;
      this.objectLength = null;
      this.interfaceType = null;
      this.versionName = null;
      this.versionTransient = null;
      this.deleteMarker = null;

      this.principals = Collections.emptyMap();
   }
}
