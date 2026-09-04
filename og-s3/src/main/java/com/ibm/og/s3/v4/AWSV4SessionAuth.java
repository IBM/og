/* Copyright (c) IBM Corporation 2026. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.s3.v4;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import javax.inject.Named;

import org.checkerframework.checker.units.qual.A;

import com.google.common.cache.CacheBuilder;
import com.ibm.og.api.AuthenticatedRequest;
import com.ibm.og.api.DataType;
import com.ibm.og.api.Request;
import com.ibm.og.http.AuthenticatedHttpRequest;
import com.ibm.og.util.Context;

public class AWSV4SessionAuth extends AWSV4Auth {

    @Inject
    public AWSV4SessionAuth(@Named("authentication.awsChunked") final boolean chunkedEncoding,
      @Named("authentication.awsCacheSize") final int cacheSize, final DataType data) {
        super(chunkedEncoding, cacheSize, data);
    }

    @Override
    public AuthenticatedRequest authenticate(final Request request) {
        checkNotNull(request);
        final AuthenticatedHttpRequest authenticatedRequest = (AuthenticatedHttpRequest) super.authenticate(request);

        // set session token
        final String sessionToken = checkNotNull(request.getContext().get(Context.X_OG_SESSION_TOKEN));
        authenticatedRequest.addHeader("x-amz-security-token", sessionToken);

        return authenticatedRequest;
    }

}
