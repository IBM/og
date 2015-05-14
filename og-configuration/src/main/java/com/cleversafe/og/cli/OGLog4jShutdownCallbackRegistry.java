/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.cli;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;

import com.google.common.util.concurrent.Uninterruptibles;

@SuppressWarnings("serial")
public class OGLog4jShutdownCallbackRegistry extends DefaultShutdownCallbackRegistry {
  private static Runnable OG_SHUTDOWN_HOOK;

  public OGLog4jShutdownCallbackRegistry() {}

  @Override
  public void run() {
    // gracefully shutdown og components
    if (OG_SHUTDOWN_HOOK != null) {
      LOGGER.info("Running og shutdown hook");
      OG_SHUTDOWN_HOOK.run();
    }

    // workaround to wait for log4j completion
    LOGGER.info("Running wait for gzCompressAction task");
    waitForGzCompressActionCompletion();

    // run default log4j hooks
    LOGGER.info("Running log4j shutdown hooks");
    super.run();
  }

  // FIXME ensure log4j async gzip compress action completes; default log4j shutdown
  // hook does not wait for completion, so the application can exit with a partially written gzip
  // file. Remove once a better alternative is known or log4j has been updated
  public static void waitForGzCompressActionCompletion() {
    final Logger loggerImpl = (Logger) LogManager.getLogger("RequestLogger");
    final RollingRandomAccessFileAppender appender =
        (RollingRandomAccessFileAppender) loggerImpl.getAppenders().get("RequestLog");

    final File logDirectory = new File(appender.getFileName()).getParentFile();
    final FilenameFilter incompleteGzFilenameFilter = new IncompleteGzCompressAction();

    while (logDirectory.list(incompleteGzFilenameFilter).length > 0) {
      LOGGER.debug("Polling found incomplete gz files, will wait");
      Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
    }
  }

  public static class IncompleteGzCompressAction implements FilenameFilter {
    private final Pattern incompletePattern;

    public IncompleteGzCompressAction() {
      this.incompletePattern = Pattern.compile("request.log-\\d+");
    }

    @Override
    public boolean accept(final File dir, final String name) {
      return this.incompletePattern.matcher(name).matches();
    }

  }

  public static void setOGShutdownHook(final Runnable ogShutdownHook) {
    LOGGER.debug("Setting ogShutdownHook to [{}]", ogShutdownHook);
    OG_SHUTDOWN_HOOK = checkNotNull(ogShutdownHook);
  }
}
