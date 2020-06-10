package org.terracotta.management.resource;

import java.util.ArrayList;
import java.util.Collection;

public class ResponseEntityV2<T extends AbstractEntityV2> extends AbstractEntityV2 {

  private String apiVersion = VERSION_V2;
  private final Collection<T> entities = new ArrayList<T>();
  private final Collection<ExceptionEntityV2> exceptionEntities = new ArrayList<ExceptionEntityV2>();

  public Collection<T> getEntities() {
    return entities;
  }

  public Collection<ExceptionEntityV2> getExceptionEntities() {
    return exceptionEntities;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  /**
   * @return version detail for associated with this entity
   */
  public String getApiVersion() {
    return apiVersion;
  }

}
