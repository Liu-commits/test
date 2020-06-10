/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.management.resource.exceptions;

/**
 * Common REST exception class.
 *
 * @author Ludovic Orban
 */
public class ResourceRuntimeException extends RuntimeException {

  private final int statusCode;

  public ResourceRuntimeException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  public ResourceRuntimeException(String message, Throwable t, int statusCode) {
    super(message, t);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
