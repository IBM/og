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
// Date: Jun 16, 2014
// ---------------------

package com.cleversafe.og.http.auth;

import com.cleversafe.og.operation.Request;

/**
 * A creator of http authorization headers
 * 
 * @since 1.0
 */
public interface HttpAuth
{
   /**
    * Creates an http authorization header value
    * 
    * @param request
    *           the request to create the authorization header value for
    * @return the created authorization header value for the provided request
    */
   String nextAuthorizationHeader(final Request request);
}
