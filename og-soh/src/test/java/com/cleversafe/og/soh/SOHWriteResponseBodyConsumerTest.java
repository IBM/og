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
// Date: Jul 15, 2014
// ---------------------

package com.cleversafe.og.soh;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Test;

import com.cleversafe.og.http.Headers;
import com.google.common.base.Charsets;

public class SOHWriteResponseBodyConsumerTest
{
   @Test(expected = NullPointerException.class)
   public void nullInputStream() throws IOException
   {
      new SOHWriteResponseBodyConsumer().consume(201, null);
   }

   @Test
   public void invalidStatusCode() throws IOException
   {
      final SOHWriteResponseBodyConsumer consumer = new SOHWriteResponseBodyConsumer();
      final InputStream in = mock(InputStream.class);
      final Iterator<Entry<String, String>> it = consumer.consume(500, in);
      assertThat(it.hasNext(), is(false));
   }

   @Test
   public void consume() throws IOException
   {
      final SOHWriteResponseBodyConsumer consumer = new SOHWriteResponseBodyConsumer();
      final StringBuilder s = new StringBuilder();
      for (int i = 0; i < 10000; i++)
      {
         s.append("objectName").append(i).append("\n");
      }
      final InputStream in = new ByteArrayInputStream(s.toString().getBytes(Charsets.UTF_8));
      final Iterator<Entry<String, String>> it = consumer.consume(201, in);

      assertThat(it.hasNext(), is(true));

      final Entry<String, String> e = it.next();

      assertThat(e.getKey(), is(Headers.OBJECT_NAME.toString()));
      assertThat(e.getValue(), is("objectName0"));
      assertThat(it.hasNext(), is(false));
      assertThat(in.available(), is(0));
   }
}
