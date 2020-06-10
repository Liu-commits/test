/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.management.resource.events;

import java.util.HashMap;
import java.util.Map;

import org.terracotta.management.resource.AbstractEntityV2;

/**
 * A {@link org.terracotta.management.resource.AbstractEntityV2} representing an event
 * from the management API.
 *
 * @author Ludovic Orban
 */
public class EventEntityV2 extends AbstractEntityV2 {

  private String type;
  private String apiVersion = VERSION_V2;
  private final Map<String, Object> rootRepresentables = new HashMap<String, Object>();

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public Map<String, Object> getRootRepresentables() {
    return rootRepresentables;
  }

}
