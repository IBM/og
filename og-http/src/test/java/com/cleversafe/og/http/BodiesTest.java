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
// Date: Jun 28, 2014
// ---------------------

package com.cleversafe.og.http;

import org.junit.Assert;
import org.junit.Test;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Data;
import com.cleversafe.og.http.Bodies;

public class BodiesTest
{
   @Test
   public void testNone()
   {
      final Body e = Bodies.none();
      Assert.assertEquals(Data.NONE, e.getData());
      Assert.assertEquals(0, e.getSize());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testRandomNegativeSize()
   {
      Bodies.random(-1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testZeroesNegativeSize()
   {
      Bodies.zeroes(-1);
   }

   @Test
   public void testRandomZeroSize()
   {
      Bodies.random(0);
   }

   @Test
   public void testZeroesZeroSize()
   {
      Bodies.zeroes(0);
   }

   @Test
   public void testRandomPositiveSize()
   {
      final Body e = Bodies.random(1);
      Assert.assertEquals(Data.RANDOM, e.getData());
      Assert.assertEquals(1, e.getSize());
   }

   @Test
   public void testZeroesPositiveSize()
   {
      final Body e = Bodies.zeroes(1);
      Assert.assertEquals(Data.ZEROES, e.getData());
      Assert.assertEquals(1, e.getSize());
   }
}
