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
// Date: Jul 9, 2014
// ---------------------

package com.cleversafe.og.test.operation.manager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.operation.manager.OperationManagerException;
import com.cleversafe.og.util.consumer.Consumer;
import com.cleversafe.og.util.producer.Producer;
import com.cleversafe.og.util.producer.ProducerException;
import com.cleversafe.og.util.producer.Producers;

public class SimpleOperationManagerTest
{
   private Producer<Producer<Request>> requestMix;
   private List<Consumer<Response>> consumers;
   private Request request;

   @Before
   @SuppressWarnings("unchecked")
   public void setBefore()
   {
      this.requestMix = mock(Producer.class);
      this.consumers = Collections.emptyList();
      this.request = mock(Request.class);
      when(this.requestMix.produce()).thenReturn(Producers.of(this.request));
   }

   @Test(expected = NullPointerException.class)
   public void testNullRequestMix()
   {
      new SimpleOperationManager(null, this.consumers);
   }

   @Test(expected = NullPointerException.class)
   public void testNullConsumersList()
   {
      new SimpleOperationManager(this.requestMix, null);
   }

   @Test
   public void testProduce() throws OperationManagerException
   {
      final SimpleOperationManager m = new SimpleOperationManager(this.requestMix, this.consumers);
      final Request r = m.next();
      Assert.assertEquals(r, this.request);
   }

   @Test(expected = OperationManagerException.class)
   public void testProduceOperationManagerException() throws OperationManagerException
   {
      when(this.requestMix.produce()).thenThrow(new ProducerException());
      final SimpleOperationManager m = new SimpleOperationManager(this.requestMix, this.consumers);
      m.next();
   }

   @Test(expected = OperationManagerException.class)
   public void testProduceOperationManagerException2() throws OperationManagerException
   {
      @SuppressWarnings("unchecked")
      final Producer<Request> p = mock(Producer.class);
      when(p.produce()).thenThrow(new ProducerException());
      when(this.requestMix.produce()).thenReturn(p);
      final SimpleOperationManager m = new SimpleOperationManager(this.requestMix, this.consumers);
      m.next();
   }
}
