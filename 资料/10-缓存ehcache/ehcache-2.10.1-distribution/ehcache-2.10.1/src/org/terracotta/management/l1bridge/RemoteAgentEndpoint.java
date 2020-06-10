/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.management.l1bridge;

/**
 * The root interface that all agents remotely accessible (eg: via the TSA) must implement and make available
 * using some remoting protocol.
 *
 * @author Ludovic Orban
 */
public interface RemoteAgentEndpoint {

  final static String IDENTIFIER = RemoteAgentEndpoint.class.getSimpleName();

  /**
   * Invoke a method on the current object.
   *
   * @param remoteCallDescriptor the remote call descriptor
   * @return the result of the invocation in serialized form
   */
  byte[] invoke(RemoteCallDescriptor remoteCallDescriptor) throws Exception;

  /**
   * Get the implementation version of the agent.
   *
   * @return the version.
   */
  String getVersion();

  /**
   * Get the agency of the remote agent.
   *
   * @return the version.
   */
  String getAgency();


}
