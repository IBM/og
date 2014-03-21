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
// Date: Mar 19, 2014
// ---------------------

package com.cleversafe.oom.util.producer;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import com.cleversafe.oom.api.Producer;
import com.cleversafe.oom.operation.RequestContext;

public class ListProducer<T> implements Producer<List<T>>
{
   private final List<Producer<T>> producers;

   public ListProducer(final List<Producer<T>> producers)
   {
      this.producers = checkNotNull(producers, "producers must not be null");
   }

   @Override
   public List<T> produce(final RequestContext context)
   {
      final List<T> items = new ArrayList<T>(this.producers.size());
      for (final Producer<T> producer : this.producers)
      {
         items.add(producer.produce(context));
      }
      return items;
   }
}
