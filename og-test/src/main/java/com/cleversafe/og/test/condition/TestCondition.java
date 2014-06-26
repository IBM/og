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
// Date: Jun 26, 2014
// ---------------------

package com.cleversafe.og.test.condition;

import com.cleversafe.og.statistic.Statistics;

public interface TestCondition
{
   boolean isTriggered(Statistics stats);
}
