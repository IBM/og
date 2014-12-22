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
// Date: Jun 28, 2014
// ---------------------

package com.cleversafe.og.json.type;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.cleversafe.og.util.Units;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class TimeUnitTypeAdapter extends TypeAdapter<TimeUnit> {
  @Override
  public void write(final JsonWriter out, final TimeUnit value) throws IOException {
    out.value(value.toString().toLowerCase(Locale.US));
  }

  @Override
  public TimeUnit read(final JsonReader in) throws IOException {
    return Units.time(in.nextString());
  }
}
