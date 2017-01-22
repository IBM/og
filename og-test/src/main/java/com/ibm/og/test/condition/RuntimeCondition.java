/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.test.condition;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.og.test.LoadTest;
import com.google.common.util.concurrent.Uninterruptibles;

/**
 * A test condition which is triggered when a period of time has elapsed
 * 
 * @since 1.0
 */
public class RuntimeCondition implements TestCondition {
  private static final Logger _logger = LoggerFactory.getLogger(RuntimeCondition.class);
  private final LoadTest test;
  private final long runtime;
  private final TimeUnit unit;
  private final long timestampStart;
  private final boolean failureCondition;

  /**
   * Creates an instance
   * 
   * @param test the load test to stop when this condition is triggered
   * @param runtime the duration of time prior to the triggering of this condition
   * @param unit the duration unit
   * @throws NullPointerException if test or unit is null
   * @throws IllegalArgumentException if runtime is zero or negative
   */
  public RuntimeCondition(final LoadTest test, final double runtime, final TimeUnit unit,
      final boolean failureCondition) {
    this.test = checkNotNull(test);
    checkArgument(runtime > 0.0, "duration must be > 0.0 [%s]", runtime);
    this.unit = checkNotNull(unit);
    this.runtime = (long) (runtime * unit.toNanos(1));
    this.timestampStart = System.nanoTime();
    this.failureCondition = failureCondition;

    final Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        Uninterruptibles.sleepUninterruptibly(RuntimeCondition.this.runtime, TimeUnit.NANOSECONDS);
        if (failureCondition){
          RuntimeCondition.this.test.abortTest(String.format("Failed Condition: %s", toString()));
        } else {
          RuntimeCondition.this.test.stopTest();
        }
      }
    });
    t.setName("runtime-condition");
    t.setDaemon(true);
    t.start();
  }

  @Override
  public boolean isTriggered() {
    final long currentRuntime = System.nanoTime() - this.timestampStart;
    if (currentRuntime >= this.runtime) {
      _logger.info("{} is triggered [{}]", toString(), currentRuntime);
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format("RuntimeCondition [%n" + "runtime=%s,%n" + "unit=%s,%n" + "]",
        this.runtime, this.unit);
  }
}
