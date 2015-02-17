/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.cleversafe.og.statistic.Counter;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.util.Operation;
import com.google.common.collect.ImmutableSortedMap;

public class Summary {
  private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern(
      "dd/MMM/yyyy:HH:mm:ss Z").withLocale(Locale.US);
  private SummaryStats summaryStats;

  public Summary(final Statistics stats, final long timestampStart, final long timestampFinish) {
    checkNotNull(stats);
    checkArgument(timestampStart >= 0, "timestampStart must be >= 0 [%s]", timestampStart);
    checkArgument(timestampStart <= timestampFinish,
        "timestampStart must be <= timestampFinish [%s, %s]", timestampStart, timestampFinish);
    this.summaryStats = new SummaryStats(stats, timestampStart, timestampFinish);
  }

  static class SummaryStats {
    final long timestampStart;
    final long timestampFinish;
    final double runtime;
    final long operations;
    final OperationStats write;
    final OperationStats read;
    final OperationStats delete;

    private SummaryStats(Statistics stats, final long timestampStart, final long timestampFinish) {
      this.timestampStart = timestampStart;
      this.timestampFinish = timestampFinish;
      this.runtime = ((double) (timestampFinish - timestampStart)) / TimeUnit.SECONDS.toMillis(1);
      this.operations = stats.get(Operation.ALL, Counter.OPERATIONS);
      this.write = new OperationStats(stats, Operation.WRITE);
      this.read = new OperationStats(stats, Operation.READ);
      this.delete = new OperationStats(stats, Operation.DELETE);
    }

    @Override
    public String toString() {
      final String format = "Start: %s%nEnd: %s%nRuntime: %.2f Seconds%nOperations: %s%n%n%s%s%s";
      return String.format(Locale.US, format, FORMATTER.print(this.timestampStart),
          FORMATTER.print(this.timestampFinish), this.runtime, this.operations, this.write,
          this.read, this.delete);
    }
  }

  static class OperationStats {
    final transient Operation operation;
    final long operations;
    final long bytes;
    final Map<Integer, Long> statusCodes;

    private OperationStats(Statistics stats, Operation operation) {
      this.operation = operation;
      this.operations = stats.get(operation, Counter.OPERATIONS);
      this.bytes = stats.get(operation, Counter.BYTES);
      this.statusCodes = ImmutableSortedMap.copyOf(stats.statusCodes(operation));
    }

    @Override
    public String toString() {
      return String.format("[%s]%nOperations: %s%nBytes: %s%nStatus Codes:%n%s%n", this.operation,
          this.operations, this.bytes, formatStatusCodes());
    }

    private String formatStatusCodes() {
      if (this.statusCodes.isEmpty())
        return String.format("N/A%n");

      final StringBuilder s = new StringBuilder();
      for (final Entry<Integer, Long> sc : this.statusCodes.entrySet()) {
        s.append(String.format("%s: %s%n", sc.getKey(), sc.getValue()));
      }
      return s.toString();
    }
  }

  public SummaryStats getSummaryStats() {
    return this.summaryStats;
  }

  @Override
  public String toString() {
    return this.summaryStats.toString();
  }
}
