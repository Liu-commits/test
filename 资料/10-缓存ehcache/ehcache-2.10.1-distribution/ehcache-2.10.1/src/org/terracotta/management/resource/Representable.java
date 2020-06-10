/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package org.terracotta.management.resource;

import java.io.Serializable;

/**
 * <p>
 * An interface that identifies a resource representation served from embedded management web services.
 * </p>
 * 
 * @author brandony
 * 
 */
public interface Representable extends Serializable {
  public static final String EMBEDDED_AGENT_ID = "embedded";

  /**
   * <p>
   * Get the identifier for the agent that provided this representable object.
   * </p>
   * 
   * @return
   */
  String getAgentId();

  /**
   * <p>
   * Set the identifier for the agent that provided this representable object.
   * </p>
   * 
   * @param agentId
   */
  void setAgentId(String agentId);
}
