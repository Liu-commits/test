/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.management.resource;


/**
 * <p>
 * A {@link AbstractEntityV2} that represents the metadata relevant to this agent's API and state.
 * </p>
 * 
 * @author brandony
 * 
 */
public class AgentMetadataEntityV2 extends AbstractEntityV2 {

  private String productVersion;

  private String agencyOf;

  private boolean available;

  private boolean secured;

  private boolean sslEnabled;

  private boolean needClientAuth;

  private boolean licensed;

  private int sampleHistorySize;

  private int sampleIntervalSeconds;

  private boolean enabled;


  public AgentMetadataEntityV2() {
  }

  public AgentMetadataEntityV2(AgentMetadataEntityV2 ame) {
    setAgentId(ame.getAgentId());
    setProductVersion(ame.getProductVersion());
    setAgencyOf(ame.getAgencyOf());
    setAvailable(ame.isAvailable());
    setSecured(ame.isSecured());
    setLicensed(ame.isLicensed());
    setSslEnabled(ame.isSslEnabled());
    setNeedClientAuth(ame.isNeedClientAuth());
    setSampleHistorySize(ame.getSampleHistorySize());
    setSampleIntervalSeconds(ame.getSampleIntervalSeconds());
    setEnabled(ame.isEnabled());
  }


  public String getProductVersion() {
    return productVersion;
  }

  public void setProductVersion(String productVersion) {
    this.productVersion = productVersion;
  }

  /**
   * @return the monitorable entity which this agent represents
   */
  public String getAgencyOf() {
    return agencyOf;
  }

  /**
   * @param agencyOf the monitorable entity which this agent represents to set
   */
  public void setAgencyOf(String agencyOf) {
    this.agencyOf = agencyOf;
  }

  /**
   * @return the available
   */
  public boolean isAvailable() {
    return available;
  }

  /**
   * @param available the available to set
   */
  public void setAvailable(boolean available) {
    this.available = available;
  }

  /**
   * Indicates whether or not this agent is setup with security for identity assertion.
   *
   * @return true if AAA enabled, false otherwise.
   */
  public boolean isSecured() {
    return secured;
  }

  /**
   * Set AAA indicator.
   *
   * @param secured the AAA indicator
   */
  public void setSecured(boolean secured) {
    this.secured = secured;
  }

  /**
   * Indicates whether or not the agent is licensed.
   *
   * @return true if licensed, false otherwise.
   */
  public boolean isLicensed() {
    return licensed;
  }

  /**
   * Set licensed indicator.
   *
   * @param licensed the license indicator
   */
  public void setLicensed(boolean licensed) {
    this.licensed = licensed;
  }

  /**
   * Indicates whether or not this agent's connection is established over SSL.
   *
   * @return true if SSL connection, false if not
   */
  public boolean isSslEnabled() {
    return sslEnabled;
  }

  /**
   * Set SSL connection enabled indicator.
   *
   * @param sslEnabled ssl enabled indicator
   */
  public void setSslEnabled(boolean sslEnabled) {
    this.sslEnabled = sslEnabled;
  }

  /**
   * Indicates whether or not this agent's connection requires SSL client certificate authorization. This setting is only
   * relevant if {@link #isSslEnabled()} is true.
   *
   * @return true if SSL client certificate authorization is enabled, false if not.
   *
   * @see #isSslEnabled()
   */
  public boolean isNeedClientAuth() {
    return needClientAuth;
  }

  /**
   * Set needs SSL client certificate authorization indicator. This setting is only relevant if {@link #isSslEnabled()} is true.
   *
   * @param needClientAuth SSL client certificate indicator
   */
  public void setNeedClientAuth(boolean needClientAuth) {
    this.needClientAuth = needClientAuth;
  }

  /**
   * Returns the configured sample history size for this agent.
   *
   * @return size of sample history
   */
  public int getSampleHistorySize() {
    return sampleHistorySize;
  }

  /**
   * Sets the agent's configured sample history size.
   *
   * @param sampleHistorySize sample history size
   */
  public void setSampleHistorySize(int sampleHistorySize) {
    this.sampleHistorySize = sampleHistorySize;
  }

  /**
   * Returns the configured sample interval in seconds for this agent.
   *
   * @return sample interval in seconds
   */
  public int getSampleIntervalSeconds() {
    return sampleIntervalSeconds;
  }

  /**
   * Sets the agent's configured sample interval in seconds.
   *
   * @param sampleIntervalSeconds sample interval in seconds
   */
  public void setSampleIntervalSeconds(int sampleIntervalSeconds) {
    this.sampleIntervalSeconds = sampleIntervalSeconds;
  }

  /**
   * Indicates whether or not this agent's connection is enabled.
   * 
   * @return true if it is enabled, false if not
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Set connection enabled indicator.
   * 
   * @param enabled
   *          's connection enabled indicator
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
