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
// Date: Jan 15, 2014
// ---------------------

package com.cleversafe.og.object;


/**
 * A collection of objects, and the requisite services for persisting object names to disk.
 * Implementations of this interface may provide varying policies for how and when objects are
 * stored on disk. Caching and retrieval policies may also differ.
 * 
 * @since 1.0
 */
public interface ObjectManager
{
   /**
    * Selects an existing object name for deletion. Implementations may define whether the returned
    * object name remains available for concurrent reading or not.
    * 
    * @return an available object name for deletion
    */
   ObjectName getNameForDelete();

   /**
    * Selects an existing object name for reading. Implementations may define whether the returned
    * object name remains available for concurrent reading or not.
    * 
    * @return an available object name for reading
    */
   ObjectName acquireNameForRead();

   /**
    * Informs this object manager that the caller is done reading the provided object name.
    * Implementations may use this method as an indication that an object is safe to overwrite,
    * delete, read, etc.
    * 
    * @param objectName
    *           the name of the object that the caller has finished reading
    */
   void releaseNameFromRead(ObjectName objectName);

   /**
    * Informs this object manager that the caller has completed writing an object with the provided
    * object name.
    * 
    * @param objectName
    *           the name of the object that the caller has finished writing
    */
   void writeNameComplete(ObjectName objectName);

   /**
    * Informs this object manager that the test has ended and it should persist any in-memory object
    * names and clean up any important resources.
    * 
    */
   void testComplete();

   /**
    * Calculates the value of the current number of objects that are persisted to disk. This method
    * does not include the count of in-memory only objects.
    * 
    * @return the current number of persisted objects.
    */
   long getSavedObjectCount();
}
