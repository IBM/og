/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.json.type;

import java.io.IOException;
import java.util.Locale;

import com.cleversafe.og.util.SizeUnit;
import com.cleversafe.og.util.Units;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * A type adapter for {@code SizeUnit} that allows for a variety of unit name strings to be used in
 * place of the enum name
 * 
 * @see Units
 * @since 1.0
 */
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
