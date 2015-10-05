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

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

/**
 * A scheduler which permits calls at a configured rate
 * 
 * @since 1.0
 */
public class RequestRateScheduler implements Scheduler {
  private static final Logger _logger = LoggerFactory.getLogger(RequestRateScheduler.class);
  private final double rate;
  private final TimeUnit unit;
  private final double rampup;
  private final TimeUnit rampupUnit;
  private final long rampDuration;
  private final RateLimiter ramp;
  private final RateLimiter steady;
  private Thread requestThread;
  private final Semaphore permits;

  /**
   * Constructs an instance using the provided rate {@code count / unit }
   * 
   * @param rate the numerator of the rate to configure
   * @param unit the denominator of the rate to configure
   * @param rampup the duration to ramp up to the stable request rate
   * @param rampupUnit the rampup duration unit
   */
  public RequestRateScheduler(final double rate, final TimeUnit unit, final double rampup,
      final TimeUnit rampupUnit) {
    checkArgument(rate >= 0.0, "rate must be >= 0.0 [%s]", rate);
    this.rate = rate;
    this.unit = checkNotNull(unit);
    checkArgument(rampup >= 0.0, "rampup must be >= 0.0 [%s]", rampup);
    this.rampup = rampup;
    this.rampupUnit = checkNotNull(rampupUnit);

    // convert arbitrary rate unit to rate/second
    final double requestsPerSecond =
        rate / (unit.toNanos(1) / (double) TimeUnit.SECONDS.toNanos(1));

    this.rampDuration = (long) (rampup * rampupUnit.toNanos(1));
    if (this.rampDuration > 0.0) {
      this.ramp = RateLimiter.create(requestsPerSecond, this.rampDuration, TimeUnit.NANOSECONDS);
    } else {
      this.ramp = null;
    }

    this.steady = RateLimiter.create(requestsPerSecond);
    this.permits = new Semaphore(0);
  }

  @Override
  public void waitForNext() {
    if (this.requestThread == null) {
      this.requestThread = new Thread(new Permitter());
      this.requestThread.setDaemon(true);
      this.requestThread.start();
    }

    try {
      this.permits.acquire();
    } catch (final InterruptedException e) {
      _logger.info("Interrupted while waiting to schedule next request", e);
    }
  }

  private class Permitter implements Runnable {
    public Permitter() {}

    @Override
    public void run() {
      if (RequestRateScheduler.this.ramp != null) {
        rampWait();
      }
      steadyWait();
    }

    private void rampWait() {
      final long start = System.nanoTime();
      while (System.nanoTime() - start < RequestRateScheduler.this.rampDuration) {
        RequestRateScheduler.this.steady.acquire();
        RequestRateScheduler.this.permits.release();
      }
    }

    private void steadyWait() {
      while (true) {
        RequestRateScheduler.this.steady.acquire();
        RequestRateScheduler.this.permits.release();
      }
    }
  }

  @Override
  public String toString() {
    return String.format(
        "RequestRateScheduler [rate=%s, unit=%s, rampup=%s, rampupUnit=%s, rampDuration=%s]",
        this.rate, this.unit, this.rampup, this.rampupUnit, this.rampDuration);
  }
}
