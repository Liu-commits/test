/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package org.terracotta.management.resource;

import java.io.Serializable;

/**
 * An error representation.
 * @author Ludovic Orban
 */
public class ErrorEntity implements Serializable {
  private String error;
  private String details;
  private String stackTrace;

  public ErrorEntity() {
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getStackTrace() {
    return stackTrace;
  }

  public void setStackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
  }

  public String toJSON() {
    return String.format("{\"error\" : \"%s\" , \"details\" : \"%s\" , \"stackTrace\" : \"%s\"}", error, details, stackTrace);
  }

}
