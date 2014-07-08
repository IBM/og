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
// Date: Jul 7, 2014
// ---------------------

package com.cleversafe.og.client;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.util.consumer.ByteBufferConsumer;
import com.google.common.base.Function;

public class ApacheClientTest
{
   private Function<String, ByteBufferConsumer> byteBufferConsumers;

   // TODO @Mock annotation?
   @SuppressWarnings("unchecked")
   @Before()
   public void setupBefore()
   {
      this.byteBufferConsumers = mock(Function.class);
   }

   @Test(expected = NullPointerException.class)
   public void testNullByteBufferConsumers()
   {
      new ApacheClient.Builder(null).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeConnectTimeout()
   {
      new ApacheClient.Builder(this.byteBufferConsumers).withConnectTimeout(-1).build();
   }

   @Test
   public void testZeroConnectTimeout()
   {
      new ApacheClient.Builder(this.byteBufferConsumers).withConnectTimeout(0).build();
   }

   @Test
   public void testPositiveConnectTimeout()
   {
      new ApacheClient.Builder(this.byteBufferConsumers).withConnectTimeout(1).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeSoTimeout()
   {
      new ApacheClient.Builder(this.byteBufferConsumers).withSoTimeout(-1).build();
   }

   @Test
   public void testZeroSoTimeout()
   {
      new ApacheClient.Builder(this.byteBufferConsumers).withSoTimeout(0).build();
   }

   @Test
   public void testPositiveSoTimeout()
   {
      new ApacheClient.Builder(this.byteBufferConsumers).withSoTimeout(1).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeSoLinger()
   {
      new ApacheClient.Builder(this.byteBufferConsumers).withSoLinger(-2).build();
   }

   @Test
   public void testNegativeSoLinger2()
   {
      new ApacheClient.Builder(this.byteBufferConsumers).withSoLinger(-1).build();
   }

   @Test
   public void testZeroSoLinger()
   {
      new ApacheClient.Builder(this.byteBufferConsumers).withSoLinger(0).build();
   }

   @Test
   public void testPositiveSoLinger()
   {
      new ApacheClient.Builder(this.byteBufferConsumers).withSoLinger(1).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeWaitForContinue()
   {
      new ApacheClient.Builder(this.byteBufferConsumers).withWaitForContinue(-1).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testZeroWaitForContinue()
   {
      new ApacheClient.Builder(this.byteBufferConsumers).withWaitForContinue(0).build();
   }

   @Test
   public void testPositiveWaitForContinue()
   {
      new ApacheClient.Builder(this.byteBufferConsumers).withWaitForContinue(1).build();
   }
}
