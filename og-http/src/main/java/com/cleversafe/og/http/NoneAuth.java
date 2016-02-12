/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.http;

import com.cleversafe.og.api.AuthenticatedRequest;
import com.cleversafe.og.api.Request;

public class NoneAuth implements HttpAuth {
  public NoneAuth() {}

  @Override
  public AuthenticatedRequest authenticate(final Request request) {
    return new AuthenticatedHttpRequest(request);
  }

  @Override
  public String toString() {
    return "NoneAuth []";
  }
}
