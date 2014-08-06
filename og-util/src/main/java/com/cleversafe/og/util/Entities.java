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

import com.cleversafe.og.api.Entity;
import com.cleversafe.og.api.EntityType;

/**
 * A utility class for creating entity instances
 * 
 * @since 1.0
 */
public class Entities
{
   private static final Entity NONE_ENTITY = Entities.create(EntityType.NONE, 0);

   private Entities()
   {}

   /**
    * Creates an entity instance representing no entity
    * 
    * @return an entity instance
    */
   public static Entity none()
   {
      return NONE_ENTITY;
   }

   /**
    * Creates an entity instance representing an entity with random data
    * 
    * @param size
    *           the size of the entity
    * @return a random entity instance
    * @throws IllegalArgumentException
    *            if size is negative
    */
   public static Entity random(final long size)
   {
      return create(EntityType.RANDOM, size);
   }

   /**
    * Creates an entity instance representing an entity with zeroes for data
    * 
    * @param size
    *           the size of the entity
    * @return a zero based entity instance
    * @throws IllegalArgumentException
    *            if size is negative
    */
   public static Entity zeroes(final long size)
   {
      return create(EntityType.ZEROES, size);
   }

   private static Entity create(final EntityType type, final long size)
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
