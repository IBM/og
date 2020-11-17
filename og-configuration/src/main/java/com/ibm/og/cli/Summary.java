/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.ibm.og.statistic.Counter;
import com.ibm.og.statistic.Statistics;
import com.ibm.og.api.Operation;


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
                 final int exitCode, ImmutableList<String> messages, final int requestsAborted) {
    checkNotNull(stats);
    checkArgument(timestampStart >= 0, "timestampStart must be >= 0 [%s]", timestampStart);
    checkArgument(timestampStart <= timestampFinish,
        "timestampStart must be <= timestampFinish [%s, %s]", timestampStart, timestampFinish);
    this.summaryStats = new SummaryStats(stats, timestampStart, timestampFinish, exitCode, messages, requestsAborted);
  }


  static class SummaryOperationStats {
    long timestampStart;
    long timestampFinish;
    double runtime;
    long operations;
    OperationStats write;
    OperationStats read;
    OperationStats delete;
    OperationStats metadata;
    OperationStats overwrite;
    OperationStats list;
    OperationStats containerList;
    OperationStats containerCreate;
    OperationStats multipartWriteInitiate;
    OperationStats multipartWritePart;
    OperationStats multipartWriteComplete;
    OperationStats multipartWriteAbort;
    OperationStats writeCopy;
    OperationStats writeLegalHold;
    OperationStats readLegalHold;
    OperationStats deleteLegalHold;
    OperationStats extendRetention;
    OperationStats objectRestore;
    OperationStats putContainerLifecycle;
    OperationStats getContainerLifecycle;
    OperationStats deleteContainerLifecycle;
    OperationStats putContainerProtection;
    OperationStats getContainerProtection;
    OperationStats multidelete;
    OperationStats writeObjectTags;
    OperationStats deleteObjectTags;
    OperationStats getObjectTags;

    protected SummaryOperationStats(final long timestampStart, final long timestampFinish) {
      this.timestampStart = timestampStart;
      this.timestampFinish = timestampFinish;
      this.runtime = ((double) (timestampFinish - timestampStart)) / TimeUnit.SECONDS.toMillis(1);
    }

    protected SummaryOperationStats(final Statistics stats, final long timestampStart,
                                    final long timestampFinish) {
      this.timestampStart = timestampStart;
      this.timestampFinish = timestampFinish;
      this.runtime = ((double) (timestampFinish - timestampStart)) / TimeUnit.SECONDS.toMillis(1);
      this.operations = stats.get(Operation.ALL, Counter.OPERATIONS);
      this.write = new OperationStats(stats, Operation.WRITE, timestampStart, timestampFinish);
      this.read = new OperationStats(stats, Operation.READ, timestampStart, timestampFinish);
      this.delete = new OperationStats(stats, Operation.DELETE, timestampStart, timestampFinish);
      this.metadata = new OperationStats(stats, Operation.METADATA, timestampStart, timestampFinish);
      this.overwrite = new OperationStats(stats, Operation.OVERWRITE, timestampStart, timestampFinish);
      this.list = new OperationStats(stats, Operation.LIST, timestampStart, timestampFinish);
      this.containerList = new OperationStats(stats, Operation.CONTAINER_LIST, timestampStart, timestampFinish);
      this.containerCreate = new OperationStats(stats, Operation.CONTAINER_CREATE, timestampStart, timestampFinish);
      this.multipartWriteInitiate = new OperationStats(stats, Operation.MULTIPART_WRITE_INITIATE, timestampStart, timestampFinish);
      this.multipartWritePart = new OperationStats(stats, Operation.MULTIPART_WRITE_PART, timestampStart, timestampFinish);
      this.multipartWriteComplete = new OperationStats(stats, Operation.MULTIPART_WRITE_COMPLETE, timestampStart, timestampFinish);
      this.multipartWriteAbort = new OperationStats(stats, Operation.MULTIPART_WRITE_ABORT, timestampStart, timestampFinish);
      this.writeCopy = new OperationStats(stats, Operation.WRITE_COPY, timestampStart, timestampFinish);
      this.writeLegalHold = new OperationStats(stats, Operation.WRITE_LEGAL_HOLD, timestampStart, timestampFinish);
      this.readLegalHold = new OperationStats(stats, Operation.READ_LEGAL_HOLD, timestampStart, timestampFinish);
      this.deleteLegalHold = new OperationStats(stats, Operation.DELETE_LEGAL_HOLD, timestampStart, timestampFinish);
      this.extendRetention = new OperationStats(stats, Operation.EXTEND_RETENTION, timestampStart, timestampFinish);
      this.objectRestore = new OperationStats(stats, Operation.OBJECT_RESTORE, timestampStart, timestampFinish);
      this.putContainerLifecycle = new OperationStats(stats, Operation.PUT_CONTAINER_LIFECYCLE, timestampStart, timestampFinish);
      this.getContainerLifecycle = new OperationStats(stats, Operation.GET_CONTAINER_LIFECYCLE, timestampStart, timestampFinish);
      this.deleteContainerLifecycle = new OperationStats(stats, Operation.DELETE_CONTAINER_LIFECYCLE, timestampStart, timestampFinish);
      this.putContainerProtection = new OperationStats(stats, Operation.PUT_CONTAINER_PROTECTION, timestampStart, timestampFinish);
      this.getContainerProtection = new OperationStats(stats, Operation.GET_CONTAINER_PROTECTION, timestampStart, timestampFinish);
      this.multidelete = new OperationStats(stats, Operation.MULTI_DELETE, timestampStart, timestampFinish);
      this.writeObjectTags = new OperationStats(stats, Operation.PUT_TAGS, timestampStart, timestampFinish);
      this.deleteObjectTags = new OperationStats(stats, Operation.DELETE_TAGS, timestampStart, timestampFinish);
      this.getObjectTags = new OperationStats(stats, Operation.GET_TAGS, timestampStart, timestampFinish);
    }

    public OperationStats getOperation(Operation operation) {
      if (operation == Operation.WRITE) {
        return this.write;
      } else if (operation == Operation.READ) {
        return this.read;
      } else if (operation == Operation.DELETE) {
        return this.delete;
      } else if (operation == Operation.METADATA) {
        return this.metadata;
      } else if (operation == Operation.OVERWRITE) {
        return this.overwrite;
      } else if (operation == Operation.LIST) {
        return this.list;
      } else if (operation == Operation.CONTAINER_LIST) {
        return this.containerList;
      } else if (operation == Operation.CONTAINER_CREATE) {
        return this.containerCreate;
      } else if (operation == Operation.MULTIPART_WRITE_INITIATE) {
        return this.multipartWriteInitiate;
      } else if (operation == Operation.MULTIPART_WRITE_PART) {
        return this.multipartWritePart;
      } else if (operation == Operation.MULTIPART_WRITE_COMPLETE) {
        return this.multipartWriteComplete;
      } else if (operation == Operation.MULTIPART_WRITE_ABORT) {
        return this.multipartWriteAbort;
      } else if (operation == Operation.WRITE_COPY) {
        return this.writeCopy;
      } else if (operation == Operation.WRITE_LEGAL_HOLD) {
        return this.writeLegalHold;
      } else if (operation == Operation.READ_LEGAL_HOLD) {
        return this.readLegalHold;
      } else if (operation == Operation.DELETE_LEGAL_HOLD) {
        return this.deleteLegalHold;
      } else if (operation == Operation.EXTEND_RETENTION) {
        return this.extendRetention;
      } else if (operation == Operation.OBJECT_RESTORE) {
        return this.objectRestore;
      } else if (operation == Operation.PUT_CONTAINER_LIFECYCLE) {
        return this.putContainerLifecycle;
      } else if (operation == Operation.GET_CONTAINER_LIFECYCLE) {
        return this.getContainerLifecycle;
      } else if (operation == Operation.DELETE_CONTAINER_LIFECYCLE) {
        return this.deleteContainerLifecycle;
      } else if (operation == Operation.PUT_CONTAINER_PROTECTION) {
        return this.putContainerProtection;
      } else if (operation == Operation.GET_CONTAINER_PROTECTION) {
        return this.getContainerProtection;
      } else if (operation == Operation.MULTI_DELETE) {
        return this.multidelete;
      } else if (operation == Operation.PUT_TAGS) {
        return this.writeObjectTags;
      } else if (operation == Operation.DELETE_TAGS) {
        return this.deleteObjectTags;
      } else if (operation == Operation.GET_TAGS) {
        return this.getObjectTags;
      }
      return null;

    }

    public void setOperation(OperationStats operationStat) {
      Operation operation = operationStat.operation;
      this.operations += operationStat.operations;
      if (operation == Operation.WRITE) {
        this.write = operationStat;
      } else if (operation == Operation.READ) {
        this.read = operationStat;
      } else if (operation == Operation.DELETE) {
        this.delete = operationStat;
      } else if (operation == Operation.METADATA) {
        this.metadata = operationStat;
      } else if (operation == Operation.OVERWRITE) {
        this.overwrite = operationStat;
      } else if (operation == Operation.LIST) {
        this.list = operationStat;
      } else if (operation == Operation.CONTAINER_LIST) {
        this.containerList = operationStat;
      } else if (operation == Operation.CONTAINER_CREATE) {
        this.containerCreate = operationStat;
      } else if (operation == Operation.MULTIPART_WRITE_INITIATE) {
        this.multipartWriteInitiate = operationStat;
      } else if (operation == Operation.MULTIPART_WRITE_PART) {
        this.multipartWritePart = operationStat;
      } else if (operation == Operation.MULTIPART_WRITE_COMPLETE) {
        this.multipartWriteComplete = operationStat;
      } else if (operation == Operation.MULTIPART_WRITE_ABORT) {
        this.multipartWriteAbort = operationStat;
      } else if (operation == Operation.WRITE_COPY) {
        this.writeCopy = operationStat;
      } else if (operation == Operation.WRITE_LEGAL_HOLD) {
        this.writeLegalHold = operationStat;
      } else if (operation == Operation.READ_LEGAL_HOLD) {
        this.readLegalHold = operationStat;
      } else if (operation == Operation.DELETE_LEGAL_HOLD) {
        this.deleteLegalHold = operationStat;
      } else if (operation == Operation.EXTEND_RETENTION) {
        this.extendRetention = operationStat;
      } else if (operation == Operation.OBJECT_RESTORE) {
        this.objectRestore = operationStat;
      } else if (operation == Operation.PUT_CONTAINER_LIFECYCLE) {
        this.putContainerLifecycle = operationStat;
      } else if (operation == Operation.GET_CONTAINER_LIFECYCLE) {
        this.getContainerLifecycle = operationStat;
      } else if (operation == Operation.DELETE_CONTAINER_LIFECYCLE) {
        this.deleteContainerLifecycle = operationStat;
      } else if (operation == Operation.PUT_CONTAINER_PROTECTION) {
        this.putContainerProtection = operationStat;
      } else if (operation == Operation.GET_CONTAINER_PROTECTION) {
          this.getContainerProtection = operationStat;
      } else if (operation == Operation.MULTI_DELETE) {
        this.multidelete = operationStat;
      } else if (operation == Operation.PUT_TAGS) {
        this.writeObjectTags = operationStat;
      } else if (operation == Operation.DELETE_TAGS) {
        this.deleteObjectTags = operationStat;
      } else if (operation == Operation.GET_TAGS) {
        this.getObjectTags = operationStat;
      }
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
      if (this.deleteContainerLifecycle.operations > 0) {
        sb.append(this.deleteContainerLifecycle).append("\n");
      }
      if (this.putContainerProtection.operations > 0) {
        sb.append(this.putContainerProtection).append("\n");
      }
      if (this.getContainerProtection.operations > 0) {
        sb.append(this.getContainerProtection).append("\n");
      }
      if (this.multidelete.operations > 0) {
        sb.append(this.multidelete).append("\n");
      }
      if (this.writeObjectTags.operations > 0) {
        sb.append(this.writeObjectTags).append("\n");
      }
      if (this.deleteObjectTags.operations > 0) {
        sb.append(this.deleteObjectTags).append("\n");
      }
      if (this.getObjectTags.operations > 0) {
        sb.append(this.getObjectTags).append("\n");
      }
      return sb.toString();
    }

    @Override
    public String toString() {
      return condensedStats();
    }

  }

  static class SummaryStats extends SummaryOperationStats {

    final int exitCode;
    final ImmutableList<String> exitMessages;
    final int requestsAborted;

    SummaryStats(final Statistics stats, final long timestampStart,
                 final long timestampFinish, final int exitCode, final ImmutableList<String> messages,
                 final int requestsAborted) {
      super(stats, timestampStart, timestampFinish);

      this.exitCode = exitCode;
      this.exitMessages = messages;
      this.requestsAborted = requestsAborted;
    }

    public String condensedSummary() {

      StringBuilder sb = new StringBuilder(condensedStats());
      sb.append("RequestsAborted: ").append(this.requestsAborted).append("\n");
      sb.append("ExitCode: ").append(this.exitCode).append("\n");
      sb.append("ExitMessages:").append(prettyExitMessages());

      return sb.toString();
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



    @Override
    public String toString() {
      final String format = "Start: %s%nEnd: %s%nRuntime: %.2f "
              + "Seconds%nOperations: %s%n%n%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%sRequestsAborted: %s%nExitCode: %s%nExitMessages:%s";
      return String.format(Locale.US, format, FORMATTER.print(this.timestampStart),
              FORMATTER.print(this.timestampFinish), this.runtime, this.operations, this.write,
              this.read, this.delete, this.metadata, this.overwrite, this.list, this.containerList,
              this.containerCreate, this.multipartWriteInitiate, this.multipartWritePart, this.multipartWriteComplete,
              this.multipartWriteAbort,this.writeCopy, this.writeLegalHold, this.readLegalHold, this.deleteLegalHold,
              this.extendRetention, this.objectRestore, this.putContainerLifecycle, this.getContainerLifecycle,
              this.deleteContainerLifecycle, this.multidelete, this.writeObjectTags,this.deleteObjectTags,
              this.getObjectTags, this.requestsAborted, this.exitCode, prettyExitMessages());
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
