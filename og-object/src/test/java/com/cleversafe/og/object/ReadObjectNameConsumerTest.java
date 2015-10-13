/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.object;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Set;

import com.cleversafe.og.api.Method;

public class ReadObjectNameConsumerTest extends AbstractObjectNameConsumerTest {
  @Override
  public AbstractObjectNameConsumer create(final ObjectManager objectManager,
      final Set<Integer> statusCodes) {
    return new ReadObjectNameConsumer(objectManager, statusCodes);
  }

  @Override
  public Method method() {
    return Method.GET;
  }

  @Override
  public void doVerify() {
    verify(this.objectManager).getComplete(isA(ObjectMetadata.class));
  }

  @Override
  public void doVerifyNever() {
    verify(this.objectManager, never()).getComplete(isA(ObjectMetadata.class));
  }

  @Override
  public void doThrowIt() {
    doThrow(new ObjectManagerException()).when(this.objectManager)
        .getComplete(any(ObjectMetadata.class));
  }
}
