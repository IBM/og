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

    // Object retention extension
    public static final String OBJECT_RETENTION_EXTENSION_PARAMETER = "extendRetention";

    // S3 bucket list api v2
    public static final String S3_START_AFTER = "start-after";

    // S3 bucket list max keys
    public static final String S3_LIST_MAX_KEYS = "max-keys";

    // Object Restore
    public static final String OBJECT_RESTORE_PARAMETER = "restore";

    //Put and Get Bucket Lifecycle
    public static final String BUCKET_LIFECYCLE_PARAMETER = "lifecycle";

    private QueryParameters() {}
}
