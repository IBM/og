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

import java.net.URI;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * An object that describes an http request.
 */
public interface Request
{
   /**
    * Gets the id for this request. An id should uniquely identify this request relative to any past
    * or future requests.
    * 
    * @return the id of this request
    */
   long getId();

   /**
    * Gets the http method for this request.
    * 
    * @return the http method for this request
    * @see Method
    */
   Method getMethod();

   /**
    * Gets the uri for this request.
    * 
    * @return the uri for this request
    */
   URI getURI();

   /**
    * Gets the value of the request header with the specified key.
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
    * Gets the description of the entity for this request.
    * 
    * @return the description of the entity for this request
    */
   Entity getEntity();

   /**
    * Gets the value of the request metadata entry with the specified key.
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
    * Gets the value of the request metadata entry with the specified key.
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
