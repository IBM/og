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

package com.cleversafe.og.cli.report;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.statistic.Counter;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.util.Operation;

public class Summary
{
   private static final Logger _logger = LoggerFactory.getLogger(Summary.class);
   private final Statistics stats;
   private SummaryStats summary;
   private final double runtime;

   public Summary(final Statistics stats, final long runtime)
   {
      this.stats = checkNotNull(stats);
      checkArgument(runtime >= 0, "runtime must be >= 0 [%s]", runtime);
      this.runtime = (double) runtime / TimeUnit.SECONDS.toNanos(1);
   }

   private static class SummaryStats
   {
      private final double runtime;
      private final OperationStats write = new OperationStats();
      private final OperationStats read = new OperationStats();
      private final OperationStats delete = new OperationStats();
      private final OperationStats all = new OperationStats();

      public SummaryStats(final double runtime)
      {
         this.runtime = runtime;
      }
   }

   private static class OperationStats
   {
      private long operations = 0;
      private final Map<Integer, Long> statusCodes = new TreeMap<Integer, Long>();
      private long aborts = 0;
   }

   @Override
   public String toString()
   {
      retrieveStats();

      return String.format(Locale.US, "Runtime: %.2f Seconds\n", this.runtime) +
            "Write: " + getOperation(this.summary.write) +
            "Read: " + getOperation(this.summary.read) +
            "Delete: " + getOperation(this.summary.delete) +
            "All: " + getOperation(this.summary.all);
   }

   private String getOperation(final OperationStats opStats)
   {
      final String format = "Operations=%s Bytes=0 Aborts=%s\nStatus Codes:\n%s\n";
      String statusCodes = getStatusCodes(opStats);
      if (statusCodes.length() == 0)
         statusCodes = "N/A\n";

      return String.format(Locale.US, format, opStats.operations, opStats.aborts, statusCodes);
   }

   private String getStatusCodes(final OperationStats opStats)
   {
      final StringBuilder s = new StringBuilder();
      for (final Entry<Integer, Long> sc : opStats.statusCodes.entrySet())
      {
         s.append(sc.getKey())
               .append(": ")
               .append(sc.getValue())
               .append("\n");
      }
      return s.toString();
   }

   public SummaryStats getSummaryStats()
   {
      retrieveStats();
      return this.summary;
   }

   private void retrieveStats()
   {
      if (this.summary != null)
         return;

      this.summary = new SummaryStats(this.runtime);
      retrieveStats(this.summary.write, Operation.WRITE);
      retrieveStats(this.summary.read, Operation.READ);
      retrieveStats(this.summary.delete, Operation.DELETE);
      retrieveStats(this.summary.all, Operation.ALL);
   }

   private void retrieveStats(final OperationStats opStats, final Operation operation)
   {
      opStats.operations = this.stats.get(operation, Counter.OPERATIONS);
      final Iterator<Entry<Integer, Long>> it = this.stats.statusCodeIterator(operation);
      while (it.hasNext())
      {
         final Entry<Integer, Long> sc = it.next();
         opStats.statusCodes.put(sc.getKey(), sc.getValue());
      }
      opStats.aborts = this.stats.get(operation, Counter.ABORTS);
   }
}
