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
// @author: rveitch
//
// Date: Feb 21, 2014
// ---------------------

package com.cleversafe.oom.operation;

import java.net.URI;
import java.util.Map;

public interface RequestContext extends Request
{
   public RequestContext withId(final long id);

   public RequestContext withCustomRequestKey(final String customRequestKey);

   public RequestContext withMethod(final Method method);

   public RequestContext withURI(final URI uri);

   public RequestContext withHeader(final String key, final String value);

   public RequestContext withHeaders(Map<String, String> headers);

   public RequestContext withEntity(final Entity entity);

   public RequestContext withMetaDataEntry(final String key, final String value);

   public RequestContext withMetaData(Map<String, String> metadata);

   public Request build();
}
