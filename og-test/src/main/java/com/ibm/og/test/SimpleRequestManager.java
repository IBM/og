/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.test;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.og.api.Request;
import com.ibm.og.supplier.RandomSupplier;
import com.ibm.og.supplier.Suppliers;
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
  public SimpleRequestManager(
      @Named("write") final Supplier<Request> write,
      @Named("write.weight") final double writeWeight,
      @Named("overwrite") final Supplier<Request> overwrite,
      @Named("overwrite.weight") final double overwriteWeight,
      @Named("read") final Supplier<Request> read,
      @Named("read.weight") final double readWeight,
      @Named("metadata") final Supplier<Request> metadata,
      @Named("metadata.weight") final double metadataWeight,
      @Named("delete") final Supplier<Request> delete,
      @Named("delete.weight") final double deleteWeight,
      @Named("list") final Supplier<Request> list,
      @Named("list.weight") final double listWeight,
      @Named("containerList") final Supplier<Request> containerList,
      @Named("containerList.weight") final double containerListWeight,
      @Named("containerCreate") final Supplier<Request> containerCreate,
      @Named("containerCreate.weight") final double containerCreateWeight,
      @Named("multipartWrite") final Supplier<Request> writeMultipart,
      @Named("multipartWrite.weight") final double writeMultipartWeight,
      @Named("writeCopy") final Supplier<Request> writeCopy,
      @Named("writeCopy.weight") final double writeCopyWeight,
      @Named("write_legalhold") final Supplier<Request> writeLegalHold,
      @Named("write_legalhold.weight") final double writeLegalHoldWeight,
      @Named("read_legalhold") final Supplier<Request> readLegalhold,
      @Named("read_legalhold.weight") final double readLegalholdWeight,
      @Named("delete_legalhold") final Supplier<Request> deleteLegalhold,
      @Named("delete_legalhold.weight") final double deleteLegalholdWeight,
      @Named("extend_retention") final Supplier<Request> extendRetention,
      @Named("extend_retention.weight") final double extendRetentionWeight){


    checkNotNull(write);
    checkNotNull(overwrite);
    checkNotNull(read);
    checkNotNull(metadata);
    checkNotNull(delete);
    checkNotNull(list);
    checkNotNull(containerList);
    checkNotNull(containerCreate);
    checkNotNull(writeMultipart);
    checkNotNull(writeCopy);

    final RandomSupplier.Builder<Supplier<Request>> wrc = Suppliers.random();
    if (writeWeight > 0.0) {
      wrc.withChoice(write, writeWeight);
    }
    if (overwriteWeight > 0.0) {
      wrc.withChoice(overwrite, overwriteWeight);
    }
    if (readWeight > 0.0) {
      wrc.withChoice(read, readWeight);
    }
    if (metadataWeight > 0.0) {
      wrc.withChoice(metadata, metadataWeight);
    }
    if (deleteWeight > 0.0) {
      wrc.withChoice(delete, deleteWeight);
    }
    if (listWeight > 0.0) {
      wrc.withChoice(list, listWeight);
    }
    if (containerListWeight > 0.0) {
      wrc.withChoice(containerList, containerListWeight);
    }
    if (containerCreateWeight > 0.0) {
      wrc.withChoice(containerCreate, containerCreateWeight);
    }
    if (writeMultipartWeight > 0.0) {
      wrc.withChoice(writeMultipart, writeMultipartWeight);
    }
    if (writeCopyWeight > 0.0){
      wrc.withChoice(writeCopy, writeCopyWeight);
    }
    if (writeLegalHoldWeight > 0.0) {
      wrc.withChoice(writeLegalHold, writeLegalHoldWeight);
    }
    if (readLegalholdWeight > 0.0) {
      wrc.withChoice(readLegalhold, readLegalholdWeight);
    }
    if (deleteLegalholdWeight > 0.0) {
      wrc.withChoice(deleteLegalhold, deleteLegalholdWeight);
    }
    if (extendRetentionWeight > 0.0) {
      wrc.withChoice(extendRetention, extendRetentionWeight);
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
