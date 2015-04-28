/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.scheduling;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.util.Pair;
import com.google.common.eventbus.Subscribe;
import com.google.common.math.DoubleMath;
import com.google.common.util.concurrent.Uninterruptibles;

/**
 * A scheduler which simulates concurrent actions
 * 
 * @since 1.0
 */
public class ConcurrentRequestScheduler implements Scheduler {
  private static final Logger _logger = LoggerFactory.getLogger(ConcurrentRequestScheduler.class);
  private final int concurrentRequests;
  private final double rampup;
  private final TimeUnit rampupUnit;
  private final long rampDuration;
  private final Semaphore sem;
  private final CountDownLatch started;

  /**
   * Constructs an instance with the provided concurrency
   * 
   * @param concurrentRequests the number of concurrent requests allowed
   * @throws IllegalArgumentException if concurrentRequests is negative or zero
   */
  public ConcurrentRequestScheduler(final int concurrentRequests, final double rampup,
      final TimeUnit rampupUnit) {
    checkArgument(concurrentRequests > 0, "concurrentRequests must be > 0");
    checkArgument(rampup >= 0.0, "rampup must be >= 0.0 [%s]", rampup);
    checkNotNull(rampupUnit);
    this.concurrentRequests = concurrentRequests;
    this.rampup = rampup;
    this.rampupUnit = rampupUnit;
    this.rampDuration = (long) (rampup * rampupUnit.toNanos(1));
    this.started = new CountDownLatch(1);

    if (DoubleMath.fuzzyEquals(this.rampDuration, 0.0, Math.pow(0.1, 6)))
      this.sem = new Semaphore(concurrentRequests - 1);
    else {
      this.sem = new Semaphore(0);
      final Thread rampupThread = new Thread(new Runnable() {
        @Override
        public void run() {
          final CountDownLatch started = ConcurrentRequestScheduler.this.started;
          final int interval = ConcurrentRequestScheduler.this.concurrentRequests - 1;
          final long sleepDuration = ConcurrentRequestScheduler.this.rampDuration / interval;

          Uninterruptibles.awaitUninterruptibly(started);
          for (int i = 0; i < interval; i++) {
            Uninterruptibles.sleepUninterruptibly(sleepDuration, TimeUnit.NANOSECONDS);
            ConcurrentRequestScheduler.this.sem.release();
          }
        }
      });
      rampupThread.setDaemon(true);
      rampupThread.start();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * This implementation blocks until a previously scheduled request has completed
   */
  @Override
  public void waitForNext() {
    this.started.countDown();
    try {
      this.sem.acquire();
    } catch (final InterruptedException e) {
      _logger.warn("Interrupted while waiting to schedule next request", e);
      return;
    }
  }

  /**
   * Informs this scheduler that it should allow the calling thread on {@link #waitForNext} to
   * proceed
   * 
   * @param operation the operation for the completed request
   */
  @Subscribe
  public void complete(final Pair<Request, Response> operation) {
    this.sem.release();
  }

  @Override
  public String toString() {
    return String
        .format(
            "ConcurrentRequestScheduler [concurrentRequests=%s, rampup=%s, rampupUnit=%s, rampDuration=%s]",
            this.concurrentRequests, this.rampup, this.rampupUnit, this.rampDuration);
  }
}
