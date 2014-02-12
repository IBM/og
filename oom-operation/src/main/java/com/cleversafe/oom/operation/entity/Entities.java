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
// Date: Feb 12, 2014
// ---------------------

package com.cleversafe.oom.operation.entity;

import java.io.InputStream;

import org.cleversafe.util.RandomInputStream;

public class Entities
{
   private static final Entity EMPTY = new Entity()
   {
      @Override
      public InputStream getInputStream()
      {
         return null;
      }

      @Override
      public long getSize()
      {
         return 0;
      }
   };

   private Entities()
   {}

   public static Entity random(final long size)
   {
      return new RandomInputStreamEntity(new RandomInputStream(size));
   }

   public static Entity empty()
   {
      return Entities.EMPTY;
   }
}
