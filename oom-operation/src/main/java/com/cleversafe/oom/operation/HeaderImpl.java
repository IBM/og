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
// Date: Feb 24, 2014
// ---------------------

package com.cleversafe.oom.operation;

import static com.google.common.base.Preconditions.checkNotNull;

import com.cleversafe.oom.api.Header;

public class HeaderImpl implements Header
{
   private final String key;
   private final String value;

   /**
    * Constructs an http header from the given key and value. Headers may contain multiple values,
    * in which case the user should concatenate those values prior to calling this constructor.
    * 
    * @param key
    *           the key of this header
    * @param value
    *           the value of this header
    * @throws NullPointerException
    *            if key is null
    * @throws NullPointerException
    *            if value is null
    */
   public HeaderImpl(final String key, final String value)
   {
      this.key = checkNotNull(key, "key must not be null");
      this.value = checkNotNull(value, "value must not be null");
   }

   @Override
   public String getKey()
   {
      return this.key;
   }

   @Override
   public String getValue()
   {
      return this.value;
   }
}
