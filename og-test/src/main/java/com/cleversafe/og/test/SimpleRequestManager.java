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

/**
 * A request manager which provides basic write/read/delete capability
 * 
 * @since 1.0
 */
public class SimpleRequestManager implements RequestManager {
  private static final Logger _logger = LoggerFactory.getLogger(SimpleRequestManager.class);
  private static final Range<Double> PERCENTAGE = Range.closed(0.0, 100.0);
  private static final double ERR = Math.pow(0.1, 6);
  private final Supplier<Supplier<Request>> requestSupplier;

  /**
   * Creates an instance. This manager determines which type of request to generate based on the
   * provided weights for each request type.
   * 
   * @param write a supplier of write requests
   * @param writeWeight percentage of the time that a write request should be generated
   * @param read a supplier of read requests
   * @param readWeight percentage of the time that a read request should be generated
   * @param delete a supplier of delete requests
   * @param deleteWeight percentage of the time that a delete request should be generated
   * @param metadata a supplier of metadata (HEAD) requests
   * @param metadataWeight percentage of the time that a metadata (HEAD) request should be generated
   * @throws NullPointerException if write, read, or delete are null
   * @throws IllegalArgumentException if writeWeight, readWeight, or deleteWeight are not in the
   *         range [0.0, 100.0], or if the sum of the individual weights is not 100.0
   */
  @Inject
  @Singleton
  public SimpleRequestManager(@Named("write") final Supplier<Request> write,
      @Named("write.weight") final double writeWeight, @Named("read") final Supplier<Request> read,
      @Named("read.weight") final double readWeight,
      @Named("delete") final Supplier<Request> delete,
      @Named("delete.weight") final double deleteWeight, @Named("metadata") final Supplier<Request> metadata,
      @Named("metadata.weight") final double metadataWeight,
      @Named("overwrite") final Supplier<Request> overwrite,
      @Named("overwrite.weight") final double overwriteWeight,
      @Named("list") final Supplier<Request> list,
      @Named("list.weight") final double listWeight){

    checkNotNull(write);
    checkNotNull(read);
    checkNotNull(delete);
    checkNotNull(metadata);
    checkNotNull(overwrite);
    checkNotNull(list);

    checkArgument(PERCENTAGE.contains(writeWeight),
        "write weight must be in range [0.0, 100.0] [%s]", writeWeight);
    checkArgument(PERCENTAGE.contains(readWeight), "read weight must be in range [0.0, 100.0] [%s]",
        readWeight);
    checkArgument(PERCENTAGE.contains(deleteWeight),
        "delete weight must be in range [0.0, 100.0] [%s]", deleteWeight);
    checkArgument(PERCENTAGE.contains(metadataWeight),
        "delete weight must be in range [0.0, 100.0] [%s]", metadataWeight);
    checkArgument(PERCENTAGE.contains(overwriteWeight),
        "overwrite weight must be in range [0.0, 100.0] [%s]", overwriteWeight);
    checkArgument(PERCENTAGE.contains(listWeight),
        "list weight must be in range [0.0, 100.0] [%s]", listWeight);
    final double sum = writeWeight + readWeight + deleteWeight +
        metadataWeight + overwriteWeight + listWeight;
    checkArgument(DoubleMath.fuzzyEquals(sum, 100.0, ERR), "sum of weights must be 100.0 [%s]",
        sum);

    final RandomSupplier.Builder<Supplier<Request>> wrc = Suppliers.random();
    if (writeWeight > 0.0) {
      wrc.withChoice(write, writeWeight);
    }
    if (readWeight > 0.0) {
      wrc.withChoice(read, readWeight);
    }
    if (deleteWeight > 0.0) {
      wrc.withChoice(delete, deleteWeight);
    }
    if (metadataWeight > 0.0) {
      wrc.withChoice(metadata, metadataWeight);
    }
    if (overwriteWeight > 0.0) {
      wrc.withChoice(overwrite, overwriteWeight);
    }
    if (listWeight > 0.0) {
      wrc.withChoice(list, listWeight);
    }

    this.requestSupplier = wrc.build();
  }

  @Override
  public Request get() {
    final Request request = this.requestSupplier.get().get();
    return request;
  }

  @Override
  public String toString() {
    return String.format("SimpleRequestManager [%n" + "requestSupplier=%s%n" + "]",
        this.requestSupplier);
  }
}
