/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.management.resource;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.ws.rs.core.GenericType;

/**
 * @author Ludovic Orban
 */
public class SubGenericType<T, S> extends GenericType<T> {
  private final Class<T> type;
  private final Class<S> subType;

  public SubGenericType(final Class<T> type, final Class<S> subType) {
    super(new ParameterizedType() {
      @Override
      public Type[] getActualTypeArguments() {
        return new Type[] { subType };
      }

      @Override
      public Type getRawType() {
        return type;
      }

      @Override
      public Type getOwnerType() {
        return type;
      }
    });
    this.type = type;
    this.subType = subType;
  }

  @Override
  public boolean equals(Object obj) {
    if (SubGenericType.class.equals(obj.getClass())) {
      SubGenericType other = (SubGenericType)obj;
      return other.type.equals(type) && other.subType.equals(subType);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.type.hashCode() + this.subType.hashCode();
  }

  @Override
  public String toString() {
    return "SubGenericType<" + type.getName() + ", " + subType.getName() + ">";
  }

}