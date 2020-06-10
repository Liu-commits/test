/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package org.terracotta.management.resource;

import javax.xml.bind.annotation.XmlAttribute;

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
public abstract class VersionedEntity implements Representable {
  
  private String version;
  
  /**
   * @return version detail for associated with this entity
   */
  @XmlAttribute
  public String getVersion() {
    return version;
  }
  
  public void setVersion(String version) {
    this.version = version;
  }
}
