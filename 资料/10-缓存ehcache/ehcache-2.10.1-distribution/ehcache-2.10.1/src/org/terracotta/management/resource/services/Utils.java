/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.management.resource.services;

/**
 * <p>Utility class for management resource services.</p>
 *
 * @author brandony
 */
public class Utils {

  /**
   * <p>A convenience method to prevent adding some massive dependency like commons lang.</p>
   *
   * @param string to trim
   * @return trimmed string or {@code null}
   */
  public static String trimToNull(String string) {
    return string == null || string.trim().length() == 0 ? null : string.trim();
  }
}
