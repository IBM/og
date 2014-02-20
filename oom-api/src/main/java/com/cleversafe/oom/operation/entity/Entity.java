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
// Date: Jan 24, 2014
// ---------------------

package com.cleversafe.oom.operation.entity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A container representing a request or response body.
 */
public interface Entity
{
   /**
    * Constructs an input stream for this entity
    * 
    * @return this entity, as an input stream
    */
   InputStream asInputStream() throws IOException;

   /**
    * Gets whether this entity is backed by a file
    * 
    * @return true if this entity is backed by a file, else false
    */
   boolean isFile();

   /**
    * Gets the file for a file backed entity. Callers should first call <code>isFile</code> to
    * determine if this entity is file backed or not.
    * 
    * @return the file for this entity if it is backed by a file, else null
    */
   File getFile();

   /**
    * Gets the size of this entity.
    * 
    * @return the size of this entity, or -1 if the size is not known
    */
   long getSize();
}
