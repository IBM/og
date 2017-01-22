/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.test.condition;

import com.ibm.og.api.Request;
import com.ibm.og.api.Response;
import com.ibm.og.statistic.Counter;
import com.ibm.og.statistic.Statistics;
import com.ibm.og.test.LoadTest;
import com.ibm.og.api.Operation;
import com.ibm.og.util.Pair;
import com.google.common.eventbus.Subscribe;

/**
 * A test condition which is triggered when a threshold number of concurrent requests is met
 * 
 * @since 1.0
 */
public class ConcurrentRequestCondition extends CounterCondition {

  public ConcurrentRequestCondition(final Operation operation, final long thresholdValue,
                                    final LoadTest test, final Statistics stats, final boolean failureCondition) {
    super(operation, Counter.ACTIVE_OPERATIONS, thresholdValue, test, stats, failureCondition);
  }

  @Subscribe
  public void update(final Request request) {
    if (isTriggered()) {
      if (this.failureCondition) {
        this.test.abortTest(String.format("Failed Condition: %s", toString()));
      } else {
        this.test.stopTest();
      }
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
