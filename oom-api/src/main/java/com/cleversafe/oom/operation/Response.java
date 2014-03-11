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
// Date: Feb 21, 2014
// ---------------------

package com.cleversafe.oom.operation;

import java.util.Iterator;

import com.cleversafe.oom.api.Header;
import com.cleversafe.oom.api.MetaDataEntry;

/**
 * An object that describes an http response.
 */
public interface Response
{
   /**
    * Gets the id for the request associated with this response.
    * 
    * @return the id of the associated request
    */
   long getRequestId();

   /**
    * Gets the status code for this response.
    * 
    * @return the status code for this response
    */
   public int getStatusCode();

   /**
    * Gets the value of the response header with the specified key.
    * 
    * @param key
    *           the key of the header
    * @return the value for the header with the specified key, or null if no such mapping exists
    * @throws NullPointerException
    *            if key is null
    */
   public String getHeader(String key);

   /**
    * Returns an iterator over the response headers for this response.
    * 
    * @return a response header iterator
    */
   public Iterator<Header> headers();

   /**
    * Gets the value of the response metadata entry with the specified key.
    * 
    * @param key
    *           the key of the metadata entry
    * @return the value for the metadata entry with the specified key, or null if no such mapping
    *         exists
    * @throws NullPointerException
    *            if key is null
    */
   public String getMetaDataEntry(String key);

   /**
    * Returns an iterator over the response metadata for this response.
    * 
    * @return a response metadata iterator
    */
   public Iterator<MetaDataEntry> metaData();
}
