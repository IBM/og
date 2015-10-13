/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.test.condition;

import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.statistic.Counter;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.LoadTest;
import com.cleversafe.og.util.Operation;
import com.cleversafe.og.util.Pair;
import com.google.common.eventbus.Subscribe;

/**
 * A test condition which is triggered when a threshold number of concurrent requests is met
 * 
 * @since 1.0
 */
public class ConcurrentRequestCondition extends CounterCondition {

  public ConcurrentRequestCondition(final Operation operation, final long thresholdValue,
      final LoadTest test, final Statistics stats) {
    super(operation, Counter.ACTIVE_OPERATIONS, thresholdValue, test, stats);
  }

  @Subscribe
  public void update(final Request request) {
    if (isTriggered()) {
      this.test.stopTest();
    }
  }

  @Subscribe
  @Override
  public void update(final Pair<Request, Response> operation) {
    // prevent parent class implementation from being invoked
  }


  @Override
  public String toString() {
    return String.format(
        "ConcurrentRequestCondition [%n" + "operation=%s,%n" + "thresholdValue=%s%n" + "]",
        this.operation, this.thresholdValue);
  }
}
