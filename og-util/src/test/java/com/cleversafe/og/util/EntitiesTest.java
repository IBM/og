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

package com.cleversafe.og.util;

import org.junit.Assert;
import org.junit.Test;

import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.operation.EntityType;

public class EntitiesTest
{
   @Test
   public void testNone()
   {
      final Entity e = Entities.none();
      Assert.assertEquals(EntityType.NONE, e.getType());
      Assert.assertEquals(0, e.getSize());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testRandomNegativeSize()
   {
      Entities.random(-1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testZeroesNegativeSize()
   {
      Entities.zeroes(-1);
   }

   @Test
   public void testRandomZeroSize()
   {
      Entities.random(0);
   }

   @Test
   public void testZeroesZeroSize()
   {
      Entities.zeroes(0);
   }

   @Test
   public void testRandomPositiveSize()
   {
      final Entity e = Entities.random(1);
      Assert.assertEquals(EntityType.RANDOM, e.getType());
      Assert.assertEquals(1, e.getSize());
   }

   @Test
   public void testZeroesPositiveSize()
   {
      final Entity e = Entities.zeroes(1);
      Assert.assertEquals(EntityType.ZEROES, e.getType());
      Assert.assertEquals(1, e.getSize());
   }
}
