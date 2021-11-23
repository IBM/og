/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.test;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.ibm.og.s3.MultipartRequestSupplier;
import com.ibm.og.supplier.RequestSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.og.api.Request;
import com.ibm.og.supplier.RandomSupplier;
import com.ibm.og.supplier.Suppliers;
import com.google.common.base.Supplier;

/**
 * A request manager which provides basic write/read/delete capability
 * 
 * @since 1.0
 */
public class SimpleRequestManager implements RequestManager {
  private static final Logger _logger = LoggerFactory.getLogger(SimpleRequestManager.class);
  private final Supplier<Supplier<Request>> requestSupplier;
  private final MultipartRequestSupplier multipartWriteSupplier;
  private boolean abort = false;

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
      @Named("extend_retention.weight") final double extendRetentionWeight,
      @Named("objectRestore") final Supplier<Request> objectRestore,
      @Named("objectRestore.weight") final double objectRestoreWeight,
      @Named("putContainerLifecycle") final Supplier<Request> putContainerLifecycle,
      @Named("putContainerLifecycle.weight") final double putContainerLifecycleWeight,
      @Named("getContainerLifecycle") final Supplier<Request> getContainerLifecycle,
      @Named("getContainerLifecycle.weight") final double getContainerLifecycleWeight,
      @Named("deleteContainerLifecycle") final Supplier<Request> deleteContainerLifecycle,
      @Named("deleteContainerLifecycle.weight") final double deleteContainerLifecycleWeight,
      @Named("getContainerProtection") final Supplier<Request> getContainerProtection,
      @Named("getContainerProtection.weight") final double getContainerProtectionWeight,
      @Named("putContainerProtection") final Supplier<Request> putContainerProtection,
      @Named("putContainerProtection.weight") final double putContainerProtectionWeight,
      @Named("multiDelete") final Supplier<Request> multiDelete,
      @Named("multiDelete.weight") final double multiDeleteWeight,
      @Named("writeTags") final Supplier<Request> writeTags,
      @Named("writeTags.weight") final double writeTagsWeight,
      @Named("deleteTags") final Supplier<Request> deleteTags,
      @Named("deleteTags.weight") final double deleteTagsWeight,
      @Named("getTags") final Supplier<Request> getTags,
      @Named("getTags.weight") final double getTagsWeight,
      @Named("listObjectVersions") final Supplier<Request> listObjectVersions,
      @Named("listObjectVersions.weight") final double listObjectVersionsWeight){

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
    checkNotNull(writeLegalHold);
    checkNotNull(readLegalhold);
    checkNotNull(deleteLegalhold);
    checkNotNull(extendRetention);
    checkNotNull(objectRestore);
    checkNotNull(putContainerLifecycle);
    checkNotNull(getContainerLifecycle);
    checkNotNull(deleteContainerLifecycle);
    checkNotNull(putContainerProtection);
    checkNotNull(getContainerProtection);
    checkNotNull(writeTags);
    checkNotNull(deleteTags);
    checkNotNull(getTags);
    checkNotNull(listObjectVersions);

    this.multipartWriteSupplier = (MultipartRequestSupplier)writeMultipart;

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
    if (objectRestoreWeight > 0.0) {
      wrc.withChoice(objectRestore, objectRestoreWeight);
    }
    if (putContainerLifecycleWeight > 0.0) {
      wrc.withChoice(putContainerLifecycle, putContainerLifecycleWeight);
    }
    if (getContainerLifecycleWeight > 0.0) {
      wrc.withChoice(getContainerLifecycle, getContainerLifecycleWeight);
    }
    if (deleteContainerLifecycleWeight > 0.0) {
      wrc.withChoice(deleteContainerLifecycle, deleteContainerLifecycleWeight);
    }
    if (putContainerProtectionWeight > 0.0) {
      wrc.withChoice(putContainerProtection, putContainerProtectionWeight);
    }
    if (getContainerProtectionWeight > 0.0) {
      wrc.withChoice(getContainerProtection, getContainerProtectionWeight);
    }
    if (multiDeleteWeight > 0.0) {
      wrc.withChoice(multiDelete, multiDeleteWeight);
    }
    if (writeTagsWeight > 0.0) {
      wrc.withChoice(writeTags, writeTagsWeight);
    }
    if (deleteTagsWeight > 0.0) {
      wrc.withChoice(deleteTags, deleteTagsWeight);
    }
    if (getTagsWeight > 0.0) {
      wrc.withChoice(getTags, getTagsWeight);
    }
    if (listObjectVersionsWeight > 0.0) {
      wrc.withChoice(listObjectVersions, listObjectVersionsWeight);
    }

    this.requestSupplier = wrc.build();
  }

  @Override
  public Request get() {
    final Request request;
    if (!abort) {
      request = this.requestSupplier.get().get();
    } else {
      // currently only multipart uploads need to be aborted
      request = this.multipartWriteSupplier.get();
      if (request == null) {
        // done aborting all multipart uploads
        throw new NoMoreRequestsException();
      }
    }
    return request;
  }

  public void setAbort(boolean abort) {
    this.abort = abort;
    this.multipartWriteSupplier.abortSessions();
  }

  public void setShutdownImmediate(boolean shutdown) {
    //this.abort = abort;
    this.multipartWriteSupplier.shutdownImmediate(shutdown);
  }

  @Override
  public String toString() {
    return String.format("SimpleRequestManager [%n" + "requestSupplier=%s%n" + "]",
        this.requestSupplier);
  }
}
