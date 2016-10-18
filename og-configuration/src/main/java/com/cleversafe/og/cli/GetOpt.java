/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;


/**
 *  class to hold command line arguments.  This class can be extended to add additional command line options and
 *
 *
 * @since 1.0
 */
public class GetOpt {
    @Parameter(names = {"--version", "-v"}, description = "Prints the og version and exits")
    protected boolean version = false;

    @Parameter(names = {"--help", "-h"}, description = "Prints this help and exits")
    protected boolean help = false;

    public boolean getVersion() {
        return version;
    }

    public boolean getHelp() {
        return help;
    }


    public GetOpt() {

    }


    /**
     * Checks the validity of the arguments. The application specific logic to
     * validate the various combination of the arguments etc.
     *
     * @return boolean  true if arguments are valid false otherwise
     * @throws RuntimeException if arguments are not valid
     */
    public boolean validate() {
        return true;
    }



}
