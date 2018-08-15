/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.ibm.og.statistic.Counter;
import com.ibm.og.statistic.Statistics;
import com.ibm.og.api.Operation;
import com.ibm.og.util.Pair;
import com.ibm.og.util.SizeUnit;
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
  public Summary(final Statistics stats, final long timestampStart, final long timestampFinish,
                 final int exitCode, ImmutableList<String> messages) {
    checkNotNull(stats);
    checkArgument(timestampStart >= 0, "timestampStart must be >= 0 [%s]", timestampStart);
    checkArgument(timestampStart <= timestampFinish,
        "timestampStart must be <= timestampFinish [%s, %s]", timestampStart, timestampFinish);
    this.summaryStats = new SummaryStats(stats, timestampStart, timestampFinish, exitCode, messages);
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
    final OperationStats multipartWriteInitiate;
    final OperationStats multipartWritePart;
    final OperationStats multipartWriteComplete;
    final OperationStats multipartWriteAbort;
    final OperationStats writeCopy;
    final OperationStats writeLegalHold;
    final OperationStats readLegalHold;
    final OperationStats deleteLegalHold;
    final OperationStats extendRetention;
    final OperationStats objectRestore;
    final OperationStats putContainerLifecycle;
    final OperationStats getContainerLifecycle;
    final OperationStats putContainerProtection;
    final OperationStats getContainerProtection;
    final int exitCode;
    final ImmutableList<String> exitMessages;

    private SummaryStats(final Statistics stats, final long timestampStart,
        final long timestampFinish, final int exitCode, final ImmutableList<String> messages) {
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
      this.multipartWriteInitiate = new OperationStats(stats, Operation.MULTIPART_WRITE_INITIATE);
      this.multipartWritePart = new OperationStats(stats, Operation.MULTIPART_WRITE_PART);
      this.multipartWriteComplete = new OperationStats(stats, Operation.MULTIPART_WRITE_COMPLETE);
      this.multipartWriteAbort = new OperationStats(stats, Operation.MULTIPART_WRITE_ABORT);
      this.writeCopy = new OperationStats(stats, Operation.WRITE_COPY);
      this.writeLegalHold = new OperationStats(stats, Operation.WRITE_LEGAL_HOLD);
      this.readLegalHold = new OperationStats(stats, Operation.READ_LEGAL_HOLD);
      this.deleteLegalHold = new OperationStats(stats, Operation.DELETE_LEGAL_HOLD);
      this.extendRetention = new OperationStats(stats, Operation.EXTEND_RETENTION);
      this.objectRestore = new OperationStats(stats, Operation.OBJECT_RESTORE);
      this.putContainerLifecycle = new OperationStats(stats, Operation.PUT_CONTAINER_LIFECYCLE);
      this.getContainerLifecycle = new OperationStats(stats, Operation.GET_CONTAINER_LIFECYCLE);
      this.putContainerProtection = new OperationStats(stats, Operation.PUT_CONTAINER_PROTECTION);
      this.getContainerProtection = new OperationStats(stats, Operation.GET_CONTAINER_PROTECTION);
      this.exitCode = exitCode;
      this.exitMessages = messages;
    }

    @Override
    public String toString() {
      final String format = "Start: %s%nEnd: %s%nRuntime: %.2f "
          + "Seconds%nOperations: %s%n%n%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%sExitCode: %s%nExitMessages:%s";
      return String.format(Locale.US, format, FORMATTER.print(this.timestampStart),
          FORMATTER.print(this.timestampFinish), this.runtime, this.operations, this.write,
          this.read, this.delete, this.metadata, this.overwrite, this.list, this.containerList,
          this.containerCreate, this.multipartWriteInitiate, this.multipartWritePart, this.multipartWriteComplete,
          this.multipartWriteAbort,this.writeCopy, this.writeLegalHold, this.readLegalHold, this.deleteLegalHold,
          this.extendRetention, this.objectRestore, this.putContainerLifecycle, this.getContainerLifecycle, this.exitCode,
              prettyExitMessages());
    }

    public String condensedStats() {
      StringBuilder sb = new StringBuilder();
      sb.append("Start: ").append(this.timestampStart).append("\n");
      sb.append("End: ").append(this.timestampFinish).append("\n");
      sb.append("Runtime: ").append(this.runtime).append("\n");
      sb.append("Operations: ").append(this.operations).append("\n\n");

      if (this.write.operations > 0) {
        sb.append(this.write).append("\n");
      }
      if (this.read.operations > 0) {
        sb.append(this.read).append("\n");
      }
      if (this.delete.operations > 0) {
        sb.append(this.delete).append("\n");
      }
      if (this.metadata.operations > 0) {
        sb.append(this.metadata).append("\n");
      }
      if (this.overwrite.operations > 0) {
        sb.append(this.overwrite).append("\n");
      }
      if (this.list.operations > 0) {
        sb.append(this.list).append("\n");
      }
      if (this.containerList.operations > 0) {
        sb.append(this.containerList).append("\n");
      }
      if (this.containerCreate.operations > 0) {
        sb.append(this.containerCreate).append("\n");
      }
      if (this.multipartWriteInitiate.operations > 0) {
        sb.append(this.multipartWriteInitiate).append("\n");
      }
      if (this.multipartWritePart.operations > 0) {
        sb.append(this.multipartWritePart).append("\n");
      }
      if (this.multipartWriteComplete.operations > 0) {
        sb.append(this.multipartWriteComplete).append("\n");
      }
      if (this.multipartWriteAbort.operations > 0) {
        sb.append(this.multipartWriteAbort).append("\n");
      }
      if (this.writeCopy.operations > 0) {
        sb.append(this.writeCopy).append("\n");
      }
      if (this.writeLegalHold.operations > 0) {
        sb.append(this.writeLegalHold).append("\n");
      }
      if (this.readLegalHold.operations > 0) {
        sb.append(this.readLegalHold).append("\n");
      }
      if (this.deleteLegalHold.operations > 0) {
        sb.append(this.deleteLegalHold).append("\n");
      }
      if (this.extendRetention.operations > 0) {
        sb.append(this.extendRetention).append("\n");
      }
      if (this.objectRestore.operations > 0) {
        sb.append(this.objectRestore).append("\n");
      }
      if (this.putContainerLifecycle.operations > 0) {
        sb.append(this.putContainerLifecycle).append("\n");
      }
      if (this.getContainerLifecycle.operations > 0) {
        sb.append(this.getContainerLifecycle).append("\n");
      }
      if (this.putContainerProtection.operations > 0) {
        sb.append(this.putContainerProtection).append("\n");
      }
      if (this.getContainerProtection.operations > 0) {
        sb.append(this.getContainerProtection).append("\n");
      }
      return sb.toString();
    }

    public String condensedSummary() {

      StringBuilder sb = new StringBuilder(condensedStats());
      sb.append("ExitCode: ").append(this.exitCode).append("\n");
      sb.append("ExitMessages:").append(prettyExitMessages());

      return sb.toString();
    }

    class OperationStats {
      final transient Operation operation;
      final long operations;
      final long bytes;
      final long latencies;
      final Map<Integer, Long> statusCodes;


      private OperationStats(final Statistics stats, final Operation operation) {
        this.operation = operation;
        this.operations = stats.get(operation, Counter.OPERATIONS);
        this.bytes = stats.get(operation, Counter.BYTES);
        this.latencies = stats.get(operation, Counter.LATENCY);
        this.statusCodes = ImmutableSortedMap.copyOf(stats.statusCodes(operation));
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

      private String formatAverageLatency() {
        double averageLatency = 0.0;
        if (this.operations > 0) {
          averageLatency = this.latencies / this.operations;
        }
        return String.format("Avg Latency: %.2f %s", averageLatency, "ms");
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
    private String prettyExitMessages() {
      StringBuilder sb = new StringBuilder();
      if (exitMessages != null) {
        for (String s : exitMessages) {
          sb.append(String.format("%n%s", s));
        }
      }
      return sb.toString();
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
