/* Copyright (c) IBM Corporation 2018. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.supplier;

import com.google.common.collect.Maps;
import com.ibm.og.object.LegacyObjectMetadata;
import com.ibm.og.object.ObjectManager;
import com.ibm.og.object.ObjectManagerException;
import com.ibm.og.object.ObjectMetadata;
import com.ibm.og.util.Context;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ObjectRetentionExtensionFunctionTest {
    private ObjectManager objectManager;

    @Before
    public void before() {
        this.objectManager = mock(ObjectManager.class);
    }

    @Test(expected = NullPointerException.class)
    public void nullObjectManager() {
        new DeleteObjectNameFunction(null);
    }

    @Test
    public void ObjectRetentionExtentsionTest() {
        final String objectName = UUID.randomUUID().toString().replace("-", "") + "0000";
        final LegacyObjectMetadata objectMetadata = LegacyObjectMetadata.fromMetadata(
                objectName, 1024, -1, (byte)0, 1000, null);
        final ObjectMetadata object = mock(ObjectMetadata.class);
        when(object.getName()).thenReturn(objectName);
        when(this.objectManager.removeForUpdate()).thenReturn(objectMetadata);

        final Map<String, String> context = Maps.newHashMap();
        assertThat(new ObjectRetentionExtensionFunction(this.objectManager).apply(context), is(objectName));
        assertThat(context.get(Context.X_OG_OBJECT_NAME), is(objectName));
        assertThat(context.get(Context.X_OG_OBJECT_RETENTION), is("1000"));
    }

    @Test(expected = ObjectManagerException.class)
    public void supplierException() {
        when(this.objectManager.removeForUpdate()).thenThrow(new ObjectManagerException());
        new ObjectRetentionExtensionFunction(this.objectManager).apply(Maps.<String, String>newHashMap());
    }

}
