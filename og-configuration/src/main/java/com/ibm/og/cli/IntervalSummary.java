/* Copyright (c) IBM Corporation 2018. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.cli;


import com.ibm.og.api.Operation;
import com.ibm.og.statistic.Counter;
import com.ibm.og.statistic.Statistics;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class IntervalSummary {
  private static final DateTimeFormatter FORMATTER =
          DateTimeFormat.forPattern("dd/MMM/yyyy:HH:mm:ss Z").withLocale(Locale.US);
  private Summary.SummaryOperationStats prevStats;

  public IntervalSummary(Statistics stats, final long timestampStart, final long timestampFinish) {
    checkNotNull(stats);
    checkArgument(timestampStart >= 0, "timestampStart must be >= 0 [%s]", timestampStart);
    checkArgument(timestampStart <= timestampFinish,
            "timestampStart must be <= timestampFinish [%s, %s]", timestampStart, timestampFinish);
    this.prevStats = new Summary.SummaryOperationStats(stats, timestampStart, timestampFinish);
  }

  public Summary.SummaryOperationStats intervalStats(final Statistics stats, final long timestampStart,
                                                     final long timestampFinish) {
    Summary.SummaryOperationStats intervalStats = new Summary.SummaryOperationStats(timestampStart, timestampFinish);
    for(Operation operation: Operation.values()) {
      if (operation != Operation.ALL && operation != Operation.MULTIPART_WRITE) {
//        if (stats.get(operation, Counter.OPERATIONS) > 0) {
          addOperationIntervalStats(operation, stats, intervalStats, timestampStart, timestampFinish);
        }
//      }
    }

    return  intervalStats;

  }

  private Summary.SummaryOperationStats addOperationIntervalStats(final Operation operation, final Statistics stats,
                                                                  Summary.SummaryOperationStats intervalStats,
                                                                  final long timestampStart,
                                                                  final long timestampFinish) {
    // diff new stats - previous stats and return result
    OperationStats os = diffOperationStats(stats, operation, timestampStart, timestampFinish);
    intervalStats.setOperation(os);
    return intervalStats;

  }

  public OperationStats diffOperationStats(Statistics stats, Operation operation, long timestampStart,
                                           long timestampFinish) {
    OperationStats currentOperationStats = new OperationStats(stats, operation, timestampStart, timestampFinish);
    OperationStats lastOperationStats = this.prevStats.getOperation(operation);
    long operations = currentOperationStats.operations - lastOperationStats.operations;
    long bytes = currentOperationStats.bytes - lastOperationStats.bytes;
    long latencies = currentOperationStats.latencies - lastOperationStats.latencies;
    final Map<Integer, Long> statusCodes = new HashMap<Integer, Long>();
    for (Map.Entry<Integer, Long> entry : currentOperationStats.statusCodes.entrySet()) {
      if (currentOperationStats.statusCodes.get(entry.getKey()) != null &&
              lastOperationStats.statusCodes.get(entry.getKey()) != null) {
        statusCodes.put(entry.getKey(), currentOperationStats.statusCodes.get(entry.getKey()) -
                lastOperationStats.statusCodes.get(entry.getKey()));
      } else {
        statusCodes.put(entry.getKey(), currentOperationStats.statusCodes.get(entry.getKey()));
      }
    }

    OperationStats operationIntervalStat = new OperationStats(operation, operations, bytes, latencies,
            statusCodes, timestampFinish - timestampStart);
    this.prevStats.setOperation(currentOperationStats);
    return operationIntervalStat;
  }


}
