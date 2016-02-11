/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.object.ObjectManager;
import com.cleversafe.og.object.ObjectManagerException;
import com.cleversafe.og.object.ObjectMetadata;
import com.cleversafe.og.util.Context;
import com.google.common.collect.Maps;

public class DeleteObjectNameFunctionTest {
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
  public void deleteObjectNameSupplier() {
    final String object = "objectName";
    final ObjectMetadata objectName = mock(ObjectMetadata.class);
    when(objectName.getName()).thenReturn(object);
    when(this.objectManager.remove()).thenReturn(objectName);

    final Map<String, String> context = Maps.newHashMap();
    assertThat(new DeleteObjectNameFunction(this.objectManager).apply(context), is(object));
    assertThat(context.get(Context.X_OG_OBJECT_NAME), is(object));
  }

  @Test(expected = ObjectManagerException.class)
  public void supplierException() {
    when(this.objectManager.remove()).thenThrow(new ObjectManagerException());
    new DeleteObjectNameFunction(this.objectManager).apply(Maps.<String, String>newHashMap());
  }
}
