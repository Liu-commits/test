/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.management.resource;

import org.terracotta.management.resource.exceptions.ExceptionUtils;

public class ExceptionEntityV2 extends AbstractEntityV2 {
  private String message;
  private String stackTrace;

  public ExceptionEntityV2() {
  }

  public ExceptionEntityV2(Throwable t) {
    ErrorEntity errorEntity = ExceptionUtils.toErrorEntity(t);
    this.message = errorEntity.getError();
    this.stackTrace = errorEntity.getStackTrace();
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getStackTrace() {
    return stackTrace;
  }

  public void setStackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
  }

}
