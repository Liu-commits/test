/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package org.terracotta.management.embedded;

import javax.servlet.Filter;

/**
 * @author brandony
 */
public final class FilterDetail {
  public static final String[] SECURITY_DISPATCHERS = new String[]{"REQUEST", "FORWARD", "INCLUDE", "ERROR"};

  private final String pathSpec;

  private final String[] dispatcherNames;

  private final Filter filter;

  public FilterDetail(Filter filter,
                      String pathSpec) {
    this(filter, pathSpec, SECURITY_DISPATCHERS);
  }

  public FilterDetail(Filter filter,
                      String pathSpec,
                      String... dispatchNames) {
    this.filter = filter;
    this.pathSpec = pathSpec;
    this.dispatcherNames = dispatchNames;
  }

  public String getPathSpec() {
    return pathSpec;
  }

  public String[] getDispatcherNames() {
    return dispatcherNames;
  }

  public Filter getFilter() {
    return filter;
  }
}
