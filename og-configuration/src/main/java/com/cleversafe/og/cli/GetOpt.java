/*
 * Copyright (C) 2005-2016 Cleversafe, Inc. All rights reserved.
 *
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 *
 * licensing@cleversafe.com
 */

package com.cleversafe.og.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;


public class GetOpt {
    @Parameter(names = {"--version", "-v"}, description = "Prints the og version and exits")
    private boolean version = false;

    @Parameter(names = {"--help", "-h"}, description = "Prints this help and exits")
    private boolean help;

    private boolean error;

    private String errorMsg = "";

    private JCommander jc;

    public Boolean getVersion() {
        return version;
    }

    public void setVersion(Boolean version) {
        this.version = version;
    }

    public Boolean getHelp() {
        return help;
    }

    public void setHelp(Boolean help) {
        this.help = help;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }



    public void processArguments (String progName,  String args[]) {

        try {
            jc = new JCommander(this);
            jc.setProgramName(progName);
            jc.parse(args);
        } catch(RuntimeException re) {
            // record error in the state to match with the existing semantics
            error = true;
            errorMsg = re.getLocalizedMessage();

        }

    }

    public void usage(StringBuilder sb) {
        jc.usage(sb);
    }


}
