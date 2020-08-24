package com.ibm.og.test;

public class NoMoreRequestsException extends RuntimeException {

  public NoMoreRequestsException() {}

  public NoMoreRequestsException(final String message) {
    super(message);
  }

  public NoMoreRequestsException(final Throwable cause) {
    super(cause);
  }

  public NoMoreRequestsException(final String message, final Throwable cause) {
    super(message, cause);
  }

}
