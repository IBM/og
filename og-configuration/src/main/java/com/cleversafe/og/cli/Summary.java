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
// Date: Jun 24, 2014
// ---------------------

package com.cleversafe.og.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.cleversafe.og.statistic.Counter;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.util.Operation;
import com.google.common.collect.Maps;

public class Summary {
  private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern(
      "dd/MMM/yyyy:HH:mm:ss Z").withLocale(Locale.US);
  private final Statistics stats;
  private SummaryStats summary;
  private final long timestampStart;
  private final long timestampFinish;
  private final double runtime;

  public Summary(final Statistics stats, final long timestampStart, final long timestampFinish) {
    this.stats = checkNotNull(stats);
    checkArgument(timestampStart >= 0, "timestampStart must be >= 0 [%s]", timestampStart);
    checkArgument(timestampStart <= timestampFinish,
        "timestampStart must be <= timestampFinish [%s, %s]", timestampStart, timestampFinish);
    this.timestampStart = timestampStart;
    this.timestampFinish = timestampFinish;
    this.runtime = (double) (timestampFinish - timestampStart) / TimeUnit.SECONDS.toMillis(1);
  }

  private static class SummaryStats {
    private final long timestampStart;
    private final long timestampFinish;
    private final double runtime;
    private long operations;
    private final OperationStats write = new OperationStats();
    private final OperationStats read = new OperationStats();
    private final OperationStats delete = new OperationStats();

    public SummaryStats(final long timestampStart, final long timestampFinish, final double runtime) {
      this.timestampStart = timestampStart;
      this.timestampFinish = timestampFinish;
      this.runtime = runtime;
    }
  }

  private static class OperationStats {
    private long operations = 0;
    private long bytes = 0;
    private final Map<Integer, Long> statusCodes = Maps.newTreeMap();
  }

  @Override
  public String toString() {
    retrieveStats();
    final String format =
        "Start: %s%nEnd: %s%nRuntime: %.2f Seconds%nOperations: %s%n%n[Write]%n%s[Read]%n%s[Delete]%n%s";
    return String.format(Locale.US, format, FORMATTER.print(this.summary.timestampStart),
        FORMATTER.print(this.summary.timestampFinish), this.summary.runtime,
        this.summary.operations, getOperation(this.summary.write), getOperation(this.summary.read),
        getOperation(this.summary.delete));
  }

  private String getOperation(final OperationStats opStats) {
    final String format = "Operations: %s%nBytes: %s%nStatus Codes:%n%s%n";
    String statusCodes = getStatusCodes(opStats);
    if (statusCodes.length() == 0)
      statusCodes = String.format("N/A%n");

    return String.format(Locale.US, format, opStats.operations, opStats.bytes, statusCodes);
  }

  private String getStatusCodes(final OperationStats opStats) {
    final StringBuilder s = new StringBuilder();
    for (final Entry<Integer, Long> sc : opStats.statusCodes.entrySet()) {
      s.append(sc.getKey()).append(": ").append(sc.getValue()).append(String.format("%n"));
    }
    return s.toString();
  }

  public SummaryStats getSummaryStats() {
    retrieveStats();
    return this.summary;
  }

  private void retrieveStats() {
    if (this.summary != null)
      return;

    this.summary = new SummaryStats(this.timestampStart, this.timestampFinish, this.runtime);
    retrieveStats(this.summary.write, Operation.WRITE);
    retrieveStats(this.summary.read, Operation.READ);
    retrieveStats(this.summary.delete, Operation.DELETE);
    final OperationStats all = new OperationStats();
    retrieveStats(all, Operation.ALL);
    this.summary.operations = all.operations;
  }

  private void retrieveStats(final OperationStats opStats, final Operation operation) {
    opStats.operations = this.stats.get(operation, Counter.OPERATIONS);
    opStats.bytes = this.stats.get(operation, Counter.BYTES);
    final Iterator<Entry<Integer, Long>> it = this.stats.statusCodeIterator(operation);
    while (it.hasNext()) {
      final Entry<Integer, Long> sc = it.next();
      opStats.statusCodes.put(sc.getKey(), sc.getValue());
    }
  }
}
