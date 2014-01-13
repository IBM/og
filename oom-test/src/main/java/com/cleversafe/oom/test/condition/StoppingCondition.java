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
// Date: Nov 18, 2013
// ---------------------

package com.cleversafe.oom.test.condition;

/**
 * A test condition that triggers when the test should be stopped.
 */
public interface StoppingCondition
{
   /**
    * Checks if the configured condition has been triggered
    * 
    * @return <code>true</code> if this condition has been triggered
    */
   boolean triggered();
}
