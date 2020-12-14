/* Copyright (c) IBM Corporation 2021. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json;


/**
 *  A class that is used for object version selection for reading the latest version of the object
 *
 * @since 1.12.0
 */

public class ObjectVersionSelection {

  public double versioned = 1.0;
  public double nonVersioned = 0.0;

  public enum ObjectVersionSelectionChoices{VERSIONED, NON_VERSIONED};


}
