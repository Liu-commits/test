/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package org.terracotta.management.resource;

/**
 * <p>
 * An abstract class describing a {@link Representable} that exposes version information for use by the client.  The
 * default implementation will lazily initialize version by looking to this instantiations class package for the 
 * implemented version ({@code Package#getImplementationVersion()}) on call to {@code #getVersion()} if it has 
 * not already been set.
 * </p>
 * 
 * @author brandony
 * 
 */
public abstract class AbstractEntityV2 implements Representable {
  public static final String VERSION_V2 = "v2";

  private String agentId = Representable.EMBEDDED_AGENT_ID;

  @Override
  public String getAgentId() {
    return agentId;
  }

  @Override
  public void setAgentId(String agentId) {
    this.agentId = agentId;    
  }
}
