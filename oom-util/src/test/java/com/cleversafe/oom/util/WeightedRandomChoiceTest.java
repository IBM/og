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
// Date: Oct 23, 2013
// ---------------------

package com.cleversafe.oom.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WeightedRandomChoiceTest
{
   private WeightedRandomChoice<Object> wrc;

   @Before
   public void setBefore()
   {
      this.wrc = new WeightedRandomChoice<Object>();
   }

   @Test(expected = NullPointerException.class)
   public void testNullRandom()
   {
      new WeightedRandomChoice<Object>(null);
   }

   @Test
   public void testBasicWeightedRandomChoice()
   {
      new WeightedRandomChoice<Object>();
   }

   @Test
   public void testWeightedRandomChoiceWithRandom()
   {
      new WeightedRandomChoice<Object>(new Random());
   }

   @Test(expected = NullPointerException.class)
   public void testBasicNullChoice()
   {
      this.wrc.addChoice(null);
   }

   @Test
   public void testBasicAddChoice()
   {
      this.wrc.addChoice(new Object());
   }

   @Test(expected = NullPointerException.class)
   public void testNullChoice()
   {
      this.wrc.addChoice(null, 1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeWeight()
   {
      this.wrc.addChoice(new Object(), -1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testZeroWeight()
   {
      this.wrc.addChoice(new Object(), 0);
   }

   @Test
   public void testAddChoice()
   {
      this.wrc.addChoice(new Object(), 1);
   }

   @Test(expected = IllegalStateException.class)
   public void testNextChoiceWithoutAddChoice()
   {
      this.wrc.nextChoice();
   }

   @Test
   public void testNextChoiceSingleAddChoice()
   {
      this.wrc.addChoice(new Object());
      this.wrc.nextChoice();
      this.wrc.nextChoice();
      this.wrc.nextChoice();
   }

   @Test
   public void testNextChoiceMultipleAddChoice()
   {
      final WeightedRandomChoice<String> wrc = new WeightedRandomChoice<String>();
      final String one = "one";
      final String two = "two";
      final String three = "three";
      final Map<String, Integer> m = new HashMap<String, Integer>();
      m.put(one, 0);
      m.put(two, 0);
      m.put(three, 0);
      wrc.addChoice(one, 1);
      wrc.addChoice(two, 1);
      wrc.addChoice(three, 1);

      for (int i = 0; i < 100; i++)
      {
         final String choice = wrc.nextChoice();
         final int count = m.get(choice);
         m.put(choice, count + 1);
      }

      Assert.assertTrue(m.get(one) > 0);
      Assert.assertTrue(m.get(two) > 0);
      Assert.assertTrue(m.get(three) > 0);
   }
}
