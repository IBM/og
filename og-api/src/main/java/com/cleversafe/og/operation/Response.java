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

package com.cleversafe.og.operation;

import java.util.Iterator;
import java.util.Map.Entry;

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
   int getStatusCode();

   /**
    * Gets the value of the response header with the specified key.
    * 
    * @param key
    *           the key of the header
    * @return the value for the header with the specified key, or null if no such mapping exists
    * @throws NullPointerException
    *            if key is null
    */
   String getHeader(String key);

   /**
    * Returns an iterator over the request headers for this request. The returned iterator must not
    * support remove operations and should throw <code>UnsupportedOperationException</code> instead.
    * 
    * @return a request header iterator
    */
   Iterator<Entry<String, String>> headers();

   /**
    * Gets the description of the entity for this response.
    * 
    * @return the description of the entity for this response
    */
   Entity getEntity();

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
   String getMetadata(Metadata key);

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
   String getMetadata(String key);

   /**
    * Returns an iterator over the request metadata for this request. The returned iterator must not
    * support remove operations and should throw <code>UnsupportedOperationException</code> instead.
    * 
    * @return a request metadata iterator
    */
   Iterator<Entry<String, String>> metadata();
}
