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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.statistic.Counter;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.util.Operation;

public class Summary
{
   private static Logger _logger = LoggerFactory.getLogger(Summary.class);
   private final Statistics stats;
   private SummaryStats summary;

   public Summary(final Statistics stats)
   {
      this.stats = checkNotNull(stats);
   }

   private static class SummaryStats
   {
      public final OperationStats all = new OperationStats();
      public final OperationStats write = new OperationStats();
      public final OperationStats read = new OperationStats();
      public final OperationStats delete = new OperationStats();
   }

   private static class OperationStats
   {
      public long operations = 0;
      public final Map<Integer, Long> statusCodes = new TreeMap<Integer, Long>();
      public long aborts = 0;
   }

   @Override
   public String toString()
   {
      retrieveStats();

      final String format = "\nAll\n%s\n\nWrite\n%s\n\nRead\n%s\n\nDelete\n%s";
      return String.format(Locale.US, format,
            getOperation(this.summary.all),
            getOperation(this.summary.write),
            getOperation(this.summary.read),
            getOperation(this.summary.delete));
   }

   private String getOperation(final OperationStats opStats)
   {
      final String format = "Operations: %s\nStatus Codes:\n%sAborts: %s";
      return String.format(Locale.US, format,
            opStats.operations,
            getStatusCodes(opStats),
            opStats.aborts);
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

      this.summary = new SummaryStats();
      retrieveStats(this.summary.all, Operation.ALL);
      retrieveStats(this.summary.write, Operation.WRITE);
      retrieveStats(this.summary.read, Operation.READ);
      retrieveStats(this.summary.delete, Operation.DELETE);
   }

   private void retrieveStats(final OperationStats opStats, final Operation operation)
   {
      opStats.operations = this.stats.get(operation, Counter.OPERATIONS);
      final Iterator<Entry<Integer, AtomicLong>> it = this.stats.statusCodeIterator(operation);
      while (it.hasNext())
      {
         final Entry<Integer, AtomicLong> sc = it.next();
         opStats.statusCodes.put(sc.getKey(), sc.getValue().get());
      }
      opStats.aborts = this.stats.get(operation, Counter.ABORTS);
   }
}
