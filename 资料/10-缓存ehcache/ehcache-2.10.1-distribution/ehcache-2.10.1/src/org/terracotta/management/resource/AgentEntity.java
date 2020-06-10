/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.management.resource;

import java.util.HashMap;
import java.util.Map;


/**
 * <p>
 * Represents an agent for some monitorable application. It provides access to the root {@link Representable} objects 
 * in an appropriate state as determined by the resource providing it.
 * </p>
 * 
 * @author brandony
 * 
 */
public class AgentEntity extends VersionedEntity {
  private String agentId;

  private String agencyOf;

  private final Map<String, Object> rootRepresentables = new HashMap<String, Object>();

  /**
   * @return the agentId
   */
  @Override
  public String getAgentId() {
    return agentId;
  }

  /**
   * @param agentId to set
   */
  @Override
  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  public String getAgencyOf() {
    return agencyOf;
  }

  public void setAgencyOf(String agencyOf) {
    this.agencyOf = agencyOf;
  }

  public Map<String, Object> getRootRepresentables() {
    return rootRepresentables;
  }

}
