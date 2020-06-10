/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/

package org.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.AgentMetadataEntity;
import org.terracotta.management.resource.Representable;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.AgentService;
import org.terracotta.management.resource.services.validator.RequestValidator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * An embedded implementation of {@link org.terracotta.management.resource.services.AgentsResourceService}.
 * <p/>
 *
 * @author brandony
 */
@Path("/agents")
public final class AgentsResourceServiceImpl {
  private static final Logger LOG = LoggerFactory.getLogger(AgentsResourceServiceImpl.class);

  private final AgentService agentService;

  private final RequestValidator validator;

  public AgentsResourceServiceImpl() {
    this.agentService = ServiceLocator.locate(AgentService.class);
    this.validator = ServiceLocator.locate(RequestValidator.class);
  }

  /**
   * <p>
   * A top level resource that provides each agents root {@link Representable} objects.
   * </p>
   * 
   * @param info
   *          - {@link UriInfo} for this resource request
   * @return a collection of {@link AgentEntity} objects when successful.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<AgentEntity> getAgents(@Context UriInfo info) {
    LOG.debug(String.format("Invoking AgentsResourceServiceImpl.getAgents: %s", info.getRequestUri()));

    String ids = info.getPathSegments().get(0).getMatrixParameters().getFirst("ids");
    Set<String> idSet;
    if (ids == null) {
      idSet = Collections.emptySet();
    } else {
      idSet = new HashSet<String>(Arrays.asList(ids.split(",")));
    }

    try {
      return agentService.getAgents(idSet);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get agents", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  /**
   * <p>
   * A resource that provides discovery of all agents and reveals metadata about each agents API and state.
   * <p>
   * 
   * @param info
   *          - {@link UriInfo} for this resource request
   * @return a collection of {@link AgentMetadataEntity} objects when successful.
   */
  @GET
  @Path("/info")
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<AgentMetadataEntity> getAgentsMetadata(@Context UriInfo info) {
    LOG.debug(String.format("Invoking AgentsResourceServiceImpl.getAgentsMetadata: %s", info.getRequestUri()));

    validator.validateSafe(info);
    String ids = info.getPathSegments().get(0).getMatrixParameters().getFirst("ids");
    Set<String> idSet;
    if (ids == null) {
      idSet = Collections.emptySet();
    } else {
      idSet = new HashSet<String>(Arrays.asList(ids.split(",")));
    }

    try {
      return agentService.getAgentsMetadata(idSet);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get agents metadata", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

}
