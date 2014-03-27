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
// Date: Mar 27, 2014
// ---------------------

package com.cleversafe.oom.guice;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.cleversafe.oom.api.Consumer;
import com.cleversafe.oom.api.Producer;
import com.cleversafe.oom.cli.json.JSONConfiguration;
import com.cleversafe.oom.distribution.Distribution;
import com.cleversafe.oom.distribution.UniformDistribution;
import com.cleversafe.oom.http.producer.RequestProducer;
import com.cleversafe.oom.http.producer.URLProducer;
import com.cleversafe.oom.operation.Method;
import com.cleversafe.oom.operation.OperationType;
import com.cleversafe.oom.operation.OperationTypeMix;
import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.operation.Response;
import com.cleversafe.oom.scheduling.RequestRateScheduler;
import com.cleversafe.oom.scheduling.Scheduler;
import com.cleversafe.oom.soh.SOHOperationManager;
import com.cleversafe.oom.util.producer.Producers;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class SOHOperationManagerProvider implements Provider<SOHOperationManager>
{
   private final JSONConfiguration config;
   private final DefaultProducers defaults;
   private final OperationTypeMix mix;

   @Inject
   public SOHOperationManagerProvider(
         final JSONConfiguration config,
         final DefaultProducers defaults,
         final OperationTypeMix mix)
   {
      this.config = config;
      this.defaults = defaults;
      this.mix = mix;
   }

   @Override
   public SOHOperationManager get()
   {
      final Map<OperationType, Producer<Request>> producers =
            new HashMap<OperationType, Producer<Request>>();
      producers.put(OperationType.WRITE, createSOHWriteProducer());
      producers.put(OperationType.READ, createSOHReadProducer());
      producers.put(OperationType.DELETE, createSOHDeleteProducer());

      final List<Consumer<Response>> consumers = new ArrayList<Consumer<Response>>();

      // TODO account for scheduler units and threaded vs iops scheduling
      final Distribution sleepDuration =
            new UniformDistribution(this.config.getConcurrency().getCount(), 0);
      final Scheduler scheduler = new RequestRateScheduler(sleepDuration, TimeUnit.SECONDS);
      return new SOHOperationManager(this.mix, producers, consumers, scheduler);
   }

   private Producer<Request> createSOHWriteProducer()
   {
      final List<Producer<String>> parts = new ArrayList<Producer<String>>();
      parts.add(this.defaults.getContainer());
      final Producer<URL> writeURL =
            new URLProducer(this.defaults.getScheme(), this.defaults.getHost(),
                  this.defaults.getPort(), parts,
                  this.defaults.getQueryParameters());

      return new RequestProducer(this.defaults.getId(),
            Producers.of("soh.put_object"),
            Producers.of(Method.PUT),
            writeURL,
            this.defaults.getHeaders(),
            this.defaults.getEntity(),
            this.defaults.getMetaData());
   }

   private Producer<Request> createSOHReadProducer()
   {
      return null;
   }

   private Producer<Request> createSOHDeleteProducer()
   {
      return null;
   }
}
