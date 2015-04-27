/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.test;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;

/**
 * An {@code EventBus} exception handler. This handler aborts the OG load test when an eventbus
 * exception occurs.
 * 
 * @since 1.0
 */
public class LoadTestSubscriberExceptionHandler implements SubscriberExceptionHandler {
  private static final Logger _logger = LoggerFactory
      .getLogger(LoadTestSubscriberExceptionHandler.class);
  private LoadTest test;

  /**
   * Creates an instance
   */
  public LoadTestSubscriberExceptionHandler() {}

  @Override
  public void handleException(final Throwable exception, final SubscriberExceptionContext context) {
    _logger.error("Exception while processing subscriber", exception);
    this.test.abortTest();
  }

  /**
   * Set the load test for this instance to abort in the event that an event bus exception occurs
   * 
   * @param test the load test to manage
   */
  public void setLoadTest(final LoadTest test) {
    this.test = checkNotNull(test);
  }
}
