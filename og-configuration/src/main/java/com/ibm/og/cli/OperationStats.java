/* Copyright (c) IBM Corporation 2018. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.cli;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.ibm.og.api.Operation;
import com.ibm.og.statistic.Counter;
import com.ibm.og.statistic.Statistics;
import com.ibm.og.util.Pair;
import com.ibm.og.util.SizeUnit;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class OperationStats {
  final transient Operation operation;
  final long operations;
  final long bytes;
  final transient long latencies;
  double averageLatency = 0.0;
  final Map<Integer, Long> statusCodes;
  transient double  runtime;

  public OperationStats(final Statistics stats, final Operation operation, long timestampStart, long timestampFinish) {
    this.operation = operation;
    this.operations = stats.get(operation, Counter.OPERATIONS);
    this.bytes = stats.get(operation, Counter.BYTES);
    this.latencies = stats.get(operation, Counter.LATENCY);
    this.statusCodes = ImmutableSortedMap.copyOf(stats.statusCodes(operation));
    this.runtime = ((double) (timestampFinish - timestampStart)) / TimeUnit.SECONDS.toMillis(1);
    if (this.operations > 0) {
      this.averageLatency = (double) this.latencies / this.operations;
    }
  }

  public OperationStats(final Operation operation, final long operations, final long bytes, final long latencies,
                        final Map<Integer, Long> statusCodes, long timestampStart, long timestampFinish) {
    this.operation = operation;
    this.operations = operations;
    this.bytes = bytes;
    this.latencies = latencies;
    this.statusCodes = ImmutableSortedMap.copyOf(statusCodes);
    if (this.operations > 0) {
      double average = (double) this.latencies / this.operations;
      this.averageLatency = Math.round(average * 100.00) / 100.00;
    }
    this.runtime = ((double) (timestampFinish - timestampStart)) / TimeUnit.SECONDS.toMillis(1);
  }

  @Override
  public String toString() {
    return String.format(
            "[%s]%n" + "Operations: %s%n" + "%s%n" + "%s%n" + "%s%n" + "%s%n" +  "Status Codes:%n%s%n",
            this.operation, this.operations, formatBytes(), formatThroughput(), formatOPS(), formatAverageLatency(),
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
    //TODO : fix this
    final double bytesPerSecond = (double)this.bytes / this.runtime;
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
    final double operationsPerSecond = (double) this.operations / this.runtime;
    return String.format("OPS: %.2f", operationsPerSecond);
  }

  private String formatAverageLatency() {
    double averageLatency = 0.0;
    if (this.operations > 0) {
      averageLatency = (double)this.latencies / this.operations;
    }
    return String.format("Avg Latency: %.2f %s", averageLatency, "ms");
  }

  private String formatStatusCodes() {
    if (this.statusCodes.isEmpty()) {
      return String.format("N/A%n");
    }

    final StringBuilder s = new StringBuilder();
    for (final Map.Entry<Integer, Long> sc : this.statusCodes.entrySet()) {
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
