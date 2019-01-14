/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.api;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * An executor of requests
 * 
 * @since 1.0
 */
public interface Client {
  /**
   * Executes a request asynchronously
   * 
   * @param request the request to execute
   * @return A future representing the eventual completion of this request
   */
  ListenableFuture<Response> execute(Request request);

  /**
   * Shuts down this client
   * 
   * @param immediate if true, shuts down this client immediately, else wait for ongoing requests to complete
   * @param timeout amount of time in seconds to wait for requests if shutdown is not immediate
   * @return a future representing the eventual shutdown of this client. When the future has
   *         completed, a value of 0 indicates a successful shutdown, while a positive value
   *         indicates the number of terminated requests
   */
  ListenableFuture<Integer> shutdown(boolean immediate, int timeout);
}
