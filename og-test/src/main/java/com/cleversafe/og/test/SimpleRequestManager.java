/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.test;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.api.Request;
import com.cleversafe.og.supplier.RandomSupplier;
import com.cleversafe.og.supplier.Suppliers;
import com.google.common.base.Supplier;
import com.google.common.collect.Range;
import com.google.common.math.DoubleMath;

public class SimpleRequestManager implements RequestManager {
  private static Logger _logger = LoggerFactory.getLogger(SimpleRequestManager.class);
  private static final Range<Double> PERCENTAGE = Range.closed(0.0, 100.0);
  private static final double ERR = Math.pow(0.1, 6);
  private Supplier<Supplier<Request>> requestSupplier;

  @Inject
  @Singleton
  public SimpleRequestManager(@Named("write") final Supplier<Request> write,
      @Named("write.weight") double writeWeight, @Named("read") final Supplier<Request> read,
      @Named("read.weight") double readWeight, @Named("delete") final Supplier<Request> delete,
      @Named("delete.weight") double deleteWeight) {
    checkNotNull(write);
    checkNotNull(read);
    checkNotNull(delete);
    if (allEqual(0.0, writeWeight, readWeight, deleteWeight))
      writeWeight = 100.0;

    checkArgument(PERCENTAGE.contains(writeWeight),
        "write weight must be in range [0.0, 100.0] [%s]", writeWeight);
    checkArgument(PERCENTAGE.contains(readWeight),
        "read weight must be in range [0.0, 100.0] [%s]", readWeight);
    checkArgument(PERCENTAGE.contains(deleteWeight),
        "delete weight must be in range [0.0, 100.0] [%s]", deleteWeight);
    final double sum = readWeight + writeWeight + deleteWeight;
    checkArgument(DoubleMath.fuzzyEquals(sum, 100.0, ERR), "sum of weights must be 100.0 [%s]", sum);

    final RandomSupplier.Builder<Supplier<Request>> wrc = Suppliers.random();
    if (writeWeight > 0.0)
      wrc.withChoice(write, writeWeight);
    if (readWeight > 0.0)
      wrc.withChoice(read, readWeight);
    if (deleteWeight > 0.0)
      wrc.withChoice(delete, deleteWeight);

    this.requestSupplier = wrc.build();
  }

  private boolean allEqual(final double compare, final double... values) {
    for (final double v : values) {
      if (!DoubleMath.fuzzyEquals(v, compare, ERR))
        return false;
    }
    return true;
  }

  @Override
  public Request get() {
    return this.requestSupplier.get().get();
  }
}
