/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.management.resource.exceptions;

/**
 * Misc. utility methods that work on exceptions.
 *
 * @author Ludovic Orban
 */
public class ExceptionUtilsV1 extends ExceptionUtils {

  // /**
  // * Convert a throwable to an ErrorEntity.
  // * @param t the throwable.
  // * @return the ErrorEntity describing the throwable.
  // */
  // public static ErrorEntity toErrorEntity(Throwable t) {
  // String errorMessage = "";
  // String stackTrace = null;
  // if (t != null) {
  // String message = t.getMessage();
  // errorMessage = message == null ? "" : message.replace('\"', '\'');
  //
  // StringWriter sw = new StringWriter();
  // PrintWriter pw = new PrintWriter(sw);
  // t.printStackTrace(pw);
  // pw.close();
  // stackTrace = sw.toString();
  // }
  //
  // String extraErrorMessage = "";
  // Throwable rootCause = getRootCause(t);
  // if (rootCause != t && rootCause != null && rootCause.getMessage() != null) {
  // extraErrorMessage = rootCause.getMessage().replace('\"', '\'');
  // }
  //
  // ErrorEntity errorEntity = new ErrorEntity();
  // errorEntity.setError(errorMessage);
  // errorEntity.setDetails(extraErrorMessage);
  // errorEntity.setStackTrace(stackTrace);
  // return errorEntity;
  // }

}
