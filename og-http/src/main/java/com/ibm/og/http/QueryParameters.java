/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.http;

/**
 * Custom Object Generator Query Parameter Keys
 *
 * @since 1.0
 */
public class QueryParameters {
    // S3 related query parameters
    public static final String S3_MARKER = "marker";

    // OpenStack related query parameters
    public static final String OPENSTACK_MARKER = "marker";

    // Legalhold related query parameter
    public static final String LEGALHOLD_PARAMETER = "legalHold";

    // Legalhold related query parameter for adding legalhold
    public static final String LEGALHOLD_ADD_PARAMETER = "add";

    // Legalhold related query parameter for deleting legalhold
    public static final String LEGALHOLD_REMOVE_PARAMETER = "remove";


    private QueryParameters() {}
}
