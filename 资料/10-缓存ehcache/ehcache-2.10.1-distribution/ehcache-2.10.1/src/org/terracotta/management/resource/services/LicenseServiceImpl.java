package org.terracotta.management.resource.services;

public class LicenseServiceImpl implements LicenseService {

  private boolean licensed;

  public LicenseServiceImpl(boolean licensed) {
    super();
    this.licensed = licensed;
  }

  @Override
  public boolean isLicensed() {
    return licensed;
  }

}
