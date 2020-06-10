package com.terracotta.entity;

/**
 * Marker interface for entities at the root of the hierarchy
 */
public interface RootEntity<T extends EntityConfiguration> extends ClusteredEntity<T> {
}
