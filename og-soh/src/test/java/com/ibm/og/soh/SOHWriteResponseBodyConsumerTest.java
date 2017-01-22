/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.soh;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.og.util.Context;
import org.hamcrest.Matchers;
import org.junit.Test;

import com.google.common.base.Charsets;

public class SOHWriteResponseBodyConsumerTest {
  @Test(expected = NullPointerException.class)
  public void nullInputStream() throws IOException {
    new SOHWriteResponseBodyConsumer().consume(201, null);
  }

  @Test
  public void invalidStatusCode() throws IOException {
    final SOHWriteResponseBodyConsumer consumer = new SOHWriteResponseBodyConsumer();
    final InputStream in = mock(InputStream.class);
    final Map<String, String> m = consumer.consume(500, in);
    assertThat(m.size(), is(0));
  }

  @Test
  public void consume() throws IOException {
    final SOHWriteResponseBodyConsumer consumer = new SOHWriteResponseBodyConsumer();
    final StringBuilder s = new StringBuilder();
    for (int i = 0; i < 10000; i++) {
      s.append("objectName").append(i).append("\n");
    }
    final InputStream in = new ByteArrayInputStream(s.toString().getBytes(Charsets.UTF_8));
    final Map<String, String> m = consumer.consume(201, in);

    assertThat(m.size(), is(1));

    final Entry<String, String> e = m.entrySet().iterator().next();

    assertThat(e.getKey(), Matchers.is(Context.X_OG_OBJECT_NAME));
    assertThat(e.getValue(), is("objectName0"));
    assertThat(in.available(), is(0));
  }
}
