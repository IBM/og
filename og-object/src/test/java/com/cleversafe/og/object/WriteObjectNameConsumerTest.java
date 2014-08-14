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
// Date: Jun 29, 2014
// ---------------------

package com.cleversafe.og.object;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;

import com.cleversafe.og.api.Metadata;
import com.cleversafe.og.api.Method;

public class WriteObjectNameConsumerTest extends AbstractObjectNameConsumerTest
{
   @Override
   public ObjectNameConsumer create(
         final ObjectManager objectManager,
         final List<Integer> statusCodes)
   {
      return new WriteObjectNameConsumer(objectManager, statusCodes);
   }

   @Override
   public Method method()
   {
      return Method.PUT;
   }

   @Override
   public void doVerify()
   {
      verify(this.objectManager).writeNameComplete(isA(ObjectName.class));
   }

   @Override
   public void doVerifyNever()
   {
      verify(this.objectManager, never()).writeNameComplete(isA(ObjectName.class));
   }

   @Override
   public void doThrowIt()
   {
      doThrow(new ObjectManagerException()).when(this.objectManager).writeNameComplete(
            any(ObjectName.class));
   }

   @Test
   public void successfulSOH()
   {
      // for SOH, the metadata gets set on response rather than request
      when(this.request.getMetadata(Metadata.OBJECT_NAME)).thenReturn(null);
      when(this.response.getMetadata(Metadata.OBJECT_NAME)).thenReturn(this.object);

      this.objectNameConsumer.consume(this.operation);
      doVerify();
   }
}
