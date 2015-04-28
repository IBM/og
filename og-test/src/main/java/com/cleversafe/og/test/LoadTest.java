/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.test;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.api.Client;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.http.HttpResponse;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.util.Pair;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;

/**
 * a callable test execution
 * 
 * @since 1.0
 */
@Singleton
public class LoadTest implements Callable<Boolean> {
  private static final Logger _logger = LoggerFactory.getLogger(LoadTest.class);
  private final RequestManager requestManager;
  private final Client client;
  private final Scheduler scheduler;
  private final EventBus eventBus;
  private volatile boolean running;
  private volatile boolean success;
  private final Set<ListenableFuture<Response>> activeRequests;
  private final CountDownLatch completed;

  /**
   * Creates an instance
   * 
   * @param requestManager a generator of request instances
   * @param client a request executor
   * @param scheduler a scheduler which determines request rate
   * @param eventBus an event bus for notifying components of events in the system
   * @throws NullPointerException if requestSupplier, client, scheduler, or eventBus are null
   */
  @Inject
  public LoadTest(final RequestManager requestManager, final Client client,
      final Scheduler scheduler, final EventBus eventBus) {
    this.requestManager = checkNotNull(requestManager);
    this.client = checkNotNull(client);
    this.scheduler = checkNotNull(scheduler);
    this.eventBus = checkNotNull(eventBus);
    this.running = true;
    this.success = true;
    this.activeRequests = Sets.newConcurrentHashSet();
    this.completed = new CountDownLatch(1);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.concurrent.Callable#call()
   * 
   * @return whether this test succeeded or failed
   */
  @Override
  public Boolean call() {
    try {
      while (this.running) {
        final Request request = this.requestManager.get();
        final ListenableFuture<Response> future = this.client.execute(request);
        this.activeRequests.add(future);
        addCallback(request, future);
        this.scheduler.waitForNext();
      }
    } catch (final Exception e) {
      this.success = false;
      this.running = false;
      _logger.error("Exception while producing request", e);
    }

    if (!this.activeRequests.isEmpty())
      Uninterruptibles.awaitUninterruptibly(this.completed);
    return this.success;
  }

  /**
   * Cleanly stop this test
   */
  public void stopTest() {
    this.running = false;
    for (ListenableFuture<Response> future : this.activeRequests) {
      future.cancel(true);
    }
  }

  /**
   * Immediately stop this test; marking it as failed
   */
  public void abortTest() {
    this.success = false;
    stopTest();
  }

  private void addCallback(final Request request, final ListenableFuture<Response> future) {
    Futures.addCallback(future, new FutureCallback<Response>() {
      @Override
      public void onSuccess(final Response response) {
        _logger.trace("Request executed {}, {}", request, response);
        postOperation(response);
        removeActiveOperation();
      }

      @Override
      public void onFailure(final Throwable t) {
        _logger.error("Exception while processing operation", t);
        LoadTest.this.running = false;
        final Response response = new HttpResponse.Builder().withStatusCode(599).build();
        postOperation(response);
        removeActiveOperation();
      }

      private void removeActiveOperation() {
        LoadTest.this.activeRequests.remove(future);
        if (!LoadTest.this.running && LoadTest.this.activeRequests.isEmpty())
          LoadTest.this.completed.countDown();
      }

      private void postOperation(final Response response) {
        LoadTest.this.eventBus.post(Pair.of(request, response));
      }
    });
  }

  @Override
  public String toString() {
    return String.format("LoadTest [%n" + "requestManager=%s,%n" + "scheduler=%s,%n"
        + "client=%s%n" + "]", this.requestManager, this.scheduler, this.client);
  }
}
