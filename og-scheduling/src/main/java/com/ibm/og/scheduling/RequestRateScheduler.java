/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.scheduling;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.RoundingMode;
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


    if (DoubleMath.fuzzyEquals(rampup, 0.0, Math.pow(0.1, 6))) {
      final RateLimiter steady = RateLimiter.create(requestsPerSecond);
      this.permits.set(steady);
    } else {
      // the warmup Ratelimiter will not work if the permit request rate is slow enough to not being able to reach the
      // threshold from left. The permits are accumulated faster than the request rate here.
      // So the steady OPS will not be reached at all during warm up period.
      // Approximate the warm-up period with steady ratelimiter and set the ops for the steady rate limiter
      // based on the steady state ops, warm up duration.

      // calculate the ops based on the ramp duration and steady state ops
      final double slope  = requestsPerSecond / (rampupUnit.toSeconds((long)rampup));
      final int rampStepWidth = calculateStepWidth(rate, rampup, rampupUnit);

      this.permits.set(RateLimiter.create(slope *  rampStepWidth * 1));

      final Thread rampupThread = new Thread(new Runnable() {
        @Override
        public void run() {
          _logger.debug("Awaiting start latch");
          Uninterruptibles.awaitUninterruptibly(RequestRateScheduler.this.started);

          _logger.info("Starting ramp");

          double requestsPerSecondNow;
          RateLimiter rampRateLimiter;
          int rampStepNum = 1;
          int rampSteps =  DoubleMath.roundToInt(((rampupUnit.toSeconds((long) rampup)) / rampStepWidth),
                  RoundingMode.DOWN);
          _logger.info("ramp profile rampStepWidth {}  NumRampSteps {} ", rampStepWidth, rampSteps);
          while (rampStepNum <= rampSteps) {
            Uninterruptibles.sleepUninterruptibly(rampStepWidth * 1000L, TimeUnit.MILLISECONDS);
            rampStepNum++;
            requestsPerSecondNow = slope *  rampStepWidth * rampStepNum;
            _logger.debug("slope {} rampStep  {}  targetRequestPerSecond {} ", slope, rampStepNum, requestsPerSecondNow);
            rampRateLimiter = RateLimiter.create(requestsPerSecondNow);
            RequestRateScheduler.this.permits.set(rampRateLimiter);
          }
          final RateLimiter steady = RateLimiter.create(requestsPerSecond);
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

  private int calculateStepWidth(double ops, double warmUp, TimeUnit rampupUnit) {

    double warmUpSeconds = rampupUnit.toSeconds((long)warmUp);
    double slope = ops / warmUpSeconds;

    int width = 1;
    if (slope < 1.0) {
      width = DoubleMath.roundToInt((warmUpSeconds / ops), RoundingMode.DOWN);
    }

    return width;
  }

}
