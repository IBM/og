/* Copyright (c) IBM Corporation 2020. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.scheduling;

import com.google.common.math.DoubleMath;
import com.google.common.util.concurrent.Uninterruptibles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.RoundingMode;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.ibm.og.scheduling.customratelimiters.PoissonRateLimiter;

/**
 * A scheduler which permits calls being generated at a varying rate, based on a Poisson process.
 * 
 * @since 1.10.0
 */
public class PoissonRequestRateScheduler implements Scheduler {
  private static final Logger _logger = LoggerFactory.getLogger(PoissonRequestRateScheduler.class);
  private final double rate;
  private final TimeUnit unit;
  private final double rampup;
  private final TimeUnit rampupUnit;
  private final AtomicReference<PoissonRateLimiter> permits;
  private final CountDownLatch started;

  /**
   * Constructs an instance using the provided rate {@code count / unit }
   *
   * @param rate the numerator of the rate to configure
   * @param unit the denominator of the rate to configure
   * @param rampup the duration to ramp up to the stable request rate
   * @param rampupUnit the rampup duration unit
   */
  public PoissonRequestRateScheduler(final double rate, final TimeUnit unit, final double rampup,
                                     final TimeUnit rampupUnit) {
    checkArgument(rate > 0.0, "rate must be > 0.0 [%s]", rate);
    this.rate = rate;
    this.unit = checkNotNull(unit);
    checkArgument(rampup >= 0.0, "rampup must be >= 0.0 [%s]", rampup);
    this.rampup = rampup;
    this.rampupUnit = checkNotNull(rampupUnit);
    this.permits = new AtomicReference<PoissonRateLimiter>();

    // convert arbitrary rate unit to rate/second
    final double requestsPerSecond = requestsPerSecond(rate, unit);

    _logger.debug("Calculated requests per second [{}]", requestsPerSecond);


    if (DoubleMath.fuzzyEquals(rampup, 0.0, Math.pow(0.1, 6))) {
      final PoissonRateLimiter steady = PoissonRateLimiter.create(requestsPerSecond);
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

      this.permits.set(PoissonRateLimiter.create(slope *  rampStepWidth * 1));

      final Thread rampupThread = new Thread(new Runnable() {
        @Override
        public void run() {
          _logger.debug("Awaiting start latch");
          Uninterruptibles.awaitUninterruptibly(PoissonRequestRateScheduler.this.started);

          _logger.info("Starting ramp");

          double requestsPerSecondNow;
          PoissonRateLimiter rampRateLimiter;
          int rampStepNum = 1;
          int rampSteps =  DoubleMath.roundToInt(((rampupUnit.toSeconds((long) rampup)) / rampStepWidth),
                  RoundingMode.DOWN);
          _logger.info("ramp profile rampStepWidth {}  NumRampSteps {} ", rampStepWidth, rampSteps);
          while (rampStepNum <= rampSteps) {
            Uninterruptibles.sleepUninterruptibly(rampStepWidth * 1000L, TimeUnit.MILLISECONDS);
            rampStepNum++;
            requestsPerSecondNow = slope *  rampStepWidth * rampStepNum;
            _logger.debug("slope {} rampStep  {}  targetRequestPerSecond {} ", slope, rampStepNum, requestsPerSecondNow);
            rampRateLimiter = PoissonRateLimiter.create(requestsPerSecondNow);
            PoissonRequestRateScheduler.this.permits.set(rampRateLimiter);
          }
          final PoissonRateLimiter steady = PoissonRateLimiter.create(requestsPerSecond);
          PoissonRequestRateScheduler.this.permits.set(steady);

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
  public void complete() {
    // nothing to do for this scheduler type
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
