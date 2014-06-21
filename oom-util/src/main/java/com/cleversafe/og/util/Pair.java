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
// Date: Apr 2, 2014
// ---------------------

package com.cleversafe.og.util;

import static com.google.common.base.Preconditions.checkNotNull;

public class Pair<K, V>
{
   private final K key;
   private final V value;

   public Pair(final K key, final V value)
   {
      this.key = checkNotNull(key, "key must not be null");
      this.value = checkNotNull(value, "value must not be null");
   }

   public K getKey()
   {
      return this.key;
   }

   public V getValue()
   {
      return this.value;
   }
}
