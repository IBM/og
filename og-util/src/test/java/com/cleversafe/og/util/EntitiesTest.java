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

   @Test(expected = NullPointerException.class)
   public void testNullEntityType()
   {
      Entities.of(null, 0);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeSize()
   {
      Entities.of(EntityType.RANDOM, -1);
   }

   @Test
   public void testZeroSizeNoneType()
   {
      Entities.of(EntityType.NONE, 0);
   }

   @Test
   public void testZeroSize()
   {
      Entities.of(EntityType.RANDOM, 0);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testPositiveSizeNoneType()
   {
      // NONE represents lack of an entity, and shouldn't be used otherwise
      Entities.of(EntityType.NONE, 1);
   }

   @Test
   public void testPositiveSize()
   {
      final Entity e = Entities.of(EntityType.ZEROES, 1);
      Assert.assertEquals(EntityType.ZEROES, e.getType());
      Assert.assertEquals(1, e.getSize());
   }
}
