/*
 * Copyright (c) IBM Corporation 2020. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Comparator;
import java.util.regex.Pattern;

public class ObjectManagerUtils {

  public static class ObjectFileNameIndexComparator<K extends File> implements Comparator<K> {

    private final String prefix;

    public ObjectFileNameIndexComparator(final String prefix) {
      this.prefix = prefix;
    }
    @Override
    public int compare(K k1, K k2) {
      String fn1 = k1.getName();
      String[] parts = fn1.split("\\.");
      String name = parts[0];
      int index1 = Integer.parseInt(name.substring(prefix.length()));

      String fn2 = k2.getName();
      parts = fn2.split("\\.");
      name = parts[0];
      int index2 = Integer.parseInt(name.substring(prefix.length()));;
      return index1 - index2;
    }
  }

  public static int getFileIndex(String prefix, String fileName) {
    String[] parts = fileName.split("\\.");
    String name = parts[0];
    int index = Integer.parseInt(name.substring(prefix.length()));
    return index;
  }

  public static class IdFilter implements FilenameFilter {
    final String prefix;
    final String suffix;

    public IdFilter(String prefix, String suffix) {
      this.prefix = prefix;
      this.suffix = suffix;
    }

    @Override
    public boolean accept(final File dir, final String name) {
      final Pattern filenamePattern = Pattern
              .compile(String.format("%s(\\d|[1-9]\\d*)%s", this.prefix, this.suffix));
      return filenamePattern.matcher(name).matches();
    }

  }

  public static File[] getIdFiles(String prefix, String suffix, String directory) {
    final File dir = new File(directory);
    return dir.listFiles(new ObjectManagerUtils.IdFilter(prefix, suffix));

  }


}
