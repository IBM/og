/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.ibm.og.util.Context;
import org.junit.Before;
import org.junit.Test;

import com.ibm.og.object.ObjectManager;
import com.ibm.og.object.ObjectManagerException;
import com.ibm.og.object.ObjectMetadata;
import com.google.common.collect.Maps;

public class ReadObjectNameFunctionTest {
  private ObjectManager objectManager;

  @Before
  public void before() {
    this.objectManager = mock(ObjectManager.class);
  }

  @Test(expected = NullPointerException.class)
  public void nullObjectManager() {
    new ReadObjectNameFunction(null);
  }

  @Test
  public void readObjectNameSupplier() {
    final String object = "objectName";
    final ObjectMetadata objectName = mock(ObjectMetadata.class);
    when(objectName.getName()).thenReturn(object);
    when(this.objectManager.get()).thenReturn(objectName);

    final Map<String, String> context = Maps.newHashMap();
    assertThat(new ReadObjectNameFunction(this.objectManager).apply(context), is(object));
    assertThat(context.get(Context.X_OG_OBJECT_NAME), is(object));
  }

  @Test(expected = ObjectManagerException.class)
  public void supplierException() {
    when(this.objectManager.get()).thenThrow(new ObjectManagerException());
    new ReadObjectNameFunction(this.objectManager).apply(Maps.<String, String>newHashMap());
  }
}
