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
// Date: Jun 29, 2014
// ---------------------

package com.cleversafe.og.object.consumer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.consumer.ObjectNameConsumer;
import com.cleversafe.og.consumer.WriteObjectNameConsumer;
import com.cleversafe.og.http.HttpUtil;
import com.cleversafe.og.object.ObjectManager;
import com.cleversafe.og.object.ObjectManagerException;
import com.cleversafe.og.object.ObjectName;
import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Method;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.util.Pair;
import com.google.common.collect.Lists;

public class WriteObjectNameConsumerTest
{
   private ObjectManager mockObjectManager;
   private List<Integer> statusCodes;
   private Request mockRequest;
   private Response mockResponse;

   @Before
   public void before() throws URISyntaxException
   {
      this.mockObjectManager = mock(ObjectManager.class);
      this.statusCodes = HttpUtil.SUCCESS_STATUS_CODES;
      this.mockRequest = mock(Request.class);
      when(this.mockRequest.getUri()).thenReturn(
            new URI("http://192.168.8.1/soh/container/5c18be1057404792923dc487ca40f2370000"));
      this.mockResponse = mock(Response.class);
      when(this.mockResponse.getMetadata(Metadata.REQUEST_ID)).thenReturn("1");
   }

   @Test(expected = NullPointerException.class)
   public void testNullObjectManager()
   {
      new WriteObjectNameConsumer(null, this.statusCodes);
   }

   @Test(expected = NullPointerException.class)
   public void testNullStatusCodes()
   {
      new WriteObjectNameConsumer(this.mockObjectManager, null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testEmptyStatusCodes()
   {
      new WriteObjectNameConsumer(this.mockObjectManager, Collections.<Integer> emptyList());
   }

   @Test(expected = NullPointerException.class)
   public void testStatusCodesNullElement()
   {
      final List<Integer> sc = Lists.newArrayList();
      sc.add(null);
      new WriteObjectNameConsumer(this.mockObjectManager, sc);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testSmallStatusCode()
   {
      final List<Integer> sc = Lists.newArrayList();
      sc.add(99);
      new WriteObjectNameConsumer(this.mockObjectManager, sc);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testLargeStatusCode()
   {
      final List<Integer> sc = Lists.newArrayList();
      sc.add(600);
      new WriteObjectNameConsumer(this.mockObjectManager, sc);
   }

   @Test(expected = NullPointerException.class)
   public void testNullResponse()
   {
      new WriteObjectNameConsumer(this.mockObjectManager, this.statusCodes).consume(null);
   }

   @Test
   public void testSuccessfulWrite()
   {
      when(this.mockRequest.getMethod()).thenReturn(Method.PUT);
      when(this.mockRequest.getMetadata(Metadata.OBJECT_NAME)).thenReturn(
            "5c18be1057404792923dc487ca40f2370000");
      when(this.mockResponse.getStatusCode()).thenReturn(201);

      final ObjectNameConsumer c =
            new WriteObjectNameConsumer(this.mockObjectManager, this.statusCodes);

      c.consume(new Pair<Request, Response>(this.mockRequest, this.mockResponse));
      verify(this.mockObjectManager).writeNameComplete(isA(ObjectName.class));
   }

   @Test
   public void testSuccessfulWriteSOH()
   {
      when(this.mockRequest.getMethod()).thenReturn(Method.PUT);
      when(this.mockResponse.getStatusCode()).thenReturn(201);
      // for SOH, the metadata gets set on response rather than request
      when(this.mockResponse.getMetadata(Metadata.OBJECT_NAME)).thenReturn(
            "5c18be1057404792923dc487ca40f2370000");

      final ObjectNameConsumer c =
            new WriteObjectNameConsumer(this.mockObjectManager, this.statusCodes);

      c.consume(new Pair<Request, Response>(this.mockRequest, this.mockResponse));
      verify(this.mockObjectManager).writeNameComplete(isA(ObjectName.class));
   }

   @Test
   public void testUnsuccessfulWrite()
   {
      when(this.mockRequest.getMethod()).thenReturn(Method.PUT);
      when(this.mockResponse.getStatusCode()).thenReturn(500);

      final ObjectNameConsumer c =
            new WriteObjectNameConsumer(this.mockObjectManager, this.statusCodes);

      c.consume(new Pair<Request, Response>(this.mockRequest, this.mockResponse));
      verify(this.mockObjectManager, never()).writeNameComplete(isA(ObjectName.class));
   }

   @Test
   public void testOperationDoesNotMatchMethod()
   {
      when(this.mockRequest.getMethod()).thenReturn(Method.GET);

      final ObjectNameConsumer c =
            new WriteObjectNameConsumer(this.mockObjectManager, this.statusCodes);

      c.consume(new Pair<Request, Response>(this.mockRequest, this.mockResponse));
      verify(this.mockObjectManager, never()).writeNameComplete((isA(ObjectName.class)));
   }

   @Test(expected = ObjectManagerException.class)
   public void testObjectManagerException()
   {
      when(this.mockRequest.getMethod()).thenReturn(Method.PUT);
      when(this.mockRequest.getMetadata(Metadata.OBJECT_NAME)).thenReturn(
            "52f7ee3599723d3d9ead2cc492c8209f0010");
      when(this.mockResponse.getStatusCode()).thenReturn(201);
      doThrow(new ObjectManagerException()).when(this.mockObjectManager).writeNameComplete(
            any(ObjectName.class));

      final ObjectNameConsumer c =
            new WriteObjectNameConsumer(this.mockObjectManager, this.statusCodes);

      c.consume(new Pair<Request, Response>(this.mockRequest, this.mockResponse));
   }

   @Test(expected = IllegalStateException.class)
   public void testNoObject() throws URISyntaxException
   {
      when(this.mockRequest.getUri()).thenReturn(new URI("http://192.168.8.1/container"));
      when(this.mockRequest.getMethod()).thenReturn(Method.PUT);
      when(this.mockResponse.getStatusCode()).thenReturn(201);

      new WriteObjectNameConsumer(this.mockObjectManager, this.statusCodes)
            .consume(new Pair<Request, Response>(this.mockRequest, this.mockResponse));
   }
}
