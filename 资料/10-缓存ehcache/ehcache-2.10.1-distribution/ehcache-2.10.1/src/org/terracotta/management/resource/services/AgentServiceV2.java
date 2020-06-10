/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package org.terracotta.management.resource.services;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntityV2;
import org.terracotta.management.resource.AgentMetadataEntityV2;
import org.terracotta.management.resource.ResponseEntityV2;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public interface AgentServiceV2 {

  /**
   * Get a collection of agent entities known by this agent.
   * @param ids a set of IDs. If empty, this means all known agents.
   * @return a ResponseEntityV2
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<AgentEntityV2> getAgents(Set<String> ids) throws ServiceExecutionException;

  /**
   * Get a collection of agent metadata entities known by this agent.
   * @param ids a set of IDs. If empty, this means all known agents.
   * @return a ResponseEntityV2
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<AgentMetadataEntityV2> getAgentsMetadata(Set<String> ids) throws ServiceExecutionException;

}
