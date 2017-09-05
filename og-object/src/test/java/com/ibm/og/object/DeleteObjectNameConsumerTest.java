/* Copyright (c) IBM Corporation 2017. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;


import com.google.common.collect.ImmutableMap;
import com.ibm.og.api.Method;
import com.ibm.og.api.Operation;
import com.ibm.og.util.Context;
import org.junit.Test;

import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeleteObjectNameConsumerTest extends AbstractObjectNameConsumerTest {
    @Override
    public AbstractObjectNameConsumer create(final ObjectManager objectManager,
                                             final Set<Integer> statusCodes) {
        return new DeleteObjectConsumer(objectManager, statusCodes);
    }

    @Override
    public Method method() {
        return Method.DELETE;
    }

    @Override
    public Operation operation() { return Operation.DELETE; }

    @Override
    public void doVerify() {
        verify(this.objectManager).removeUpdatedObject(isA(ObjectMetadata.class));
    }

    @Override
    public void doVerifyNever() {
        verify(this.objectManager, never()).removeUpdatedObject(isA(ObjectMetadata.class));
    }

    @Override
    public void doThrowIt() {
        doThrow(new ObjectManagerException()).when(this.objectManager).removeUpdatedObject(any(ObjectMetadata.class));
    }

    @Test
    public void successfulDelete() {
        // for SOH, the header gets set on response rather than request
        when(this.request.headers()).thenReturn(ImmutableMap.<String, String>of());
        when(this.response.getContext())
                .thenReturn(ImmutableMap.of(Context.X_OG_OBJECT_NAME, this.object));

        this.objectNameConsumer.consume(this.operation);
        doVerify();
    }
}
