/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;
import com.beust.jcommander.converters.LongConverter;
import com.beust.jcommander.converters.IntegerConverter;
import com.ibm.og.cli.util.IntegerSetConverter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 *  class to hold command line arguments for ObjectGenerator.
 *  @since 1.0
 */

public class ObjectFileGetOpt extends GetOpt {

    @Parameter(names = {"--write", "-w"}, description = "Read plain text source and output in object file format")
    private boolean write;

    @Parameter(names = {"--read", "-r"}, description = "Read object source and output in plain text file format")
    private boolean read;

    @Parameter(names = {"--shuffle"}, description = "Combine, Shuffle to create a new object file from the existing object files")
    private boolean shuffle;

    @Parameter(names = {"--shuffle-object-file-directory"}, description = "path where the object-files for shuffling are located")
    private String objectFilesDir;

    @Parameter(names = {"--shuffle-object-file-prefix"}, description = "vault prefix for selecting object files to be shuffled")
    private String prefix;

    @Parameter(names = {"--shuffle-object-file-max-size"}, description = "Desired number of maximum objects in the shuffle output file")
    private int shuffleMaxObjectFileSize;


    @Parameter(names = {"--filter", "-f"}, description = "Filter an existing object file source and output in object file format")
    private boolean filter;

    @Parameter(names = {"--min-filesize"}, description = "Minimum file size to include an object in the output when using\n" +
            "        --filter, in bytes (default: 0)", converter = LongConverter.class)
    private long minSize = 0;


    //todo: check the range
    @Parameter(names = {"--max-filesize"}, description = "Maximum file size to include an object in the output when using\n" +
            "--filter, in bytes (default: 9223372036854775807)", converter = LongConverter.class)
    private long maxSize = Long.MAX_VALUE;

    @Parameter(names= {"--min-suffix"}, description = "Minimum container suffix to include an object in the output when using\n" +
            "        --filter (default: -1)", converter = IntegerConverter.class)
    private int minSuffix = -1;


    @Parameter(names= {"--max-suffix"}, description = "Maximum container suffix to include an object in the output when using\n" +
            "        --filter (default: 2147483647)", converter = IntegerConverter.class)
    private int maxSuffix = Integer.MAX_VALUE;


    @Parameter(names = {"--container-suffixes"}, description = "List of container suffixes (comma delimited) to include an object in the\n" +
            "        output when using --filter", converter = IntegerSetConverter.class)
    private Set<Integer> containerSuffixes = new TreeSet<Integer>();

    @Parameter(names={"--min-retention"},
            description = "Minimum value for retention in seconds from epoch to include an object in the output when using\n" +
                    "        --filter, in seconds from epoch (default: -1)", converter = IntegerConverter.class)
    private int minRetention = -1;

    @Parameter(names={"--max-retention"},
            description = "Maximum value for retention in seconds from epoch to include an object in the output when using\n" +
                    "        --filter, in seconds from epoch (default: -1)", converter = IntegerConverter.class)
    private int maxRetention = Integer.MAX_VALUE;

    @Parameter(names={"--min-legalholds"},
            description = "Minumum number of legalholds the object has to include an object in the output when using\n" +
                    "        --filter(default: 0)", converter = IntegerConverter.class)
    private int minLegalHolds = 0;

    @Parameter(names={"--max-legalholds"},
            description = "Maximum number of legalholds the object has to include an object in the output when using\n" +
                    "        --filter(default: 100)", converter = IntegerConverter.class)
    private int maxLegalHolds = 100;

    @Parameter(names = {"--upgrade", "-u"}, description = "Upgrade an oom bin file to an og object file")
    private boolean upgrade;

    @Parameter(names = {"--split", "-s"}, description = "Split the output of a --write, --filter, or --split call into one or\n" +
            "        more object files. object file name prefix is determined by --output")
    private boolean split;

    @Parameter(names= {"--split-size"}, description = "Maximum file size to use when using the --split option with --split-size\n" +
            "       (default: -1)", converter = IntegerConverter.class)
    private int splitSize = -1;

    @Parameter(names= {"--output", "-o"}, description = "A relative or absolute path to an output file, rather than stdout")
    private String output;

    @Parameter(description = "A relative or absolute paths to input files")
    private List<String> input = new ArrayList<String>(); // main parameter - currently only input file is expected.

    public boolean getWrite() {
        return write;
    }

    public boolean getRead() {
        return read;
    }

    public boolean getShuffle() {
        return shuffle;
    }

    public String getObjectFilesDir() { return objectFilesDir; }

    public int getShuffleMaxObjectFileSize() { return shuffleMaxObjectFileSize; }

    public String getPrefix() { return  prefix; }

    public boolean getFilter() {
        return filter;
    }

    public long getMinSize() {
        return minSize;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public int getMinSuffix() {
        return minSuffix;
    }

    public int getMaxSuffix() {
        return maxSuffix;
    }

    public Set<Integer> getContainerSuffixes() {
        return containerSuffixes;
    }

    public int getMinRetention() { return minRetention; }

    public int getMaxRetention() { return maxRetention; }

    public int getMinLegalHolds() { return minLegalHolds;}

    public int getMaxLegalHolds() { return maxLegalHolds;}

    public boolean getUpgrade() {
        return upgrade;
    }

    public boolean getSplit() {
        return split;
    }

    public int getSplitSize() {
        return splitSize;
    }

    public String getOutput() {
        return output;
    }

    public List<File> getInput() {
        List<File> files = new ArrayList<File>();
        if (input != null) {
            for (String s:input) {
                File f = new FileConverter().convert(s);
                if (f.exists()) {
                    files.add(f);
                } else {
                    throw new IllegalArgumentException(String.format("File[%s] does not exists", f.getName()));
                }
            }
        }
        return files;
    }

    @Override
    public boolean validate() {
        if (help || version) {
            // if command line contains help or version option, give priority to them
            return true;
        }
        if (!shuffle) {
            // if no input argument stdin is used so check for more than 1 argument
            checkNotNull(input);
            checkArgument(input.size() >= 1, "Invalid command line arguments. Atleast one input file is expected");
        }

        return true;
    }

}
