/* Copyright (c) IBM Corporation 2023. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json;

/**
 *  A class that is used for object lock legalhold status selection
 *
 * @since 1.14.0
 */
public class LegalHoldStatusSelection {
    public double on = 1.0;
    public double off = 0.0;
    public enum ObjectLegalHoldStatusSelectionChoice {ON, OFF};
}