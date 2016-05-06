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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.cleversafe.og.statistic.Counter;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.api.Operation;
import com.cleversafe.og.util.Pair;
import com.cleversafe.og.util.SizeUnit;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

/**
 * A statistics summary block
 * 
 * @since 1.0
 */
public class Summary {
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormat.forPattern("dd/MMM/yyyy:HH:mm:ss Z").withLocale(Locale.US);
  private final SummaryStats summaryStats;

  /**
   * Constructs an instance
   * 
   * @param stats the underlying stats to pull from when creating this instance
   * @param timestampStart the global test start timestamp, in millis.
   * @param timestampFinish the global test stop timestamp, in millis
   * @throws NullPointerException if stats is null
   * @throws IllegalArgumentException if timestampStart is zero or negative, or if timestampEnd is
   *         less than timestampStart
   */
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
    final OperationStats metadata;
    final OperationStats overwrite;
    final OperationStats list;
    final OperationStats containerList;
    final OperationStats containerCreate;

    private SummaryStats(final Statistics stats, final long timestampStart,
        final long timestampFinish) {
      this.timestampStart = timestampStart;
      this.timestampFinish = timestampFinish;
      this.runtime = ((double) (timestampFinish - timestampStart)) / TimeUnit.SECONDS.toMillis(1);
      this.operations = stats.get(Operation.ALL, Counter.OPERATIONS);
      this.write = new OperationStats(stats, Operation.WRITE);
      this.read = new OperationStats(stats, Operation.READ);
      this.delete = new OperationStats(stats, Operation.DELETE);
      this.metadata = new OperationStats(stats, Operation.METADATA);
      this.overwrite = new OperationStats(stats, Operation.OVERWRITE);
      this.list = new OperationStats(stats, Operation.LIST);
      this.containerList = new OperationStats(stats, Operation.CONTAINER_LIST);
      this.containerCreate = new OperationStats(stats, Operation.CONTAINER_CREATE);
    }

    @Override
    public String toString() {
      final String format = "Start: %s%nEnd: %s%nRuntime: %.2f "
          + "Seconds%nOperations: %s%n%n%s%s%s%s%s%s%s%s";
      return String.format(Locale.US, format, FORMATTER.print(this.timestampStart),
          FORMATTER.print(this.timestampFinish), this.runtime, this.operations, this.write,
          this.read, this.delete, this.metadata, this.overwrite, this.list, this.containerList,
          this.containerCreate);
    }

    class OperationStats {
      final transient Operation operation;
      final long operations;
      final long bytes;
      final Map<Integer, Long> statusCodes;

      private OperationStats(final Statistics stats, final Operation operation) {
        this.operation = operation;
        this.operations = stats.get(operation, Counter.OPERATIONS);
        this.bytes = stats.get(operation, Counter.BYTES);
        this.statusCodes = ImmutableSortedMap.copyOf(stats.statusCodes(operation));
      }

      @Override
      public String toString() {
        return String.format(
            "[%s]%n" + "Operations: %s%n" + "%s%n" + "%s%n" + "%s%n" + "Status Codes:%n%s%n",
            this.operation, this.operations, formatBytes(), formatThroughput(), formatOPS(),
            formatStatusCodes());
      }

      // determine whether to display byte total in gb, mb, kb or bytes
      private String formatBytes() {
        final Pair<Double, SizeUnit> displaySize = displaySize(this.bytes);
        String displayUnit = displaySize.getValue().toString().toLowerCase();
        // capitalize
        displayUnit = displayUnit.substring(0, 1).toUpperCase() + displayUnit.substring(1);
        return String.format("%s: %.2f", displayUnit, displaySize.getKey());
      }

      private String formatThroughput() {
        final double bytesPerSecond = this.bytes / SummaryStats.this.runtime;
        final Pair<Double, SizeUnit> displaySize = displaySize(bytesPerSecond);
        String displayUnit;
        if (displaySize.getValue() != SizeUnit.BYTES) {
          displayUnit = String.format("%sB/s", displaySize.getValue().toString().substring(0, 1));
        } else {
          displayUnit = "B/s";
        }
        return String.format("Throughput: %.2f %s", displaySize.getKey(), displayUnit);
      }

      private String formatOPS() {
        final double operationsPerSecond = this.operations / SummaryStats.this.runtime;
        return String.format("OPS: %.2f", operationsPerSecond);
      }

      private String formatStatusCodes() {
        if (this.statusCodes.isEmpty()) {
          return String.format("N/A%n");
        }

        final StringBuilder s = new StringBuilder();
        for (final Entry<Integer, Long> sc : this.statusCodes.entrySet()) {
          s.append(String.format("%s: %s%n", sc.getKey(), sc.getValue()));
        }
        return s.toString();
      }

      private Pair<Double, SizeUnit> displaySize(final double bytes) {
        final List<SizeUnit> units = ImmutableList.of(SizeUnit.TERABYTES, SizeUnit.GIGABYTES,
            SizeUnit.MEGABYTES, SizeUnit.KILOBYTES);
        for (final SizeUnit unit : units) {
          final double size = bytes / unit.toBytes(1);
          if (size >= 1.0) {
            return Pair.of(size, unit);
          }
        }
        return Pair.of(bytes, SizeUnit.BYTES);
      }
    }
  }

  /**
   * Creates and returns a version of this summary suitable for serializing to json
   * 
   * @return a json serializable summary block
   */
  public SummaryStats getSummaryStats() {
    return this.summaryStats;
  }

  @Override
  public String toString() {
    return this.summaryStats.toString();
  }
}
