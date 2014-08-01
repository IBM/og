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

package com.cleversafe.og.http;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;

import com.cleversafe.og.http.HttpUtil;
import com.cleversafe.og.operation.Method;
import com.cleversafe.og.util.Operation;

public class HttpUtilTest
{
   @Test(expected = UnsupportedOperationException.class)
   public void testModifyStatusCodes()
   {
      HttpUtil.SUCCESS_STATUS_CODES.add(500);
   }

   @Test(expected = NullPointerException.class)
   public void testNullMethod()
   {
      HttpUtil.toOperation(null);
   }

   @Test
   public void testPutMethod()
   {
      Assert.assertEquals(Operation.WRITE, HttpUtil.toOperation(Method.PUT));
   }

   @Test
   public void testPostMethod()
   {
      Assert.assertEquals(Operation.WRITE, HttpUtil.toOperation(Method.POST));
   }

   @Test
   public void testGetMethod()
   {
      Assert.assertEquals(Operation.READ, HttpUtil.toOperation(Method.GET));
   }

   @Test
   public void testHeadMethod()
   {
      Assert.assertEquals(Operation.READ, HttpUtil.toOperation(Method.HEAD));
   }

   @Test
   public void testDeleteMethod()
   {
      Assert.assertEquals(Operation.DELETE, HttpUtil.toOperation(Method.DELETE));
   }

   @Test(expected = NullPointerException.class)
   public void testNullUri()
   {
      HttpUtil.getObjectName(null);
   }

   @Test(expected = NullPointerException.class)
   public void testNullScheme() throws URISyntaxException
   {
      HttpUtil.getObjectName(new URI("192.168.8.1/container"));
   }

   @Test(expected = IllegalArgumentException.class)
   public void testUnrecognizedScheme() throws URISyntaxException
   {
      HttpUtil.getObjectName(new URI("ftp://192.168.8.1/container"));
   }

   @Test
   public void testRootUriNoObject() throws URISyntaxException
   {
      Assert.assertNull(HttpUtil.getObjectName(new URI("http://192.168.8.1/container")));
   }

   @Test
   public void testRootUriObject() throws URISyntaxException
   {
      Assert.assertEquals("object",
            HttpUtil.getObjectName(new URI("https://192.168.8.1/container/object")));
   }

   @Test
   public void testRootUriObjectSlash() throws URISyntaxException
   {
      Assert.assertEquals("object",
            HttpUtil.getObjectName(new URI("http://192.168.8.1/container/object/")));
   }

   @Test
   public void testNoObject() throws URISyntaxException
   {
      Assert.assertNull(HttpUtil.getObjectName(new URI("http://192.168.8.1/soh/container")));
   }

   @Test
   public void testObject() throws URISyntaxException
   {
      Assert.assertEquals("object",
            HttpUtil.getObjectName(new URI("https://192.168.8.1/soh/container/object")));
   }

   @Test
   public void testObjectSlash() throws URISyntaxException
   {
      Assert.assertEquals("object",
            HttpUtil.getObjectName(new URI("http://192.168.8.1/soh/container/object/")));
   }
}
