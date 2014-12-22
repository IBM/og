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
// Date: Jul 11, 2014
// ---------------------

package com.cleversafe.og.json.type;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.util.SizeUnit;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class SizeUnitTypeAdapterTest {
  private SizeUnitTypeAdapter typeAdapter;

  @Before
  public void before() {
    this.typeAdapter = new SizeUnitTypeAdapter();
  }

  @Test
  public void write() throws IOException {
    final JsonWriter writer = mock(JsonWriter.class);
    this.typeAdapter.write(writer, SizeUnit.BYTES);
    verify(writer).value("bytes");
  }

  @Test
  public void read() throws IOException {
    final JsonReader reader = mock(JsonReader.class);
    when(reader.nextString()).thenReturn("b");

    assertThat(this.typeAdapter.read(reader), is(SizeUnit.BYTES));
  }
}
