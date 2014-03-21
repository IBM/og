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
// Date: Mar 12, 2014
// ---------------------

package com.cleversafe.oom.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.util.Random;

import com.cleversafe.oom.operation.Entity;
import com.cleversafe.oom.operation.EntityType;

public class Entities
{
   private static final int BUF_SIZE = 1024;

   private Entities()
   {}

   public static Entity of(final EntityType type, final long size)
   {
      checkNotNull(type, "type must not be null");
      checkArgument(size >= 0, "size must be >= 0 [%s]", size);
      return new Entity()
      {
         private final EntityType entityType = type;
         private final long entitySize = size;

         @Override
         public EntityType getType()
         {
            return this.entityType;
         }

         @Override
         public long getSize()
         {
            return this.entitySize;
         }
      };
   }

   public static InputStream createInputStream(final Entity entity)
   {
      checkNotNull(entity, "entity must not be null");

      final byte[] buf = createBuffer(entity.getType());
      return new FixedBufferInputStream(buf, entity.getSize());
   }

   private static byte[] createBuffer(final EntityType type)
   {
      final byte[] buf = new byte[Entities.BUF_SIZE];
      switch (type)
      {
         default :
            final Random random = new Random(System.currentTimeMillis());
            random.nextBytes(buf);
            break;
      }
      return buf;
   }
}
