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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.math.DoubleMath;
import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.Uninterruptibles;

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
  private final AtomicReference<RateLimiter> permits;
  private final CountDownLatch started;

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
    this.permits = new AtomicReference<RateLimiter>();

    // convert arbitrary rate unit to rate/second
    final double requestsPerSecond = requestsPerSecond(rate, unit);

    _logger.debug("Calculated requests per second [{}]", requestsPerSecond);
    final RateLimiter steady = RateLimiter.create(requestsPerSecond);

    if (DoubleMath.fuzzyEquals(rampup, 0.0, Math.pow(0.1, 6))) {
      this.permits.set(steady);
    } else {
      // two RateLimiters (ramp, steady) are used rather than one because the RateLimiter class
      // includes undesirable code which cools down request rate if inactive, but only if configured
      // with a rampup. Workaround is to configure one instance with rampup and another that uses a
      // fixed ops.
      final long rampDuration = (long) (rampup * rampupUnit.toNanos(1));
      this.permits.set(RateLimiter.create(requestsPerSecond, rampDuration, TimeUnit.NANOSECONDS));
      final Thread rampupThread = new Thread(new Runnable() {
        @Override
        public void run() {
          _logger.debug("Awaiting start latch");
          Uninterruptibles.awaitUninterruptibly(RequestRateScheduler.this.started);

          _logger.info("Starting ramp");
          _logger.debug("Sleeping for [{}] nanoseconds of ramp activity", rampDuration);
          Uninterruptibles.sleepUninterruptibly(rampDuration, TimeUnit.NANOSECONDS);

          _logger.debug("Swapping RateLimiter implementation from ramp to steady", rampDuration);
          RequestRateScheduler.this.permits.set(steady);

          _logger.info("Finished ramp");
        }

      }, "rate-scheduler-ramp");
      rampupThread.setDaemon(true);
      rampupThread.start();
    }
    this.started = new CountDownLatch(1);
  }

  double requestsPerSecond(final double rate, final TimeUnit unit) {
    return rate / (unit.toNanos(1) / (double) TimeUnit.SECONDS.toNanos(1));
  }

  @Override
  public void schedule() {
    this.started.countDown();
    this.permits.get().acquire();
  }

  @Override
  public String toString() {
    return String.format("RequestRateScheduler [rate=%s, unit=%s, rampup=%s, rampupUnit=%s]",
        this.rate, this.unit, this.rampup, this.rampupUnit);
  }
}
