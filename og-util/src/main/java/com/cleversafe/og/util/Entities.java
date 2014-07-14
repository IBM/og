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

import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.operation.EntityType;

public class Entities
{
   private static final Entity NONE_ENTITY = Entities.createEntity(EntityType.NONE, 0);

   private Entities()
   {}

   public static Entity none()
   {
      return NONE_ENTITY;
   }

   public static Entity random(final long size)
   {
      return createEntity(EntityType.RANDOM, size);
   }

   public static Entity zeroes(final long size)
   {
      return createEntity(EntityType.ZEROES, size);
   }

   private static Entity createEntity(final EntityType type, final long size)
   {
      checkArgument(size >= 0, "size must be >= 0 [%s]", size);
      return new Entity()
      {
         private final EntityType entityType = checkNotNull(type);
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
}
