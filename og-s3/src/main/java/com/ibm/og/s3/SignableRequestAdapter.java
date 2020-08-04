/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.s3;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import com.ibm.og.api.AuthType;
import com.ibm.og.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ReadLimitInfo;
import com.amazonaws.SignableRequest;
import com.amazonaws.http.HttpMethodName;
import com.ibm.og.api.Request;
import com.ibm.og.http.AuthenticatedHttpRequest;

/**
 * An adapter class to bridge between OG {@code Request} and amazonaws {@code SignableRequest}.
 * 
 * @since 1.0
 */
public class SignableRequestAdapter implements SignableRequest<Request> {
  private static Logger _logger = LoggerFactory.getLogger(SignableRequestAdapter.class);

  private final AuthenticatedHttpRequest request;
  private final URI endpoint;
  private final String resourcePath;
  private final ReadLimitInfo readLimitInfo;
  private int timeOffset;
  private final AuthType authType;

  public SignableRequestAdapter(final AuthenticatedHttpRequest request, AuthType authType) {
    this.request = checkNotNull(request);
    this.endpoint =
        URI.create(request.getUri().getScheme() + "://" + request.getUri().getAuthority());
    this.resourcePath = this.request.getUri().getPath();
    this.readLimitInfo = new ReadLimitInfo() {
      @Override
      public int getReadLimit() {
        return Integer.MAX_VALUE;
      }
    };
    this.authType = authType;
  }

  @Override
  public void addHeader(final String name, final String value) {
    this.request.addHeader(name, value);
  }

  @Override
  public Map<String, String> getHeaders() {
    return this.request.headers();
  }

  @Override
  public String getResourcePath() {
    if (authType == AuthType.AWSV2 &&
        request.getContext().containsKey(Context.X_OG_STATIC_WEBSITE_VIRTUAL_HOST_SUFFIX)) {
      String host = request.headers().get("Host");
      checkNotNull(host);
      final String bucket = host.substring(0, host.indexOf("."));
      return "/" + bucket + this.resourcePath;
    }
    return this.resourcePath;

  }

  @Override
  public void addParameter(final String name, final String value) {
    this.request.addQueryParameter(name, value);
  }

  @Override
  public Map<String, List<String>> getParameters() {
    return this.request.getQueryParameters();
  }

  @Override
  public URI getEndpoint() {
    return this.endpoint;
  }

  @Override
  public HttpMethodName getHttpMethod() {
    return HttpMethodName.valueOf(this.request.getMethod().toString());
  }

  @Override
  public int getTimeOffset() {
    return this.timeOffset;
  }

  @Override
  public InputStream getContent() {
    return this.request.getContent();
  }

  @Override
  public InputStream getContentUnwrapped() {
    return getContent();
  }

  @Override
  public ReadLimitInfo getReadLimitInfo() {
    return this.readLimitInfo;
  }

  @Override
  public Object getOriginalRequestObject() {
    return null;
  }

  @Override
  public void setContent(final InputStream content) {
    this.request.setContent(content);
  }
}
