package com.ibm.og.json;

/**
 * A guice configuration module for object selection for putting object for select query
 *
 * @since 1.15.0
 */

public class WriteSelectBodyConfig {

    public final String filepath;

    public WriteSelectBodyConfig(final String filepath) {
        this.filepath = filepath;
    }

}
