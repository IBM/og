/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.ibm.og.api.RequestTimestamps;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.ibm.og.api.Method;
import com.ibm.og.api.Request;
import com.ibm.og.api.Response;
import com.ibm.og.http.Bodies;
import com.ibm.og.http.HttpRequest;
import com.ibm.og.http.HttpResponse;
import com.ibm.og.statistic.Statistics;
import com.ibm.og.api.Operation;
import com.ibm.og.util.Pair;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class SummaryTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @DataProvider
  public static Object[][] provideInvalidSummary() {
    final Statistics stats = new Statistics();
    return new Object[][] {{null, 0, 0, NullPointerException.class, 1, ImmutableList.of("Invalid Input")},
        {stats, -1, 0, IllegalArgumentException.class, 1, ImmutableList.of("Invalid Input")},
        {stats, 0, -1, IllegalArgumentException.class, 1, ImmutableList.of("Invalid Input")},
        {stats, 1, 0, IllegalArgumentException.class, 1, ImmutableList.of("Invalid Input")}};
  }

  @Test
  @UseDataProvider("provideInvalidSummary")
  public void invalidSummary(final Statistics stats, final long timestampStart,
      final long timestampFinish, final Class<Exception> expectedException, final int exitCode, ImmutableList<String> messages) {
    this.thrown.expect(expectedException);
    new Summary(stats, timestampStart, timestampFinish, exitCode, messages);
  }

  @Test
  public void summary() throws URISyntaxException {
    final Statistics stats = new Statistics();
    final Request request =
        new HttpRequest.Builder(Method.GET, new URI("http://127.0.0.1"), Operation.READ).build();
    RequestTimestamps timestamps = new RequestTimestamps();
    timestamps.startMillis = System.currentTimeMillis();
    timestamps.finishMillis = timestamps.startMillis + 17;
    final Response response =
        new HttpResponse.Builder().withStatusCode(200).withBody(Bodies.zeroes(1024)).withRequestTimestamps(timestamps)
                .build();
    stats.update(Pair.of(request, response));
    final long timestampStart = System.nanoTime();
    final long timestampFinish = timestampStart + 100;
    final double runtime =
        ((double) (timestampFinish - timestampStart)) / TimeUnit.SECONDS.toMillis(1);
    final Summary summary = new Summary(stats, timestampStart, timestampFinish, 0, ImmutableList.of("Test Success"));
    // can't do much to validate toString correctness, but at least execute it
    summary.toString();
    final Summary.SummaryStats summaryStats = summary.getSummaryStats();

    assertThat(summaryStats.timestampStart, is(timestampStart));
    assertThat(summaryStats.timestampFinish, is(timestampFinish));
    assertThat(summaryStats.runtime, is(runtime));
    assertThat(summaryStats.operations, is(1L));

    assertThat(summaryStats.write.operation, is(Operation.WRITE));
    assertThat(summaryStats.write.operations, is(0L));
    assertThat(summaryStats.write.bytes, is(0L));
    assertThat(summaryStats.write.statusCodes.size(), is(0));

    assertThat(summaryStats.read.operation, is(Operation.READ));
    assertThat(summaryStats.read.operations, is(1L));
    assertThat(summaryStats.read.bytes, is(1024L));
    assertThat(summaryStats.read.statusCodes.size(), is(1));
    assertThat(summaryStats.read.statusCodes, hasEntry(200, 1L));

    assertThat(summaryStats.delete.operation, is(Operation.DELETE));
    assertThat(summaryStats.delete.operations, is(0L));
    assertThat(summaryStats.delete.bytes, is(0L));
    assertThat(summaryStats.delete.statusCodes.size(), is(0));

    assertThat(summaryStats.metadata.operation, is(Operation.METADATA));
    assertThat(summaryStats.metadata.operations, is(0L));
    assertThat(summaryStats.metadata.statusCodes.size(), is(0));
    assertThat(summaryStats.metadata.statusCodes.size(), is(0));

    assertThat(summaryStats.readLegalHold.operation, is(Operation.READ_LEGAL_HOLD));
    assertThat(summaryStats.readLegalHold.operations, is(0L));
    assertThat(summaryStats.readLegalHold.statusCodes.size(), is(0));
    assertThat(summaryStats.readLegalHold.statusCodes.size(), is(0));

    assertThat(summaryStats.writeLegalHold.operation, is(Operation.WRITE_LEGAL_HOLD));
    assertThat(summaryStats.writeLegalHold.operations, is(0L));
    assertThat(summaryStats.writeLegalHold.statusCodes.size(), is(0));
    assertThat(summaryStats.writeLegalHold.statusCodes.size(), is(0));

    assertThat(summaryStats.writeLegalHold.operation, is(Operation.WRITE_LEGAL_HOLD));
    assertThat(summaryStats.writeLegalHold.operations, is(0L));
    assertThat(summaryStats.writeLegalHold.statusCodes.size(), is(0));
    assertThat(summaryStats.writeLegalHold.statusCodes.size(), is(0));

  }
}
