/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

/**
 * @author Ludovic Orban
 */
public class UriInfoUtils {

  private final static Set<String> PRODUCT_IDS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("TMS", "WAN", "USER")));

  public static Set<String> extractProductIds(UriInfo info) {
    List<String> ids = info.getQueryParameters().get("productIds");
    if (ids == null) {
      return null;
    }

    Set<String> result = new HashSet<String>();
    for (String idsString : ids) {
      List<String> idNames = Arrays.asList(idsString.split(","));
      for (String idName : idNames) {
        if (idName.equals("*")) {
          result.addAll(PRODUCT_IDS);
          continue;
        }
        result.add(idName);
      }
    }
    return result;
  }

  public static Set<String> extractAgentIds(UriInfo info) {
    PathSegment agentsPathSegment = null;

    List<PathSegment> pathSegments = info.getPathSegments();
    for (PathSegment pathSegment : pathSegments) {
      if (pathSegment.getPath().equals("agents")) {
        agentsPathSegment = pathSegment;
        break;
      }
    }

    if (agentsPathSegment == null) {
      throw new IllegalArgumentException("path does not contain /agents segment");
    }

    String value = agentsPathSegment.getMatrixParameters().getFirst("ids");

    Set<String> values;
    if (value == null) {
      values = Collections.emptySet();
    } else {
      values = new HashSet<String>(Arrays.asList(value.split(",")));
    }

    return values;
  }

  public static Set<String> extractSegmentMatrixParameterAsSet(UriInfo info, String pathName, String parameterName) {
    List<PathSegment> pathSegments = info.getPathSegments();
    for (PathSegment pathSegment : pathSegments) {
      if (pathSegment.getPath().equals(pathName)) {
        List<String> values = pathSegment.getMatrixParameters().get(parameterName);
        return toSet(values);
      }
    }
    return null;
  }

  public static Set<String> extractLastSegmentMatrixParameterAsSet(UriInfo info, String parameterName) {
    List<String> values = info.getPathSegments().get(info.getPathSegments().size() - 1).getMatrixParameters().get(parameterName);
    return toSet(values);
  }

  private static Set<String> toSet(List<String> values) {
    if (values == null) {
      return null;
    }
    Set<String> result = new HashSet<String>();
    for (String value : values) {
      result.addAll(Arrays.asList(value.split(",")));
    }
    return result;
  }

}
