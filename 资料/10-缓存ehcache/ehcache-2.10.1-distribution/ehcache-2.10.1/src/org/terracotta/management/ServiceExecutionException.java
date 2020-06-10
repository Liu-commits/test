/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package org.terracotta.management;

/**
 * A generic service exception class for management service components that are leveraged by application actions.
 * Exceptions should include a message that could be presented to a user.
 *
 * @author brandony
 */
public class ServiceExecutionException extends Exception {
  public ServiceExecutionException() {
    super();
  }

  public ServiceExecutionException(String message) {
    super(message);
  }

  public ServiceExecutionException(String message,
                                   Throwable cause) {
    super(message, cause);
  }

  public ServiceExecutionException(Throwable cause) {
    super(cause);
  }
}
