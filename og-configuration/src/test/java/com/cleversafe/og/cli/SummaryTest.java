/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.cli.Summary.SummaryStats;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.HttpRequest;
import com.cleversafe.og.http.HttpResponse;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.util.Operation;
import com.cleversafe.og.util.Pair;
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
    return new Object[][] {{null, 0, 0, NullPointerException.class},
        {stats, -1, 0, IllegalArgumentException.class},
        {stats, 0, -1, IllegalArgumentException.class},
        {stats, 1, 0, IllegalArgumentException.class}};
  }

  @Test
  @UseDataProvider("provideInvalidSummary")
  public void invalidSummary(final Statistics stats, final long timestampStart,
      final long timestampFinish, final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    new Summary(stats, timestampStart, timestampFinish);
  }

  @Test
  public void summary() throws URISyntaxException {
    final Statistics stats = new Statistics();
    final Request request =
        new HttpRequest.Builder(Method.GET, new URI("http://127.0.0.1")).build();
    final Response response =
        new HttpResponse.Builder().withStatusCode(200).withBody(Bodies.zeroes(1024)).build();
    stats.update(Pair.of(request, response));
    final long timestampStart = System.nanoTime();
    final long timestampFinish = timestampStart + 100;
    final double runtime =
        ((double) (timestampFinish - timestampStart)) / TimeUnit.SECONDS.toMillis(1);
    final Summary summary = new Summary(stats, timestampStart, timestampFinish);
    // can't do much to validate toString correctness, but at least execute it
    summary.toString();
    final SummaryStats summaryStats = summary.getSummaryStats();

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
  }
}
