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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.InputStream;

import org.cleversafe.util.RandomInputStream;

public class RandomInputStreamEntity implements Entity
{
   private final RandomInputStream in;

   public RandomInputStreamEntity(final RandomInputStream in)
   {
      this.in = checkNotNull(in, "in must not be null");
   }

   @Override
   public InputStream getInputStream()
   {
      return this.in;
   }

   @Override
   public boolean isFile()
   {
      return false;
   }

   @Override
   public File getFile()
   {
      return null;
   }

   @Override
   public long getSize()
   {
      return this.in.getSize();
   }
}
