/*
 * Copyright (C) 2005-2016 Cleversafe, Inc. All rights reserved.
 *
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 *
 * licensing@cleversafe.com
 */

package com.cleversafe.og.cli;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import static com.google.common.base.Preconditions.checkNotNull;


class OGGetOpt extends GetOpt {

    @Parameter(description = "<og_config>", required = true)
    private List<String> arguments = new ArrayList<String>();

    public OGGetOpt() {

    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    public String getOGConfigFileName() {
        checkNotNull(arguments);
        return arguments.get(0);

    }

    public GetOpt getObject() {
        return new OGGetOpt();
    }






}
