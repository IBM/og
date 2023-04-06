/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.test;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.Uninterruptibles;
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

/**
 * a callable test execution
 * 
 * @since 1.0
 */
@Singleton
public class   LoadTest implements Callable<LoadTestResult> {
  private static final Logger _logger = LoggerFactory.getLogger(LoadTest.class);
  private static final Logger _exceptionLogger = LoggerFactory.getLogger("ExceptionLogger");
  private static final Logger _consoleLogger = LoggerFactory.getLogger("ConsoleLogger");
  private final RequestManager requestManager;
  private final Client client;
  private final Scheduler scheduler;
  private final Thread schedulerThread;
  private final EventBus eventBus;
  private final boolean abortMpuWhenStopping;
  private final boolean shutdownImmediate;
  private final int shutdownTimeout;
  private final AtomicBoolean running;
  private long timestampStart;
  private long timestampFinish;
  private volatile int result;
  private final AtomicBoolean noMoreRequests;
  private final CountDownLatch completed;
  private ArrayList<String> messages;

  public static final int RESULT_SUCCESS = 0;
  public static final int RESULT_FAILURE = -1;

  /**
   * Creates an instance
   * 
   * @param requestManager a generator of request instances
   * @param client a request executor
   * @param scheduler a scheduler which determines request rate
   * @param eventBus an event bus for notifying components of events in the system
   * @param shutdownImmediate if true, abort all in-progress requests at shutdown,
   *        else wait for all current requests to finish and shutdown
   * @param shutdownTimeout time in seconds to wait for requests to gracefully complete
   * @throws NullPointerException if requestSupplier, client, scheduler, or eventBus are null
   */
  @Inject
  public LoadTest(final RequestManager requestManager, final Client client,
      final Scheduler scheduler, final EventBus eventBus,
      @Named("shutdownImmediate") final boolean shutdownImmediate,
      @Named("shutdownTimeout") final int shutdownTimeout,
      @Named("abortMpuWhenStopping") final boolean abortMpuWhenStopping) {
    this.requestManager = checkNotNull(requestManager);
    this.client = checkNotNull(client);
    this.scheduler = checkNotNull(scheduler);
    this.schedulerThread = new Thread(new SchedulerRunnable(), "loadtest-scheduler");
    this.schedulerThread.setDaemon(true);
    this.eventBus = checkNotNull(eventBus);
    this.abortMpuWhenStopping = abortMpuWhenStopping;
    this.shutdownImmediate = shutdownImmediate;
    this.shutdownTimeout = shutdownTimeout;
    this.running = new AtomicBoolean(true);
    this.noMoreRequests = new AtomicBoolean(false);
    this.result = RESULT_SUCCESS;
    this.completed = new CountDownLatch(1);
    this.messages =  new ArrayList<String>();
  }

  private class SchedulerRunnable implements Runnable {
    @Override
    public void run() {
      try {
        while (LoadTest.this.running.get()) {
          LoadTest.this.scheduler.schedule();
          if (LoadTest.this.noMoreRequests.get()) {
            stopScheduler();
          }
          if (LoadTest.this.running.get() && !LoadTest.this.noMoreRequests.get()) {
            try {
              final Request request = LoadTest.this.requestManager.get();
              _logger.trace("Created request {}", request);
              // RequestManager.get() could block (in case of Multipart supplier) and when it returns the test may be stopped and client could be shutdown.
              // We cannot submit a new request if client is shutdown. So check again to make sure that the test is
              // still running.
              if (LoadTest.this.running.get()) {
                final ListenableFuture<Response> future = LoadTest.this.client.execute(request);
                LoadTest.this.eventBus.post(request);
                addCallback(request, future);
              }
            } catch(NoMoreRequestsException nre) {
              _logger.info("NoMoreRequestsException thrown. All requests are cleanly aborted");
              LoadTest.this.noMoreRequests.set(true);
            }
          }
        }
      } catch (final Exception e) {
        _logger.error("Exception while producing request", e);
        _exceptionLogger.error("Exception while producing request", e);
        abortTest(e.getMessage());
        stopScheduler();
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
    return new LoadTestResult(this.timestampStart, this.timestampFinish, this.result, ImmutableList.copyOf(this.messages));
  }

  /**
   * Cleanly stop this test
   */
  public void stopTest() {
    _logger.debug("Entering stopTest");
    // ensure this code is only run once
    // abort multipart upload sessions in progress
    if (this.abortMpuWhenStopping) {
      _consoleLogger.info("aborting MPU uploads. Please wait ...");
      this.requestManager.setAbort(true);
    } else {
      // normal case - no mpu aborts when stopping
      this.requestManager.setShutdownImmediate(true);
      this.noMoreRequests.set(true);

    }
  }

  private void stopScheduler() {
    if (this.running.getAndSet(false)) {
      _logger.debug("set running flag to false to stop scheduler");
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
            Integer result = Uninterruptibles.getUninterruptibly(
                    LoadTest.this.client.shutdown(LoadTest.this.shutdownImmediate, LoadTest.this.shutdownTimeout));
            if (result > 0) {
              _logger.warn("Terminated {} ongoing requests during shutdown", result);
              if (!LoadTest.this.shutdownImmediate) {
                LoadTest.this.result = result;
                LoadTest.this.messages.add("Incomplete requests past shutdown timeout");
              }
            }
            if (result < 0) {
              _logger.error("Error encountered during client shutdown");
            }
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
    this.result = RESULT_FAILURE;


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
    final ThreadFactory fac = new ThreadFactoryBuilder().setNameFormat("clientCallback-%d").build();
    ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(fac));
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
        LoadTest.this.scheduler.complete();
      }
    }, executorService);
  }

  @Override
  public String toString() {
    return String.format(
        "LoadTest [%n" + "requestManager=%s,%n" + "scheduler=%s,%n" + "client=%s,%n"
            + "shutdownImmediate=%s,%n" + "shutdownTimeout=%s%n" + "]",
        this.requestManager, this.scheduler, this.client, this.shutdownImmediate, this.shutdownTimeout);
  }
  
}
