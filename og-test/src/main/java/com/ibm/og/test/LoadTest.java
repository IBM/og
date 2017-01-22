/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.test;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.ibm.og.http.HttpResponse;
import com.ibm.og.scheduling.Scheduler;
import com.ibm.og.test.condition.LoadTestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.og.api.Client;
import com.ibm.og.api.Request;
import com.ibm.og.api.Response;
import com.ibm.og.util.Pair;
import com.ibm.og.util.TestState;
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
  private static final Logger _exceptionLogger = LoggerFactory.getLogger("ExceptionLogger");
  private final RequestManager requestManager;
  private final Client client;
  private final Scheduler scheduler;
  private final Thread schedulerThread;
  private final EventBus eventBus;
  private final boolean shutdownImmediate;
  private final AtomicBoolean running;
  private long timestampStart;
  private long timestampFinish;
  private volatile boolean success;
  private final CountDownLatch completed;
  private ArrayList<String> messages;

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
    this.schedulerThread = new Thread(new SchedulerRunnable(), "loadtest-scheduler");
    this.schedulerThread.setDaemon(true);
    this.eventBus = checkNotNull(eventBus);
    this.shutdownImmediate = shutdownImmediate;
    this.running = new AtomicBoolean(true);
    this.success = true;
    this.completed = new CountDownLatch(1);
    this.messages =  new ArrayList<String>();

  }

  private class SchedulerRunnable implements Runnable {
    @Override
    public void run() {
      try {
        while (LoadTest.this.running.get()) {
          LoadTest.this.scheduler.schedule();
          if (LoadTest.this.running.get()) {
            final Request request = LoadTest.this.requestManager.get();
            _logger.trace("Created request {}", request);

            final ListenableFuture<Response> future = LoadTest.this.client.execute(request);
            LoadTest.this.eventBus.post(request);
            addCallback(request, future);
          }
        }
      } catch (final Exception e) {
        _logger.error("Exception while producing request", e);
        _exceptionLogger.error("Exception while producing request", e);
        abortTest(e.getMessage());
      }
    }
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
    _logger.debug("Posting TestState.RUNNING to event bus");
    this.eventBus.post(TestState.RUNNING);

    _logger.debug("Starting scheduler thread");
    this.schedulerThread.start();

    _logger.debug("Waiting for test complete");
    Uninterruptibles.awaitUninterruptibly(this.completed);
    this.timestampFinish = System.currentTimeMillis();
    return new LoadTestResult(this.timestampStart, this.timestampFinish, this.success, ImmutableList.copyOf(this.messages));
  }

  /**
   * Cleanly stop this test
   */
  public void stopTest() {
    _logger.debug("Entering stopTest");
    // ensure this code is only run once
    if (this.running.getAndSet(false)) {
      _logger.debug("Interrupting scheduler thread");
      this.schedulerThread.interrupt();

      // currently a new thread is required here to run shutdown logic because stopTest can be
      // called via a client worker thread via client -> eventbus -> stopping condition -> stopTest,
      // which will introduce a deadlock since stopTest waits until all client threads are done. An
      // alternative approach is to us an async eventbus, but this requires managing the shutdown of
      // the async eventbus' executor somewhere
      new Thread("loadtest-shutdown") {
        @Override
        public void run() {
          try {
            _logger.debug("Posting TestState.STOPPING to event bus");
            LoadTest.this.eventBus.post(TestState.STOPPING);

            _logger.debug("Waiting on client shutdown future");
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
  public void abortTest(final String message) {
    _logger.debug("Entering abortTest");
    this.success = false;


    // requestSupplierException unit test case was failing because guava library crashes because of NPE
    // when calling ImmutableList.copyOf(messages).
    // check null because the detailed message may not be set in the generated exception in rare cases.
    if (message != null) {
      this.messages.add(message);
    } else {
      _logger.error("abort test called with no message");
    }
    stopTest();
  }

  private void addCallback(final Request request, final ListenableFuture<Response> future) {
    Futures.addCallback(future, new FutureCallback<Response>() {
      @Override
      public void onSuccess(final Response response) {
        _logger.trace("Operation completed {}, {}", request, response);
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
