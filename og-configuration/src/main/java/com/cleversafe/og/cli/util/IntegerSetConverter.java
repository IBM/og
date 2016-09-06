package com.cleversafe.og.cli.util;

import com.beust.jcommander.IStringConverter;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *   Utility class to convert the suffix parameters to a set of Integers
 *
 */
public class IntegerSetConverter implements IStringConverter<Set<Integer>> {

    public Set<Integer> convert(String value) {
        SortedSet<Integer> set = new TreeSet<Integer>();
        String[] values = value.split(",");
        for (String num : values) {
            set.add(Integer.parseInt(num));
        }
        return ImmutableSet.copyOf(set);
    }
}
