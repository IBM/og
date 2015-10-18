/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.test;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.api.Client;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.http.HttpResponse;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.test.condition.LoadTestResult;
import com.cleversafe.og.util.Pair;
import com.cleversafe.og.util.TestState;
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
public class LoadTest implements Callable<LoadTestResult> {
  private static final Logger _logger = LoggerFactory.getLogger(LoadTest.class);
  private final RequestManager requestManager;
  private final Client client;
  private final Scheduler scheduler;
  private final EventBus eventBus;
  private final boolean shutdownImmediate;
  private final AtomicBoolean running;
  private long timestampStart;
  private long timestampFinish;
  private volatile boolean success;
  private final CountDownLatch completed;

  /**
   * Creates an instance
   * 
   * @param requestManager a generator of request instances
   * @param client a request executor
   * @param scheduler a scheduler which determines request rate
   * @param eventBus an event bus for notifying components of events in the system
   * @param shutdownImmediate if true, abort all in-progress requests at shutdown, else wait until
   *        all current requests finish and shutdown gracefully
   * @throws NullPointerException if requestSupplier, client, scheduler, or eventBus are null
   */
  @Inject
  public LoadTest(final RequestManager requestManager, final Client client,
      final Scheduler scheduler, final EventBus eventBus,
      @Named("shutdownImmediate") final boolean shutdownImmediate) {
    this.requestManager = checkNotNull(requestManager);
    this.client = checkNotNull(client);
    this.scheduler = checkNotNull(scheduler);
    this.eventBus = checkNotNull(eventBus);
    this.shutdownImmediate = shutdownImmediate;
    this.running = new AtomicBoolean(true);
    this.success = true;
    this.completed = new CountDownLatch(1);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.concurrent.Callable#call()
   * 
   * @return results from this test
   */
  @Override
  public LoadTestResult call() {
    this.timestampStart = System.currentTimeMillis();
    this.eventBus.post(TestState.RUNNING);
    try {
      while (this.running.get()) {
        final Request request = this.requestManager.get();
        final ListenableFuture<Response> future = this.client.execute(request);
        this.eventBus.post(request);
        addCallback(request, future);
        this.scheduler.waitForNext();
      }
    } catch (final Exception e) {
      _logger.error("Exception while producing request", e);
      abortTest();
    }

    Uninterruptibles.awaitUninterruptibly(this.completed);
    this.timestampFinish = System.currentTimeMillis();
    return new LoadTestResult(this.timestampStart, this.timestampFinish, this.success);
  }

  /**
   * Cleanly stop this test
   */
  public void stopTest() {
    // ensure this code is only run once
    if (this.running.getAndSet(false)) {
      // currently a new thread is required here to run shutdown logic because stopTest can be
      // called via a client worker thread via client -> eventbus -> stopping condition -> stopTest,
      // which will introduce a deadlock since stopTest waits until all client threads are done. An
      // alternative approach is to us an async eventbus, but this requires managing the shutdown of
      // the async eventbus' executor somewhere
      new Thread() {
        @Override
        public void run() {
          try {
            LoadTest.this.eventBus.post(TestState.STOPPING);
            Uninterruptibles
                .getUninterruptibly(LoadTest.this.client.shutdown(LoadTest.this.shutdownImmediate));
          } catch (final Exception e) {
            _logger.error("Exception while attempting to shutdown client", e);
          }
          LoadTest.this.completed.countDown();
        }
      }.start();
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
      }

      @Override
      public void onFailure(final Throwable t) {
        _logger.error("Exception while processing operation", t);
        final Response response = new HttpResponse.Builder().withStatusCode(599).build();
        postOperation(response);
      }

      private void postOperation(final Response response) {
        LoadTest.this.eventBus.post(response);
        LoadTest.this.eventBus.post(Pair.of(request, response));
      }
    });
  }

  @Override
  public String toString() {
    return String.format(
        "LoadTest [%n" + "requestManager=%s,%n" + "scheduler=%s,%n" + "client=%s,%n"
            + "shutdownImmediate=%s%n" + "]",
        this.requestManager, this.scheduler, this.client, this.shutdownImmediate);
  }
}
