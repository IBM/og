//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: rveitch
//
// Date: Feb 7, 2014
// ---------------------

package com.cleversafe.og.scheduling;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.util.distribution.Distribution;
import com.google.common.util.concurrent.RateLimiter;

/**
 * A scheduler which permits calls at a configured rate
 * 
 * @since 1.0
 */
// TODO refactor implementation
public class RequestRateScheduler implements Scheduler {
  private static final Logger _logger = LoggerFactory.getLogger(RequestRateScheduler.class);
  private final Distribution count;
  private final TimeUnit unit;
  private RateLimiter ramp;
  private final long rampDuration;
  private long firstCalledTimestamp;
  private long lastCalledTimestamp;

  /**
   * Construcst an instace using the provided rate {@code count / unit }
   * 
   * @param count the numerator of the rate to configure
   * @param unit the denominator of the rate to configure
   * @param rampup the duration to ramp up to the stable request rate
   * @param rampupUnit the rampup duration unit
   */
  public RequestRateScheduler(final Distribution count, final TimeUnit unit, final double rampup,
      final TimeUnit rampupUnit) {
    this.count = checkNotNull(count);
    this.unit = checkNotNull(unit);
    checkArgument(rampup >= 0.0, "rampup must be >= 0.0 [%s]", rampup);
    checkNotNull(rampupUnit);
    this.rampDuration = (long) (rampup * rampupUnit.toNanos(1));
    if (this.rampDuration > 0.0)
      this.ramp = RateLimiter.create(count.getAverage(), this.rampDuration, TimeUnit.NANOSECONDS);
  }

  @Override
  public void waitForNext() {
    final long timestamp = System.nanoTime();
    if (this.firstCalledTimestamp == 0)
      this.firstCalledTimestamp = timestamp;

    if (timestamp - this.firstCalledTimestamp < this.rampDuration)
      this.lastCalledTimestamp = rampWait();
    else
      this.lastCalledTimestamp = steadyWait(timestamp);
  }

  private long rampWait() {
    this.ramp.acquire();
    return System.nanoTime();
  }

  private long steadyWait(long timestamp) {
    long sleepRemaining = nextSleepDuration() - adjustment(timestamp);
    while (sleepRemaining > 0) {
      try {
        TimeUnit.NANOSECONDS.sleep(sleepRemaining);
      } catch (final InterruptedException e) {
        _logger.info("Interrupted while waiting to schedule next request", e);
        this.lastCalledTimestamp = System.nanoTime();
        return timestamp;
      }
      final long endTimestamp = System.nanoTime();
      final long sleptTime = endTimestamp - timestamp;
      timestamp = endTimestamp;
      sleepRemaining -= sleptTime;
    }
    return timestamp;
  }

  private final long nextSleepDuration() {
    return (long) (this.unit.toNanos(1) / this.count.nextSample());
  }

  private final long adjustment(final long timestamp) {
    if (this.lastCalledTimestamp > 0)
      return timestamp - this.lastCalledTimestamp;
    return 0;
  }

  @Override
  public String toString() {
    return "RequestRateScheduler [count=" + this.count + ", unit=" + this.unit + "]";
  }
}
