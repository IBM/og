/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
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
