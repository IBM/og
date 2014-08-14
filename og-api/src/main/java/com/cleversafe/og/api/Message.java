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

package com.cleversafe.og.api;

import java.util.Iterator;
import java.util.Map.Entry;

/**
 * An object that describes an http message
 * 
 * @since 1.0
 */
public interface Message
{
   /**
    * Gets the value of the message header with the specified key
    * 
    * @param key
    *           the key of the header
    * @return the value for the header with the specified key, or null if no such mapping exists
    */
   String getHeader(String key);

   /**
    * Returns an iterator over the message headers for this message
    * 
    * @return a message header iterator
    */
   Iterator<Entry<String, String>> headers();

   /**
    * Gets the description of the body for this message
    * 
    * @return the description of the body for this message
    */
   Body getBody();

   /**
    * Gets the value of the message metadata entry with the specified key
    * 
    * @param key
    *           the key of the metadata entry
    * @return the value for the metadata entry with the specified key, or null if no such mapping
    *         exists
    */
   String getMetadata(Metadata key);

   /**
    * Gets the value of the message metadata entry with the specified key
    * 
    * @param key
    *           the key of the metadata entry
    * @return the value for the metadata entry with the specified key, or null if no such mapping
    *         exists
    */
   String getMetadata(String key);

   /**
    * Returns an iterator over the message metadata for this message
    * 
    * @return a message metadata iterator
    */
   Iterator<Entry<String, String>> metadata();
}
