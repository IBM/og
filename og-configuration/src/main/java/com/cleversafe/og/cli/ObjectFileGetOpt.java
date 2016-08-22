/*
 * Copyright (C) 2005-2016 Cleversafe, Inc. All rights reserved.
 *
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 *
 * licensing@cleversafe.com
 */

package com.cleversafe.og.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.LongConverter;
import com.beust.jcommander.converters.IntegerConverter;
import com.beust.jcommander.converters.CommaParameterSplitter;

import java.io.File;
import java.util.List;


public class ObjectFileGetOpt extends GetOpt {

    @Parameter(names = {"--write", "-w"}, description = "Write plain text source and output in object file format")
    private boolean write;

    @Parameter(names = {"--read", "-r"}, description = "Read object source and output in plain text file format")
    private boolean read;

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
            "        output when using --filter")
    private String containerSuffixes;


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

    @Parameter(description = "A relative or absolute path to an input file, rather than stdin")
    private List<String> input;

    public boolean getWrite() {
        return write;
    }

    public void setWrite(boolean write) {
        this.write = write;
    }

    public boolean getRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean getFilter() {
        return filter;
    }

    public void setFilter(boolean filter) {
        this.filter = filter;
    }

    public long getMinSize() {
        return minSize;
    }

    public void setMinSize(long minSize) {
        this.minSize = minSize;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public int getMinSuffix() {
        return minSuffix;
    }

    public void setMinSuffix(int minSuffix) {
        this.minSuffix = minSuffix;
    }

    public int getMaxSuffix() {
        return maxSuffix;
    }

    public void setMaxSuffix(int maxSuffix) {
        this.maxSuffix = maxSuffix;
    }

    public int[] getContainerSuffixes() {

        if (containerSuffixes == null) {
            return new int[0];
        }
        List<String> csfx = new CommaParameterSplitter().split(containerSuffixes);
        if (csfx != null && csfx.size() > 0) {
            int[] iArray = new int[csfx.size()];
            int i = 0;
            for (String s : csfx) {
                iArray[i++] = Integer.parseInt(s);
            }
            return iArray;
        } else {
            return new int[0];
        }

    }

    public void setContainerSuffixes(String containerSuffixes) {
        this.containerSuffixes = containerSuffixes;
    }

    public boolean getUpgrade() {
        return upgrade;
    }

    public void setUpgrade(boolean upgrade) {
        this.upgrade = upgrade;
    }

    public boolean getSplit() {
        return split;
    }

    public void setSplit(boolean split) {
        this.split = split;
    }

    public int getSplitSize() {
        return splitSize;
    }

    public void setSplitSize(int splitSize) {
        this.splitSize = splitSize;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public File getInput() {
        if(input != null && input.size() > 0) {
            // pick the entry at first index, create a File object and return
            File f = new File(input.get(0));
            return f;
        } else {
            return null;
        }
    }

    public void setInput(List<String> input) {
        this.input = input;
    }

}
