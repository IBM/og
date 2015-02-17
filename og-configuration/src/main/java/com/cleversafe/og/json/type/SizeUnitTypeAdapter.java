/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json.type;

import java.io.IOException;
import java.util.Locale;

import com.cleversafe.og.util.SizeUnit;
import com.cleversafe.og.util.Units;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class SizeUnitTypeAdapter extends TypeAdapter<SizeUnit> {
  @Override
  public void write(final JsonWriter out, final SizeUnit value) throws IOException {
    out.value(value.toString().toLowerCase(Locale.US));
  }

  @Override
  public SizeUnit read(final JsonReader in) throws IOException {
    return Units.size(in.nextString());
  }
}
