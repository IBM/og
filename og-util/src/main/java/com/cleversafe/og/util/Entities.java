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

package com.cleversafe.og.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.util.Random;

import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.operation.EntityType;

public class Entities
{
   private static final int BUF_SIZE = 1024;
   private static final byte[] ZERO_BUF = new byte[BUF_SIZE];
   private static final Entity NONE_ENTITY = Entities.of(EntityType.NONE, 0);
   // TODO performance of single random instance used by multiple threads? Need to quantify
   private static final Random RANDOM = new Random();

   private Entities()
   {}

   public static Entity none()
   {
      return NONE_ENTITY;
   }

   public static Entity of(final EntityType type, final long size)
   {
      checkNotNull(type);
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
      checkNotNull(entity);
      byte[] buf;

      switch (entity.getType())
      {
         case NONE :
            // TODO should we allow null return?
            return null;
         case ZEROES :
            buf = ZERO_BUF;
            break;
         default :
            buf = createRandomBuffer();
            break;
      }
      return new FixedBufferInputStream(buf, entity.getSize());
   }

   private static byte[] createRandomBuffer()
   {
      final byte[] buf = new byte[Entities.BUF_SIZE];
      RANDOM.nextBytes(buf);
      return buf;
   }
}
