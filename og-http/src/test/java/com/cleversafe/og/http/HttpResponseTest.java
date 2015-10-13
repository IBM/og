/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.DataType;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class HttpResponseTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private int statusCode;

  @Before
  public void before() {
    this.statusCode = 201;
  }

  @Test(expected = IllegalArgumentException.class)
  public void noStatusCode() {
    new HttpResponse.Builder().build();
  }

  @DataProvider
  public static Object[][] provideInvalidStatusCode() {
    return new Object[][] {{-1}, {0}, {99}, {600}};
  }

  @Test
  @UseDataProvider("provideInvalidStatusCode")
  public void negativeStatusCode(final int statusCode) {
    this.thrown.expect(IllegalArgumentException.class);
    new HttpResponse.Builder().withStatusCode(statusCode).build();
  }

  @Test(expected = NullPointerException.class)
  public void nullKey() {
    new HttpResponse.Builder().withStatusCode(this.statusCode).withHeader(null, "value").build();
  }

  @Test(expected = NullPointerException.class)
  public void nullValue() {
    new HttpResponse.Builder().withStatusCode(this.statusCode).withHeader("key", null).build();
  }

  @Test(expected = NullPointerException.class)
  public void nullBody() {
    new HttpResponse.Builder().withStatusCode(this.statusCode).withBody(null).build();
  }

  @Test
  public void statusCode() {
    final HttpResponse response = new HttpResponse.Builder().withStatusCode(404).build();
    assertThat(response.getStatusCode(), is(404));
  }

  @Test
  public void noHeaders() {
    final HttpResponse response =
        new HttpResponse.Builder().withStatusCode(this.statusCode).build();
    assertThat(response.headers().size(), is(0));
  }

  @Test
  public void oneHeader() {
    final HttpResponse response = new HttpResponse.Builder().withStatusCode(this.statusCode)
        .withHeader("key", "value").build();
    assertThat(response.headers().size(), is(1));
    assertThat(response.headers(), hasEntry("key", "value"));
  }

  @Test
  public void multipleHeaders() {
    final HttpResponse.Builder b = new HttpResponse.Builder().withStatusCode(this.statusCode);
    for (int i = 0; i < 10; i++) {
      // (10 - i) exposes sorted vs insertion order
      b.withHeader("key" + (10 - i), "value");
    }
    final HttpResponse response = b.build();
    assertThat(response.headers().size(), is(10));
    for (int i = 0; i < 10; i++) {
      assertThat(response.headers(), hasEntry("key" + (10 - i), "value"));
    }
  }

  @Test
  public void headerModification() {
    final HttpResponse.Builder b =
        new HttpResponse.Builder().withStatusCode(this.statusCode).withHeader("key1", "value1");
    final HttpResponse response = b.build();
    b.withHeader("key2", "value2");
    assertThat(response.headers(), hasEntry("key1", "value1"));
    assertThat(response.headers(), not(hasEntry("key2", "value2")));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testHeaderIteratorRemove() {
    new HttpResponse.Builder().withStatusCode(this.statusCode).withHeader("key", "value").build()
        .headers().remove("key");
  }

  @Test
  public void defaultBody() {
    final Body body = new HttpResponse.Builder().withStatusCode(this.statusCode).build().getBody();
    assertThat(body.getDataType(), is(DataType.NONE));
    assertThat(body.getSize(), is(0L));
  }

  @Test
  public void body() {
    final Body body = new HttpResponse.Builder().withStatusCode(this.statusCode)
        .withBody(Bodies.zeroes(12345)).build().getBody();
    assertThat(body.getDataType(), is(DataType.ZEROES));
    assertThat(body.getSize(), is(12345L));
  }
}
