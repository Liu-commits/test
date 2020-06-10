/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.ClusteredInstanceFactoryAccessor;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfigurationListener;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.event.CacheManagerEventListener;
import net.sf.ehcache.management.resource.CacheConfigEntityV2;
import net.sf.ehcache.management.resource.CacheEntityV2;
import net.sf.ehcache.management.resource.CacheManagerConfigEntityV2;
import net.sf.ehcache.management.resource.CacheManagerEntityV2;
import net.sf.ehcache.management.resource.CacheStatisticSampleEntityV2;
import net.sf.ehcache.management.resource.QueryResultsEntityV2;
import net.sf.ehcache.management.sampled.CacheManagerSampler;
import net.sf.ehcache.management.sampled.CacheManagerSamplerImpl;
import net.sf.ehcache.management.sampled.CacheSampler;
import net.sf.ehcache.management.sampled.CacheSamplerImpl;
import net.sf.ehcache.management.service.CacheManagerServiceV2;
import net.sf.ehcache.management.service.CacheServiceV2;
import net.sf.ehcache.management.service.EntityResourceFactoryV2;
import net.sf.ehcache.management.service.SamplerRepositoryServiceV2;
import net.sf.ehcache.terracotta.ClusteredInstanceFactory;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.AgentEntityV2;
import org.terracotta.management.resource.AgentMetadataEntityV2;
import org.terracotta.management.resource.Representable;
import org.terracotta.management.resource.ResponseEntityV2;
import org.terracotta.management.resource.events.EventEntityV2;
import org.terracotta.management.resource.exceptions.ExceptionUtils;
import org.terracotta.management.resource.services.AgentServiceV2;
import org.terracotta.management.resource.services.LicenseService;
import org.terracotta.management.resource.services.Utils;
import org.terracotta.management.resource.services.events.EventServiceV2;

/**
 * A controller class registering new {@link CacheManager}.
 * </p>
 * An {@link EntityResourceFactoryV2} implementation that interacts with the native Ehcache API.
 * </p>
 * A {@link CacheServiceV2} implementation that interacts with the native Ehcache API to manipulate {@link Cache}
 * objects.
 *
 * @author brandony
 */
public class DfltSamplerRepositoryServiceV2 implements SamplerRepositoryServiceV2,
    EntityResourceFactoryV2, CacheManagerServiceV2, CacheServiceV2, AgentServiceV2,
    EventServiceV2 {

  public static final String AGENCY = "Ehcache";

  final Set<EventListener> listeners = new CopyOnWriteArraySet<EventListener>();

  /**
   * Guarded By cacheManagerSamplerRepoLock
   */
  private final Map<String, SamplerRepoEntry> cacheManagerSamplerRepo = new HashMap<String, SamplerRepoEntry>();

  private final ReadWriteLock cacheManagerSamplerRepoLock = new ReentrantReadWriteLock();
  protected final ManagementRESTServiceConfiguration configuration;

  private final Map<String, PropertyChangeListener> configurationChangeListenerMap = new HashMap<String, PropertyChangeListener>();
  
  private final RemoteAgentEndpointImpl remoteAgentEndpoint;

  public DfltSamplerRepositoryServiceV2(ManagementRESTServiceConfiguration configuration,
      RemoteAgentEndpointImpl remoteAgentEndpoint) {
    this.configuration = configuration;
    this.remoteAgentEndpoint = remoteAgentEndpoint;
  }

  private static void enableNonStopFor(SamplerRepoEntry samplerRepoEntry, boolean enable) {
    ClusteredInstanceFactory clusteredInstanceFactory = ClusteredInstanceFactoryAccessor.getClusteredInstanceFactory(samplerRepoEntry.cacheManager);
    if (clusteredInstanceFactory != null) {
      clusteredInstanceFactory.enableNonStopForCurrentThread(enable);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void register(CacheManager cacheManager) {
    String name = cacheManager.getName();

    cacheManagerSamplerRepoLock.writeLock().lock();
    try {
      if (!cacheManagerSamplerRepo.containsKey(name)) {
        SamplerRepoEntry entry = new SamplerRepoEntry(cacheManager);
        cacheManager.setCacheManagerEventListener(entry);
        cacheManagerSamplerRepo.put(name, entry);
      }
    } finally {
      cacheManagerSamplerRepoLock.writeLock().unlock();
    }

    Collection<Map<String, Object>> cacheEntities = new ArrayList<Map<String, Object>>();
    String[] cacheNames = cacheManager.getCacheNames();
    for (String cacheName : cacheNames) {
      Map<String, Object> cacheAttributes = new HashMap<String, Object>();
      cacheAttributes.put("version", this.getClass().getPackage().getImplementationVersion());
      cacheAttributes.put("agentId", Representable.EMBEDDED_AGENT_ID);
      cacheAttributes.put("name", cacheName);
      Collection<CacheEntityV2> createCacheEntities = createCacheEntities(
          Collections.singleton(name), Collections.singleton(cacheName), null).getEntities();
      if (createCacheEntities != null && !createCacheEntities.isEmpty()) {
        cacheAttributes.put("attributes", createCacheEntities.iterator().next().getAttributes());
      }
      cacheEntities.add(cacheAttributes);
    }

    EventEntityV2 eventEntityV2 = new EventEntityV2();
    ResponseEntityV2<CacheManagerEntityV2> responseEntity = createCacheManagerEntities(Collections.singleton(name), null);
    CacheManagerEntityV2 cacheManagerEntity = responseEntity.getEntities().iterator().next(); 
    eventEntityV2.setAgentId(Representable.EMBEDDED_AGENT_ID);
    eventEntityV2.setType("EHCACHE.CACHEMANAGER.ADDED");
    eventEntityV2.getRootRepresentables().put("attributes", cacheManagerEntity.getAttributes());
    eventEntityV2.getRootRepresentables().put("caches", cacheEntities);
    eventEntityV2.getRootRepresentables().put("cacheManagerName", name);
    cacheManager.sendManagementEvent(eventEntityV2, eventEntityV2.getType());
    for (EventListener eventListener : listeners) {
      eventListener.onEvent(eventEntityV2);
    }
    
    PropertyChangeListener pcl = new ConfigurationPropertyChangeListener(cacheManager);
    cacheManager.getConfiguration().addPropertyChangeListener(pcl);
    configurationChangeListenerMap.put(cacheManager.getName(), pcl);
  }

  class ConfigurationPropertyChangeListener implements PropertyChangeListener {
    private CacheManager cacheManager;
    
    ConfigurationPropertyChangeListener(CacheManager cacheManager) {
      this.cacheManager = cacheManager;
      
    }
    @Override
    public void propertyChange(PropertyChangeEvent pce) {
      String propName = pce.getPropertyName();
      
      if (propName.equals("maxBytesLocalHeap")) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        attrs.put("MaxBytesLocalHeap", pce.getNewValue());
        attrs.put("MaxBytesLocalHeapAsString", cacheManager.getConfiguration().getMaxBytesLocalHeapAsString());

        sendCacheManagerEvent(attrs, cacheManager);
      } else if (propName.equals("maxBytesLocalDisk")) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        attrs.put("MaxBytesLocalDisk", pce.getNewValue());
        attrs.put("MaxBytesLocalDiskAsString", cacheManager.getConfiguration().getMaxBytesLocalDiskAsString());

        sendCacheManagerEvent(attrs, cacheManager);
      }
    }
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void unregister(CacheManager cacheManager) {
    cacheManagerSamplerRepoLock.writeLock().lock();

    try {
      SamplerRepoEntry entry = cacheManagerSamplerRepo.remove(cacheManager.getName());
      entry.destroy();
    } finally {
      cacheManagerSamplerRepoLock.writeLock().unlock();
    }
    EventEntityV2 eventEntityV2 = new EventEntityV2();
    eventEntityV2.setAgentId(Representable.EMBEDDED_AGENT_ID);
    eventEntityV2.getRootRepresentables().put("cacheManagerName", cacheManager.getName());
    eventEntityV2.getRootRepresentables().put("ClusterUUID", cacheManager.getClusterUUID());
    eventEntityV2.setType("EHCACHE.CACHEMANAGER.REMOVED");
    cacheManager.sendManagementEvent(eventEntityV2, eventEntityV2.getType());
    for (EventListener eventListener : listeners) {
      eventListener.onEvent(eventEntityV2);
    }

    String cacheManagerName = cacheManager.getName();
    cacheManager.getConfiguration().removePropertyChangeListener(configurationChangeListenerMap.get(cacheManagerName));
    configurationChangeListenerMap.remove(cacheManagerName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasRegistered() {
    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      return !cacheManagerSamplerRepo.isEmpty();
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ResponseEntityV2<CacheManagerEntityV2> createCacheManagerEntities(Set<String> cacheManagerNames,
      Set<String> attributes) {
    ResponseEntityV2<CacheManagerEntityV2> responseEntityV2 = new ResponseEntityV2<CacheManagerEntityV2>();

    String requestClusterUUID = remoteAgentEndpoint.getRequestClusterUUID();
    CacheManagerEntityBuilderV2 builder = null;
    Collection<CacheManagerEntityV2> entities;
    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      if (cacheManagerNames == null) {
        for (SamplerRepoEntry entry : cacheManagerSamplerRepo.values()) {
          if (!entry.isConnectedToCluster(requestClusterUUID)) {
            continue;
          }
          builder = builder == null ? CacheManagerEntityBuilderV2.createWith(entry.getCacheManagerSampler()) : builder
              .add(entry.getCacheManagerSampler());
        }
      } else {
        for (String cmName : cacheManagerNames) {
          SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cmName);
          if (entry != null && entry.isConnectedToCluster(requestClusterUUID)) {
            builder = builder == null ? CacheManagerEntityBuilderV2.createWith(entry.getCacheManagerSampler()) : builder
                .add(entry.getCacheManagerSampler());
          }
        }
      }
      if (builder == null) {
        entities = new HashSet<CacheManagerEntityV2>(0);
      } else {
        entities = attributes == null ? builder.build() : builder.add(attributes).build();
      }
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }

    responseEntityV2.getEntities().addAll(entities);
    return responseEntityV2;
  }

  @Override
  public ResponseEntityV2<CacheManagerConfigEntityV2> createCacheManagerConfigEntities(Set<String> cacheManagerNames) {
    ResponseEntityV2<CacheManagerConfigEntityV2> responseEntityV2 = new ResponseEntityV2<CacheManagerConfigEntityV2>();

    CacheManagerConfigurationEntityBuilderV2 builder = null;
    Collection<CacheManagerConfigEntityV2> entities;
    String requestClusterUUID = remoteAgentEndpoint.getRequestClusterUUID();

    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      if (cacheManagerNames == null) {
        for (SamplerRepoEntry entry : cacheManagerSamplerRepo.values()) {
          if (!entry.isConnectedToCluster(requestClusterUUID)) {
            continue;
          }
          builder = builder == null ? CacheManagerConfigurationEntityBuilderV2
            .createWith(entry.getCacheManagerSampler()) : builder.add(entry.getCacheManagerSampler());
        }
      } else {
        for (String cmName : cacheManagerNames) {
          SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cmName);

          if (entry != null && entry.isConnectedToCluster(requestClusterUUID)) {
            builder = builder == null ? CacheManagerConfigurationEntityBuilderV2
              .createWith(entry.getCacheManagerSampler()) : builder.add(entry.getCacheManagerSampler());
          }
        }
      }
      if (builder == null) {
        entities = new HashSet<CacheManagerConfigEntityV2>(0);
      } else {
        entities = builder.build();
      }
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }

    responseEntityV2.getEntities().addAll(entities);
    return responseEntityV2;
  }

  @Override
  public ResponseEntityV2<CacheEntityV2> createCacheEntities(Set<String> cacheManagerNames,
      Set<String> cacheNames,
      Set<String> attributes) {
    ResponseEntityV2<CacheEntityV2> responseEntityV2 = new ResponseEntityV2<CacheEntityV2>();
    CacheEntityBuilderV2 builder = null;
    Collection<CacheEntityV2> entities;
    String requestClusterUUID = remoteAgentEndpoint.getRequestClusterUUID();

    cacheManagerSamplerRepoLock.readLock().lock();

    List<SamplerRepoEntry> disabledSamplerRepoEntries = new ArrayList<SamplerRepoEntry>();

    try {
      if (cacheManagerNames == null) {
        for (Map.Entry<String, SamplerRepoEntry> entry : cacheManagerSamplerRepo.entrySet()) {
          if (!entry.getValue().isConnectedToCluster(requestClusterUUID)) {
            continue;
          }
          enableNonStopFor(entry.getValue(), false);
          disabledSamplerRepoEntries.add(entry.getValue());
          for (CacheSampler sampler : entry.getValue().getComprehensiveCacheSamplers(cacheNames)) {
            builder = builder == null ? CacheEntityBuilderV2.createWith(sampler, entry.getKey()) : builder
                .add(sampler, entry.getKey());
          }
        }
      } else {
        for (String cmName : cacheManagerNames) {
          SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cmName);
          if (entry != null && entry.isConnectedToCluster(requestClusterUUID)) {
            enableNonStopFor(entry, false);
            disabledSamplerRepoEntries.add(entry);
            for (CacheSampler sampler : entry.getComprehensiveCacheSamplers(cacheNames)) {
              builder = builder == null ? CacheEntityBuilderV2.createWith(sampler, cmName) : builder.add(sampler, cmName);
            }
          }
        }
      }
      if (builder == null) {
        entities = new HashSet<CacheEntityV2>(0);
      } else {
        entities = attributes == null ? builder.build() : builder.add(attributes).build();
      }
    } finally {
      for (SamplerRepoEntry samplerRepoEntry : disabledSamplerRepoEntries) {
        enableNonStopFor(samplerRepoEntry, true);
      }
      cacheManagerSamplerRepoLock.readLock().unlock();
    }

    responseEntityV2.getEntities().addAll(entities);
    return responseEntityV2;
  }

  @Override
  public ResponseEntityV2<CacheConfigEntityV2> createCacheConfigEntities(Set<String> cacheManagerNames,
      Set<String> cacheNames) {
    CacheConfigurationEntityBuilderV2 builder = null;
    Collection<CacheConfigEntityV2> entities;
    ResponseEntityV2<CacheConfigEntityV2> responseEntityV2 = new ResponseEntityV2<CacheConfigEntityV2>();
    String requestClusterUUID = remoteAgentEndpoint.getRequestClusterUUID();

    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      if (cacheManagerNames == null) {
        for (Map.Entry<String, SamplerRepoEntry> entry : cacheManagerSamplerRepo.entrySet()) {
          for (CacheSampler sampler : entry.getValue().getComprehensiveCacheSamplers(cacheNames)) {
            if (!entry.getValue().isConnectedToCluster(requestClusterUUID)) {
              continue;
            }
            builder = builder == null ? CacheConfigurationEntityBuilderV2
              .createWith(entry.getValue().getCacheManagerSampler(), sampler.getCacheName()) : builder
              .add(entry.getValue().getCacheManagerSampler(), sampler.getCacheName());
          }
        }
      } else {
        for (String cmName : cacheManagerNames) {
          SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cmName);
          if (entry != null && entry.isConnectedToCluster(requestClusterUUID)) {
            for (CacheSampler sampler : entry.getComprehensiveCacheSamplers(cacheNames)) {
              builder = builder == null ? CacheConfigurationEntityBuilderV2
                .createWith(entry.getCacheManagerSampler(), sampler.getCacheName()) : builder
                .add(entry.getCacheManagerSampler(), sampler.getCacheName());
            }
          }
        }
      }
      if (builder == null) {
        entities = new HashSet<CacheConfigEntityV2>(0);
      } else {
        entities = builder.build();
      }
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }

    responseEntityV2.getEntities().addAll(entities);
    return responseEntityV2;
  }

  @Override
  public ResponseEntityV2<CacheStatisticSampleEntityV2> createCacheStatisticSampleEntity(Set<String> cacheManagerNames,
      Set<String> cacheNames,
      Set<String> sampleNames) {
    CacheStatisticSampleEntityBuilderV2 builder = CacheStatisticSampleEntityBuilderV2.createWith(sampleNames);
    ResponseEntityV2<CacheStatisticSampleEntityV2> responseEntityV2 = new ResponseEntityV2<CacheStatisticSampleEntityV2>();
    String requestClusterUUID = remoteAgentEndpoint.getRequestClusterUUID();

    cacheManagerSamplerRepoLock.readLock().lock();

    List<SamplerRepoEntry> disabledSamplerRepoEntries = new ArrayList<SamplerRepoEntry>();

    try {
      if (cacheManagerNames == null) {
        for (Map.Entry<String, SamplerRepoEntry> entry : cacheManagerSamplerRepo.entrySet()) {
          if (!entry.getValue().isConnectedToCluster(requestClusterUUID)) {
            continue;
          }
          enableNonStopFor(entry.getValue(), false);
          disabledSamplerRepoEntries.add(entry.getValue());
          for (CacheSampler sampler : entry.getValue().getComprehensiveCacheSamplers(cacheNames)) {
            builder.add(sampler, entry.getKey());
          }
        }
      } else {
        for (String cmName : cacheManagerNames) {
          SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cmName);
          if (entry != null && entry.isConnectedToCluster(requestClusterUUID)) {
            enableNonStopFor(entry, false);
            disabledSamplerRepoEntries.add(entry);
            for (CacheSampler sampler : entry.getComprehensiveCacheSamplers(cacheNames)) {
              builder.add(sampler, cmName);
            }
          }
        }
      }

      responseEntityV2.getEntities().addAll(builder.build());
      return responseEntityV2;
    } finally {
      for (SamplerRepoEntry samplerRepoEntry : disabledSamplerRepoEntries) {
        enableNonStopFor(samplerRepoEntry, true);
      }
      cacheManagerSamplerRepoLock.readLock().unlock();
    }
  }

  @Override
  public void createOrUpdateCache(String cacheManagerName, String cacheName, CacheEntityV2 resource)
    throws ServiceExecutionException {
    cacheManagerSamplerRepoLock.readLock().lock();

    SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cacheManagerName);
    try {
      enableNonStopFor(entry, false);
      if (entry != null) {
        entry.updateCache(cacheName, resource);
      } else {
        throw new ServiceExecutionException("CacheManager not found !");
      }
    } finally {
      enableNonStopFor(entry, true);
      cacheManagerSamplerRepoLock.readLock().unlock();
    }

  }

  @Override
  public void clearCache(String cacheManagerName, String cacheName) {
    cacheManagerSamplerRepoLock.readLock().lock();

    SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cacheManagerName);
    try {
      enableNonStopFor(entry, false);
      if (entry != null) {
        entry.clearCache(cacheName);
      }
    } finally {
      enableNonStopFor(entry, true);
      cacheManagerSamplerRepoLock.readLock().unlock();
    }
  }

  @Override
  public void updateCacheManager(String cacheManagerName,
      CacheManagerEntityV2 resource) throws ServiceExecutionException {
    cacheManagerSamplerRepoLock.writeLock().lock();

    try {
      SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cacheManagerName);
      if (entry != null) {
        CacheManagerSampler cms = entry.getCacheManagerSampler();
        checkForInvalidAttributes(cacheManagerName, resource);

        Object mbldsAttr = resource.getAttributes().get(SamplerRepoEntry.MAX_BYTES_LOCAL_DISK_STRING);
        if (mbldsAttr != null) {
          cms.setMaxBytesLocalDiskAsString(mbldsAttr.toString());
        }

        Object mblhsAttr = resource.getAttributes().get(SamplerRepoEntry.MAX_BYTES_LOCAL_HEAP_STRING);
        if (mblhsAttr != null) {
          cms.setMaxBytesLocalHeapAsString(mblhsAttr.toString());
        }

        Object enabledAttr = resource.getAttributes().get(SamplerRepoEntry.ENABLED_ATTR);
        if (enabledAttr != null) {
          cms.setEnabled(Boolean.valueOf(enabledAttr.toString()));
        }
      } else {
        throw new ServiceExecutionException("CacheManager not found !");
      }
    } finally {
      cacheManagerSamplerRepoLock.writeLock().unlock();
    }
  }

  @Override
  public ResponseEntityV2<QueryResultsEntityV2> executeQuery(String cacheManagerName, String queryString) throws ServiceExecutionException {
    cacheManagerSamplerRepoLock.writeLock().lock();
    ResponseEntityV2<QueryResultsEntityV2> responseEntityV2 = new ResponseEntityV2<QueryResultsEntityV2>();

    try {
      SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cacheManagerName);
      if (entry != null) {
        try {
          enableNonStopFor(entry, false);
          CacheManagerSampler cms = entry.getCacheManagerSampler();

          responseEntityV2.getEntities().addAll(
              buildQueryResultsEntity(cacheManagerName, cms.executeQuery(queryString)));
          return responseEntityV2;
        } catch (Exception e) {
          Throwable t = ExceptionUtils.getRootCause(e);
          throw new ServiceExecutionException(t.getMessage());
        } finally {
          enableNonStopFor(entry, true);
        }
      } else {
        throw new ServiceExecutionException("CacheManager not found !");
      }
    } finally {
      cacheManagerSamplerRepoLock.writeLock().unlock();
    }
  }

  private Collection<QueryResultsEntityV2> buildQueryResultsEntity(String cacheManagerName, Object[][] data) {
    QueryResultsEntityV2 qre = new QueryResultsEntityV2();

    qre.setAgentId(AgentEntityV2.EMBEDDED_AGENT_ID);
    qre.setName(cacheManagerName);
    qre.setData(data);

    return Collections.singleton(qre);
  }

  private void checkForInvalidAttributes(String cacheManagerName, CacheManagerEntityV2 resource) throws ServiceExecutionException {
    boolean invalidAttributesFound =  false;
    StringBuilder errorMessage =  new StringBuilder("You are not allowed to update those attributes : ");
    if(resource.getName() != null && !resource.getName().equals(cacheManagerName)) {
      errorMessage.append("name ");
      invalidAttributesFound = true;
    }
    for(Map.Entry<String,Object> attribute : resource.getAttributes().entrySet()) {
      String key = attribute.getKey();
      if(!key.equals(SamplerRepoEntry.MAX_BYTES_LOCAL_DISK_STRING) &&
          !key.equals(SamplerRepoEntry.MAX_BYTES_LOCAL_HEAP_STRING) &&
          !key.equals(SamplerRepoEntry.ENABLED_ATTR)) {
        errorMessage.append(key).append(" ");
        invalidAttributesFound = true;
      }
    }
    if (invalidAttributesFound) {
      errorMessage.append(". Only ")
      .append(SamplerRepoEntry.MAX_BYTES_LOCAL_DISK_STRING)
      .append(", ")
      .append(SamplerRepoEntry.MAX_BYTES_LOCAL_HEAP_STRING)
      .append(" and ")
      .append(SamplerRepoEntry.ENABLED_ATTR)
      .append(" can be updated for a CacheManager.");
      throw new ServiceExecutionException(errorMessage.toString());
    }
  }

  private static void checkForInvalidAttributes(String cacheName, CacheEntityV2 resource) throws ServiceExecutionException {
    boolean invalidAttributesFound =  false;
    StringBuilder errorMessage =  new StringBuilder("You are not allowed to update those attributes : ");
    if(resource.getName() != null && !resource.getName().equals(cacheName)) {
      errorMessage.append("name ");
      invalidAttributesFound = true;
    }
    Set<String> validAttributes =  new HashSet<String>();
    validAttributes.add(SamplerRepoEntry.ENABLED_ATTR);
    validAttributes.add(SamplerRepoEntry.BULK_LOAD_ENABLED);
    validAttributes.add(SamplerRepoEntry.MAX_ELEMENTS_ON_DISK);
    validAttributes.add(SamplerRepoEntry.LOGGING_ENABLED);
    validAttributes.add(SamplerRepoEntry.MAX_BYTES_LOCAL_DISK_STRING);
    validAttributes.add(SamplerRepoEntry.MAX_BYTES_LOCAL_HEAP_STRING);
    validAttributes.add(SamplerRepoEntry.MAX_ENTRIES_LOCAL_HEAP);
    validAttributes.add(SamplerRepoEntry.MAX_ENTRIES_IN_CACHE);
    validAttributes.add(SamplerRepoEntry.TIME_TO_IDLE_SECONDS);
    validAttributes.add(SamplerRepoEntry.TIME_TO_LIVE_SEC);

    for(Map.Entry<String,Object> attribute : resource.getAttributes().entrySet()) {
      String key = attribute.getKey();
      if(!validAttributes.contains(key) ) {
        errorMessage.append(key).append(" ");
        invalidAttributesFound = true;
      }
    }
    if (invalidAttributesFound) {
      errorMessage.append(". Only ");
      for (String validAttribute : validAttributes) {
        errorMessage.append(validAttribute).append(" ");
      }
      errorMessage.append("can be updated for a Cache.");
      throw new ServiceExecutionException(errorMessage.toString());
    }
  }

  @Override
  public ResponseEntityV2<AgentEntityV2> getAgents(Set<String> ids) throws ServiceExecutionException {
    ResponseEntityV2<AgentEntityV2> agentEntityCollectionV2 = new ResponseEntityV2<AgentEntityV2>();

    if (ids.isEmpty()) {
      agentEntityCollectionV2.getEntities().add(buildAgentEntity());
      return agentEntityCollectionV2;
    }

    for (String id : ids) {
      if (!id.equals(AgentEntityV2.EMBEDDED_AGENT_ID)) {
        throw new ServiceExecutionException("Unknown agent ID : " + id);
      }
      agentEntityCollectionV2.getEntities().add(buildAgentEntity());
    }

    return agentEntityCollectionV2;
  }

  private AgentEntityV2 buildAgentEntity() {
    AgentEntityV2 e = new AgentEntityV2();
    e.setAgentId(AgentEntityV2.EMBEDDED_AGENT_ID);
    e.setAgencyOf(AGENCY);

    StringBuilder sb = new StringBuilder();
    for (String cmName : cacheManagerSamplerRepo.keySet()) {
      sb.append(cmName).append(",");
    }
    if (sb.indexOf(",") > -1) {
      sb.deleteCharAt(sb.length() - 1);
    }

    e.getRootRepresentables().put("cacheManagerNames", sb.toString());
    return e;
  }

  @Override
  public ResponseEntityV2<AgentMetadataEntityV2> getAgentsMetadata(Set<String> ids) throws ServiceExecutionException {
    ResponseEntityV2<AgentMetadataEntityV2> agentEntityCollectionV2 = new ResponseEntityV2<AgentMetadataEntityV2>();

    if (ids.isEmpty()) {
      agentEntityCollectionV2.getEntities().addAll(Collections.singleton(buildAgentMetadata()));
      return agentEntityCollectionV2;
    }

    Collection<AgentMetadataEntityV2> result = new ArrayList<AgentMetadataEntityV2>();

    for (String id : ids) {
      if (!id.equals(AgentEntityV2.EMBEDDED_AGENT_ID)) {
        throw new ServiceExecutionException("Unknown agent ID : " + id);
      }
      result.add(buildAgentMetadata());
    }
    agentEntityCollectionV2.getEntities().addAll(result);
    return agentEntityCollectionV2;
  }

  private AgentMetadataEntityV2 buildAgentMetadata() {
    AgentMetadataEntityV2 ame = new AgentMetadataEntityV2();

    ame.setAgentId(AgentEntityV2.EMBEDDED_AGENT_ID);
    ame.setAgencyOf(AGENCY);
    ame.setProductVersion(this.getClass().getPackage().getImplementationVersion());
    ame.setAvailable(true);

    if (isTsaBridged()) {
      ame.setSecured(isTsaSecured());
      ame.setSslEnabled(isTsaSecured());
      ame.setNeedClientAuth(false);
    } else {
      ame.setSecured(Utils.trimToNull(configuration.getSecurityServiceLocation()) != null);
      ame.setSslEnabled(Utils.trimToNull(configuration.getSecurityServiceLocation()) != null);
      ame.setNeedClientAuth(configuration.isNeedClientAuth());
    }

    ame.setLicensed(ServiceLocator.locate(LicenseService.class).isLicensed());
    ame.setSampleHistorySize(configuration.getSampleHistorySize());
    ame.setSampleIntervalSeconds(configuration.getSampleIntervalSeconds());
    ame.setEnabled(configuration.isEnabled());

    return ame;
  }

  protected boolean isTsaBridged() {
    return remoteAgentEndpoint.isTsaBridged();
  }

  protected boolean isTsaSecured() {
    return false;
  }

  /**
   * The repository entry class that is also a {@link CacheManagerEventListener}.
   */
  private final class SamplerRepoEntry implements CacheManagerEventListener {
    private final static String ENABLED_ATTR = "Enabled";

    private final static String BULK_LOAD_ENABLED = "NodeBulkLoadEnabled";

    private final static String MAX_ELEMENTS_ON_DISK = "MaxElementsOnDisk";

    private final static String MAX_BYTES_LOCAL_DISK = "MaxBytesLocalDisk";

    private final static String MAX_BYTES_LOCAL_DISK_STRING = "MaxBytesLocalDiskAsString";

    private final static String MAX_BYTES_LOCAL_HEAP = "MaxBytesLocalHeap";

    private final static String MAX_BYTES_LOCAL_HEAP_STRING = "MaxBytesLocalHeapAsString";

    private final static String LOGGING_ENABLED = "LoggingEnabled";

    private final static String TIME_TO_IDLE_SECONDS = "TimeToIdleSeconds";

    private final static String TIME_TO_LIVE_SEC = "TimeToLiveSeconds";

    private final static String MAX_ENTRIES_LOCAL_HEAP = "MaxEntriesLocalHeap";

    private final static String MAX_ENTRIES_IN_CACHE = "MaxEntriesInCache";

    private CacheManager cacheManager;

    private CacheManagerSampler cacheManagerSampler;

    /**
     * Guarded by cacheSamplerMapLock
     */
    private Map<String, CacheSampler> cacheSamplersByName;

    private volatile Status status = Status.STATUS_UNINITIALISED;

    private final ReadWriteLock cacheSamplerMapLock = new ReentrantReadWriteLock();

    private final Map<String, PropertyChangeListenerImplementation> propertyChangeListeners = new ConcurrentHashMap<String, PropertyChangeListenerImplementation>();
    private final Map<String, SamplerCacheConfigurationListener> samplerCacheConfigurationListeners = new ConcurrentHashMap<String, SamplerCacheConfigurationListener>();

    public SamplerRepoEntry(CacheManager cacheManager) {
      if (cacheManager == null) {
        throw new IllegalArgumentException("cacheManager == null");
      }

      this.cacheManagerSampler = new CacheManagerSamplerImpl(cacheManager);
      this.cacheManager = cacheManager;

      String[] cNames = cacheManager.getCacheNames();
      this.cacheSamplersByName = new HashMap<String, CacheSampler>(cNames.length);

      for (String cName : cNames) {
        Ehcache ehcache = cacheManager.getEhcache(cName);
        cacheSamplersByName.put(cName, new CacheSamplerImpl(ehcache));
        PropertyChangeListenerImplementation propertyChangeListener = new PropertyChangeListenerImplementation(ehcache);
        SamplerCacheConfigurationListener samplerCacheConfigurationListener = new SamplerCacheConfigurationListener(ehcache);
        propertyChangeListeners.put(cName, propertyChangeListener);
        samplerCacheConfigurationListeners.put(cName, samplerCacheConfigurationListener);
        ehcache.addPropertyChangeListener(propertyChangeListener);
        ehcache.getCacheConfiguration().addConfigurationListener(samplerCacheConfigurationListener);
      }
    }

    public boolean isConnectedToCluster(String uuid) {
      return uuid == null // local requests
              || cacheManager.getClusterUUID().equals("") // unclustered cache managers
              || cacheManager.getClusterUUID().equals(uuid);
    }

    public CacheManagerSampler getCacheManagerSampler() {
      return cacheManagerSampler;
    }

    public Collection<CacheSampler> getComprehensiveCacheSamplers(Set<String> cacheSamplerNames) {
      Collection<CacheSampler> samplers = new HashSet<CacheSampler>();

      cacheSamplerMapLock.readLock().lock();
      try {
        if (cacheSamplerNames == null) {
          for (CacheSampler cs : cacheSamplersByName.values()) {
            samplers.add(cs);
          }
        } else {
          for (String cName : cacheSamplerNames) {
            CacheSampler cs = cacheSamplersByName.get(cName);
            if (cs != null) {
              samplers.add(cs);
            }
          }
        }
      } finally {
        cacheSamplerMapLock.readLock().unlock();
      }

      return samplers;
    }

    public void clearCache(String cacheSamplerName) {
      cacheSamplerMapLock.writeLock().lock();

      CacheSampler cs;
      try {
        cs = cacheSamplersByName.get(cacheSamplerName);
        if (cs != null) {
          cs.removeAll();
        }
      } finally {
        cacheSamplerMapLock.writeLock().unlock();
      }
    }

    public void updateCache(String cacheSamplerName, CacheEntityV2 cacheResource)
      throws ServiceExecutionException {
      cacheSamplerMapLock.writeLock().lock();

      CacheSampler cs;
      try {
        cs = cacheSamplersByName.get(cacheSamplerName);

        if (cs != null) {
          try {
            checkForInvalidAttributes(cacheSamplerName, cacheResource);

            Boolean enabledAttr = (Boolean) cacheResource.getAttributes().get(ENABLED_ATTR);
            if (enabledAttr != null) {
              cs.setEnabled(enabledAttr);
            }

            Boolean enabledBlkLoad = (Boolean) cacheResource.getAttributes().get(BULK_LOAD_ENABLED);
            if (enabledBlkLoad != null) {
              cs.setNodeBulkLoadEnabled(enabledBlkLoad);
            }

            Integer maxElementsOnDiskAttr = (Integer) cacheResource.getAttributes().get(MAX_ELEMENTS_ON_DISK);
            if (maxElementsOnDiskAttr != null) {
              cs.setMaxElementsOnDisk(maxElementsOnDiskAttr);
            }

            Boolean loggingEnabledAttr = (Boolean) cacheResource.getAttributes().get(LOGGING_ENABLED);
            if (loggingEnabledAttr != null) {
              cs.setLoggingEnabled(loggingEnabledAttr);
            }

            Object mbldsAttr = cacheResource.getAttributes().get(MAX_BYTES_LOCAL_DISK_STRING);
            if (mbldsAttr != null) {
              cs.setMaxBytesLocalDiskAsString(mbldsAttr.toString());
            }

            Object mblhsAttr = cacheResource.getAttributes().get(MAX_BYTES_LOCAL_HEAP_STRING);
            if (mblhsAttr != null) {
              cs.setMaxBytesLocalHeapAsString(mblhsAttr.toString());
            }

            Integer melhAttr = (Integer) cacheResource.getAttributes().get(MAX_ENTRIES_LOCAL_HEAP);
            if (melhAttr != null) {
              cs.setMaxEntriesLocalHeap(melhAttr);
            }

            Integer meicAttr = (Integer) cacheResource.getAttributes().get(MAX_ENTRIES_IN_CACHE);
            if (meicAttr != null) {
              cs.setMaxEntriesInCache(meicAttr);
            }

            Object ttiAttr = cacheResource.getAttributes().get(TIME_TO_IDLE_SECONDS);
            if (ttiAttr != null) {
              cs.setTimeToIdleSeconds(Long.parseLong(ttiAttr.toString()));
            }

            Object ttlAttr = cacheResource.getAttributes().get(TIME_TO_LIVE_SEC);
            if (ttlAttr != null) {
              cs.setTimeToLiveSeconds(Long.parseLong(ttlAttr.toString()));
            }
          } catch (RuntimeException e) {
            throw new ServiceExecutionException(e);
          }

        } else {
          throw new ServiceExecutionException("Cache not found !");
        }
      } finally {
        cacheSamplerMapLock.writeLock().unlock();
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() throws CacheException {
      status = Status.STATUS_ALIVE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Status getStatus() {
      return status;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() throws CacheException {
      cacheSamplerMapLock.writeLock().lock();

      try {
        cacheSamplersByName.clear();
        cacheSamplersByName = null;
      } finally {
        cacheSamplerMapLock.writeLock().unlock();
      }

      status = Status.STATUS_SHUTDOWN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyCacheAdded(String cacheName) {
      cacheSamplerMapLock.writeLock().lock();
      try {
        Ehcache ehcache = cacheManager.getEhcache(cacheName);

        if (ehcache != null) {
          cacheSamplersByName.put(cacheName, new CacheSamplerImpl(ehcache));

          Map<String, Object> cacheAttributes = new HashMap<String, Object>();
          cacheAttributes.put("version", this.getClass().getPackage().getImplementationVersion());
          cacheAttributes.put("name", ehcache.getName());
          cacheAttributes.put("cacheManagerName", cacheManager.getName());
          Collection<CacheEntityV2> createCacheEntities = DfltSamplerRepositoryServiceV2.this.createCacheEntities(
              Collections.singleton(cacheManager.getName()), Collections.singleton(ehcache.getName()), null).getEntities();
          if (createCacheEntities != null && !createCacheEntities.isEmpty()) {
            CacheEntityV2 next = createCacheEntities.iterator().next();
            cacheAttributes.put("attributes", next.getAttributes());
          }

          final EventEntityV2 evenEntityV2 = new EventEntityV2();
          evenEntityV2.setAgentId(Representable.EMBEDDED_AGENT_ID);
          evenEntityV2.setType("EHCACHE.CACHE.ADDED");
          evenEntityV2.getRootRepresentables().put("cache", cacheAttributes);
          cacheManager.sendManagementEvent(evenEntityV2, evenEntityV2.getType());
          for (EventListener eventListener : listeners) {
            eventListener.onEvent(evenEntityV2);
          }
          PropertyChangeListenerImplementation propertyChangeListener = new PropertyChangeListenerImplementation(ehcache);
          SamplerCacheConfigurationListener samplerCacheConfigurationListener = new SamplerCacheConfigurationListener(ehcache);
          propertyChangeListeners.put(cacheName, propertyChangeListener);
          samplerCacheConfigurationListeners.put(cacheName, samplerCacheConfigurationListener);
          ehcache.addPropertyChangeListener(propertyChangeListener);
          ehcache.getCacheConfiguration().addConfigurationListener(samplerCacheConfigurationListener);
        }

      } finally {
        cacheSamplerMapLock.writeLock().unlock();
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyCacheRemoved(String cacheName) {
      cacheSamplerMapLock.writeLock().lock();

      try {
        propertyChangeListeners.remove(cacheName);
        samplerCacheConfigurationListeners.remove(cacheName);

        cacheSamplersByName.remove(cacheName);

        Map<String, Object> cacheAttributes = new HashMap<String, Object>();
        cacheAttributes.put("version", this.getClass().getPackage().getImplementationVersion());
        cacheAttributes.put("name", cacheName);
        cacheAttributes.put("cacheManagerName", cacheManager.getName());

        EventEntityV2 evenEntityV2 = new EventEntityV2();
        evenEntityV2.setAgentId(Representable.EMBEDDED_AGENT_ID);
        evenEntityV2.setType("EHCACHE.CACHE.REMOVED");
        evenEntityV2.getRootRepresentables().put("cache", cacheAttributes);
        cacheManager.sendManagementEvent(evenEntityV2, evenEntityV2.getType());
        for (EventListener eventListener : listeners) {
          eventListener.onEvent(evenEntityV2);
        }
      } finally {
        cacheSamplerMapLock.writeLock().unlock();
      }
    }

    public void destroy() {
      cacheManagerSampler = null;
      cacheManager = null;
    }
  }

  class PropertyChangeListenerImplementation implements PropertyChangeListener {
    private final Ehcache cache;

    public PropertyChangeListenerImplementation(Ehcache cache) {
      this.cache = cache;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      String propName = evt.getPropertyName();
      Object propVal = evt.getNewValue();
      
      if ("Disabled".equals(propName)) {
        propName = "Enabled";
        propVal = Boolean.valueOf(((Boolean) propVal) != Boolean.TRUE);
      }
      
      sendCacheEvent(propVal, propName, cache);
    }
  }

  class SamplerCacheConfigurationListener implements CacheConfigurationListener {

    private final Ehcache cache;

    public SamplerCacheConfigurationListener(Ehcache ehcache) {
      this.cache = ehcache;
    }

    @Override
    public void timeToIdleChanged(long oldTimeToIdle, long newTimeToIdle) {
      String key = "TimeToIdleSeconds";
      sendCacheEvent(newTimeToIdle, key, cache);
    }

    @Override
    public void timeToLiveChanged(long oldTimeToLive, long newTimeToLive) {
      String key = "TimeToLiveSeconds";
      sendCacheEvent(newTimeToLive, key, cache);
    }

    @Override
    public void diskCapacityChanged(int oldCapacity, int newCapacity) {
      String key = "MaxEntriesLocalDisk";
      sendCacheEvent(newCapacity, key, cache);
    }

    @Override
    public void memoryCapacityChanged(int oldCapacity, int newCapacity) {
      String key = "MaxEntriesLocalHeap";
      sendCacheEvent(newCapacity, key, cache);
    }

    @Override
    public void loggingChanged(boolean oldValue, boolean newValue) {
      String key = "LoggingEnabled";
      sendCacheEvent(newValue, key, cache);
    }

    @Override
    public void registered(CacheConfiguration config) {
    }

    @Override
    public void deregistered(CacheConfiguration config) {
    }

    @Override
    public void maxBytesLocalHeapChanged(long oldValue, long newValue) {
      String key = "MaxBytesLocalHeap";
      sendCacheEvent(newValue, key, cache);
    }

    @Override
    public void maxBytesLocalDiskChanged(long oldValue, long newValue) {
      String key = "MaxBytesLocalDisk";
      sendCacheEvent(newValue, key, cache);
    }

    @Override
    public void maxEntriesInCacheChanged(long oldValue, long newValue) {
      String key = "MaxEntriesInCache";
      sendCacheEvent(newValue, key, cache);
    }
  }

  private void sendCacheEvent(Object newValue, String key, Ehcache cache) {
    sendCacheEvent(Collections.singletonMap(key, newValue), cache);
  }

  private void sendCacheEvent(Map<String, Object> attributes, Ehcache cache) {
    CacheManager cacheManager = cache.getCacheManager();
    EventEntityV2 evenEntityV2 = new EventEntityV2();
    evenEntityV2.setAgentId(Representable.EMBEDDED_AGENT_ID);
    evenEntityV2.setType("EHCACHE.CACHE.UPDATED");
    evenEntityV2.getRootRepresentables().put("attributes", attributes);
    evenEntityV2.getRootRepresentables().put("cacheManagerName", cacheManager.getName());
    evenEntityV2.getRootRepresentables().put("cacheName", cache.getName());

    cacheManager.sendManagementEvent(evenEntityV2, evenEntityV2.getType());
    for (EventListener eventListener : listeners) {
      eventListener.onEvent(evenEntityV2);
    }
  }

  private void sendCacheManagerEvent(Map<String, Object> attributes, CacheManager cacheManager) {
    EventEntityV2 evenEntityV2 = new EventEntityV2();
    evenEntityV2.setAgentId(Representable.EMBEDDED_AGENT_ID);
    evenEntityV2.setType("EHCACHE.CACHEMANAGER.UPDATED");
    evenEntityV2.getRootRepresentables().put("attributes", attributes);
    evenEntityV2.getRootRepresentables().put("cacheManagerName", cacheManager.getName());

    cacheManager.sendManagementEvent(evenEntityV2, evenEntityV2.getType());
    for (EventListener eventListener : listeners) {
      eventListener.onEvent(evenEntityV2);
    }
  }

  @Override
  public void registerEventListener(EventListener listener, boolean localOnly) {
    listeners.add(listener);
  }

  @Override
  public void unregisterEventListener(EventListener listener) {
    listeners.remove(listener);
  }
}
