/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.scheduling;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.og.api.Request;
import com.ibm.og.api.Response;
import com.ibm.og.util.Pair;
import com.google.common.eventbus.Subscribe;
import com.google.common.math.DoubleMath;
import com.google.common.util.concurrent.RateLimiter;
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
  private final Semaphore permits;
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
    this.started = new CountDownLatch(1);

    if (DoubleMath.fuzzyEquals(rampup, 0.0, Math.pow(0.1, 6))) {
      this.permits = new Semaphore(concurrentRequests);
    } else {
      this.permits = new Semaphore(0);
      final Thread rampupThread = new Thread(new Runnable() {
        @Override
        public void run() {
          final double rampSeconds = (rampup * rampupUnit.toNanos(1)) / TimeUnit.SECONDS.toNanos(1);
          _logger.debug("Ramp seconds [{}]", rampSeconds);

          final RateLimiter ramp = RateLimiter.create(concurrentRequests / rampSeconds);
          _logger.debug("Ramp rate [{}]", ramp.getRate());

          _logger.debug("Awaiting start latch");
          Uninterruptibles.awaitUninterruptibly(ConcurrentRequestScheduler.this.started);

          _logger.info("Starting ramp");
          for (int i = 0; i < concurrentRequests; i++) {
            _logger.debug("Acquiring RateLimiter permit");
            ramp.acquire();
            _logger.debug("Releasing semaphore permit");
            ConcurrentRequestScheduler.this.permits.release();
          }
          _logger.info("Finished ramp");
        }
      }, "concurrent-scheduler-ramp");
      rampupThread.setDaemon(true);
      rampupThread.start();
      _logger.debug("Starting permits [{}]", this.permits.availablePermits());
    }
  }

  /**
   * {@inheritDoc}
   * 
   * This implementation blocks until a previously scheduled request has completed
   */
  @Override
  public void schedule() {
    this.started.countDown();
    this.permits.acquireUninterruptibly();
  }

  /**
   * {@inheritDoc}
   * 
   * Informs this scheduler that it should allow the calling thread on {@link #schedule} to proceed
   */
  @Override
  public void complete() {
    this.permits.release();
  }

  @Override
  public String toString() {
    return String.format(
        "ConcurrentRequestScheduler [concurrentRequests=%s, rampup=%s, rampupUnit=%s]",
        this.concurrentRequests, this.rampup, this.rampupUnit);
  }
}
