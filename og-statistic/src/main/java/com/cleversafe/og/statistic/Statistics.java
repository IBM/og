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
// Date: Jun 21, 2014
// ---------------------

package com.cleversafe.og.statistic;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.http.HttpUtil;
import com.cleversafe.og.util.Operation;
import com.cleversafe.og.util.Pair;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AtomicLongMap;

/**
 * An aggregator of counters including:
 * <ul>
 * <li>operations</li>
 * <li>bytes</li>
 * <li>status codes</li>
 * </ul>
 * <p>
 * statistics are gathered and stored for the following operation types:
 * <ul>
 * <li>all</li>
 * <li>write</li>
 * <li>read</li>
 * <li>delete</li>
 * </ul>
 * 
 * @since 1.0
 */
public class Statistics {
  private final Map<Operation, AtomicLongMap<Counter>> counters;
  private final Map<Operation, AtomicLongMap<Integer>> scCounters;

  /**
   * Constructs an instance
   */
  public Statistics() {
    this.counters = Maps.newHashMap();
    this.scCounters = Maps.newHashMap();
    for (final Operation operation : Operation.values()) {
      this.counters.put(operation, AtomicLongMap.<Counter>create());
      this.scCounters.put(operation, AtomicLongMap.<Integer>create());
    }
  }

  /**
   * Updates this instance with data from a completed operation
   * 
   * @param result the completed operation
   */
  @Subscribe
  public void update(final Pair<Request, Response> result) {
    checkNotNull(result);
    final Request request = result.getKey();
    final Response response = result.getValue();

    final Operation operation = HttpUtil.toOperation(request.getMethod());
    updateCounter(operation, Counter.OPERATIONS, 1);
    updateCounter(Operation.ALL, Counter.OPERATIONS, 1);

    final long bytes = getBytes(operation, request, response);
    updateCounter(operation, Counter.BYTES, bytes);
    updateCounter(Operation.ALL, Counter.BYTES, bytes);
    updateStatusCode(operation, response.getStatusCode());
    updateStatusCode(Operation.ALL, response.getStatusCode());
  }

  private long getBytes(final Operation operation, final Request request, final Response response) {
    if (Operation.WRITE == operation)
      return request.getBody().getSize();
    else if (Operation.READ == operation)
      return response.getBody().getSize();
    return 0;
  }

  private void updateCounter(final Operation operation, final Counter counter, final long value) {
    this.counters.get(operation).addAndGet(counter, value);
  }

  private void updateStatusCode(final Operation operation, final int statusCode) {
    this.scCounters.get(operation).incrementAndGet(statusCode);
  }

  /**
   * Gets a counter
   * 
   * @param operation the operation type of the counter to get
   * @param counter the counter type to get
   * @return the counter's current value
   */
  public long get(final Operation operation, final Counter counter) {
    checkNotNull(operation);
    checkNotNull(counter);
    return this.counters.get(operation).get(counter);
  }

  /**
   * Gets a status code counter
   * 
   * @param operation the operation type of the counter to get
   * @param statusCode the status code
   * @return the current status code value
   */
  public long getStatusCode(final Operation operation, final int statusCode) {
    checkNotNull(operation);
    checkArgument(HttpUtil.VALID_STATUS_CODES.contains(statusCode),
        "statusCode must be a valid status code [%s]", statusCode);

    return this.scCounters.get(operation).get(statusCode);
  }

  /**
   * Gets a read-only map of status code counters for a given operation type
   * 
   * @param operation the operatino type to get status code counter values for
   * @return a map of status code counters
   */
  public Map<Integer, Long> statusCodes(final Operation operation) {
    checkNotNull(operation);
    return this.scCounters.get(operation).asMap();
  }

  @Override
  public String toString() {
    return "Statistics []";
  }
}
