/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.store;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheOperationOutcomes.EvictionOutcome;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.ReadWriteLockSync;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfigurationListener;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.SizeOfPolicyConfiguration;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.PoolParticipant;
import net.sf.ehcache.pool.Size;
import net.sf.ehcache.pool.SizeOfEngine;
import net.sf.ehcache.pool.SizeOfEngineLoader;
import net.sf.ehcache.pool.impl.UnboundedPool;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.search.impl.SearchManager;
import net.sf.ehcache.store.StoreOperationOutcomes.GetOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome;
import net.sf.ehcache.store.chm.SelectableConcurrentHashMap;
import net.sf.ehcache.store.disk.StoreUpdateException;
import net.sf.ehcache.writer.CacheWriterManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.statistics.OperationStatistic;
import org.terracotta.statistics.Statistic;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.derived.EventRateSimpleMovingAverage;
import org.terracotta.statistics.derived.OperationResultFilter;
import org.terracotta.statistics.observer.OperationObserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static net.sf.ehcache.statistics.StatisticBuilder.operation;

/**
 * A Store implementation suitable for fast, concurrent in memory stores. The policy is determined by that
 * configured in the cache.
 *
 * @author Terracotta
 * @version $Id: MemoryStore.java 10025 2015-09-30 19:58:50Z alexsnaps $
 */
public class MemoryStore extends AbstractStore implements CacheConfigurationListener, Store {

    /**
     * This is the default from {@link java.util.concurrent.ConcurrentHashMap}. It should never be used, because we size
     * the map to the max size of the store.
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * Set optimisation for 100 concurrent threads.
     */
    private static final int CONCURRENCY_LEVEL = 100;

    private static final int MAX_EVICTION_RATIO = 5;

    private static final Logger LOG = LoggerFactory.getLogger(MemoryStore.class.getName());

    private static final CopyStrategyHandler NO_COPY_STRATEGY_HANDLER = new CopyStrategyHandler(false, false, null, null);

    /**
     * Eviction outcome observer
     */
    protected final OperationObserver<EvictionOutcome> evictionObserver = operation(EvictionOutcome.class).named("eviction").of(this).build();

    /**
     * The cache this store is associated with.
     */
    private final Ehcache cache;

    /**
     * Map where items are stored by key.
     */
    private final SelectableConcurrentHashMap map;
    private final PoolAccessor poolAccessor;

    private final OperationObserver<GetOutcome> getObserver = operation(GetOutcome.class).named("get").of(this).tag("local-heap").build();
    private final OperationObserver<PutOutcome> putObserver = operation(PutOutcome.class).named("put").of(this).tag("local-heap").build();
    private final OperationObserver<RemoveOutcome> removeObserver = operation(RemoveOutcome.class).named("remove").of(this).tag("local-heap").build();

    private final boolean storePinned;
    private final CopyStrategyHandler copyStrategyHandler;

    /**
     * The maximum size of the store (0 == no limit)
     */
    private volatile int maximumSize;

    /**
     * status.
     */
    private volatile Status status;

    /**
     * The eviction policy to use
     */
    private volatile Policy policy;

    /**
     * The pool accessor
     */

    private volatile CacheLockProvider lockProvider;

    /**
     * Constructs things that all MemoryStores have in common.
     *
     * @param cache the cache
     * @param pool the pool tracking the on-heap usage
     * @param searchManager the search manager
     */
    protected MemoryStore(Ehcache cache, Pool pool, BackingFactory factory, final SearchManager searchManager) {
        super(searchManager, cache.getName());
        status = Status.STATUS_UNINITIALISED;
        this.cache = cache;
        this.maximumSize = (int) cache.getCacheConfiguration().getMaxEntriesLocalHeap();
        this.policy = determineEvictionPolicy(cache);
        if (pool instanceof UnboundedPool) {
            this.poolAccessor = pool.createPoolAccessor(null, null);
        } else {
            this.poolAccessor = pool.createPoolAccessor(new Participant(),
                SizeOfPolicyConfiguration.resolveMaxDepth(cache),
                SizeOfPolicyConfiguration.resolveBehavior(cache).equals(SizeOfPolicyConfiguration.MaxDepthExceededBehavior.ABORT));
        }

        this.storePinned = determineStorePinned(cache.getCacheConfiguration());

        int maximumCapacity = isClockEviction() && !storePinned ? maximumSize : 0;
        RegisteredEventListeners eventListener = cache.getCacheEventNotificationService();
        if (Boolean.getBoolean(MemoryStore.class.getName() + ".presize")) {
            // create the CHM with initialCapacity sufficient to hold maximumSize
            final float loadFactor = maximumSize == 1 ? 1 : DEFAULT_LOAD_FACTOR;
            int initialCapacity = getInitialCapacityForLoadFactor(maximumSize, loadFactor);
            this.map = factory.newBackingMap(poolAccessor, initialCapacity,
                    loadFactor, CONCURRENCY_LEVEL, maximumCapacity, eventListener);
        } else {
            this.map = factory.newBackingMap(poolAccessor, CONCURRENCY_LEVEL, maximumCapacity, eventListener);
        }

        this.status = Status.STATUS_ALIVE;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Initialized " + this.getClass().getName() + " for " + cache.getName());
        }
        copyStrategyHandler = getCopyStrategyHandler(cache);
    }

    static CopyStrategyHandler getCopyStrategyHandler(final Ehcache cache) {
        if (cache.getCacheConfiguration().isXaTransactional() || cache.getCacheConfiguration().isXaStrictTransactional()
            || cache.getCacheConfiguration().isLocalTransactional()) {
            return new TxCopyStrategyHandler(cache.getCacheConfiguration().isCopyOnRead(),
                cache.getCacheConfiguration().isCopyOnWrite(), cache.getCacheConfiguration().getCopyStrategy(),
                cache.getCacheConfiguration().getClassLoader());
        } else if (cache.getCacheConfiguration().isCopyOnRead() || cache.getCacheConfiguration().isCopyOnWrite()) {
            return new CopyStrategyHandler(cache.getCacheConfiguration().isCopyOnRead(),
                cache.getCacheConfiguration().isCopyOnWrite(), cache.getCacheConfiguration().getCopyStrategy(),
              cache.getCacheConfiguration().getClassLoader());
        } else {
            return NO_COPY_STRATEGY_HANDLER;
        }
    }

    private boolean determineStorePinned(CacheConfiguration cacheConfiguration) {
        PinningConfiguration pinningConfiguration = cacheConfiguration.getPinningConfiguration();
        if (pinningConfiguration == null) {
            return false;
        }

        switch (pinningConfiguration.getStore()) {
            case LOCALMEMORY:
                return false;

            case INCACHE:
                return !cacheConfiguration.isOverflowToOffHeap() && !cacheConfiguration.isOverflowToDisk();

            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Calculates the initialCapacity for a desired maximumSize goal and loadFactor.
     *
     * @param maximumSizeGoal the desired maximum size goal
     * @param loadFactor      the load factor
     * @return the calculated initialCapacity. Returns 0 if the parameter <tt>maximumSizeGoal</tt> is less than or equal
     *         to 0
     */
    protected static int getInitialCapacityForLoadFactor(int maximumSizeGoal, float loadFactor) {
        double actualMaximum = Math.ceil(maximumSizeGoal / loadFactor);
        return Math.max(0, actualMaximum >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) actualMaximum);
    }

    /**
     * A factory method to create a MemoryStore.
     *
     * @param cache the cache
     * @param pool  the pool tracking the on-heap usage
     * @return an instance of a NotifyingMemoryStore, configured with the appropriate eviction policy
     */
    public static Store create(final Ehcache cache, Pool pool) {
        CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
        final BruteForceSearchManager searchManager = new BruteForceSearchManager(cache);
        MemoryStore memoryStore = new MemoryStore(cache, pool, new BasicBackingFactory(), searchManager);
        cacheConfiguration.addConfigurationListener(memoryStore);
        searchManager.setBruteForceSource(createBruteForceSource(memoryStore, cache.getCacheConfiguration()));
        return memoryStore;
    }

    /**
     * Factory method to wrap the MemoryStore into a BruteForceSource, accounting for transactional and copy
     * configuration
     *
     * @param memoryStore the underlying store acting as source
     * @param cacheConfiguration the cache configuration
     * @return a BruteForceSource connected to underlying MemoryStore and matching configuration
     */
    protected static BruteForceSource createBruteForceSource(MemoryStore memoryStore, CacheConfiguration cacheConfiguration) {
        BruteForceSource source = new MemoryStoreBruteForceSource(memoryStore, cacheConfiguration.getSearchable());
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(cacheConfiguration.isCopyOnRead(),
                cacheConfiguration.isCopyOnWrite(),
                cacheConfiguration.getCopyStrategy(), cacheConfiguration.getClassLoader());
        if (cacheConfiguration.getTransactionalMode().isTransactional()) {
            source = new TransactionalBruteForceSource(source, copyStrategyHandler);
        } else if (cacheConfiguration.isCopyOnRead() || cacheConfiguration.isCopyOnWrite()) {
            source = new CopyingBruteForceSource(source, copyStrategyHandler);
        }
        return source;
    }

    /**
     * Puts an item in the store. Note that this automatically results in an eviction if the store is full.
     *
     * @param element the element to add
     */
    public boolean put(final Element element) throws CacheException {
        if (element == null) {
            return false;
        }
        if (searchManager != null) {
            searchManager.put(cache.getName(), -1, element, null, attributeExtractors, cache.getCacheConfiguration().getDynamicExtractor());
        }
        putObserver.begin();
        long delta = poolAccessor.add(element.getObjectKey(), element.getObjectValue(), map.storedObject(element), storePinned);
        if (delta > -1) {
            Element old = map.put(element.getObjectKey(), element, delta);
            checkCapacity(element);
            if (old == null) {
                putObserver.end(PutOutcome.ADDED);
                return true;
            } else {
                putObserver.end(PutOutcome.UPDATED);
                return false;
            }
        } else {
            notifyDirectEviction(element);
            putObserver.end(PutOutcome.ADDED);
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        if (searchManager != null) {
            searchManager.put(cache.getName(), -1, element, null, attributeExtractors, cache.getCacheConfiguration().getDynamicExtractor());
        }
        long delta = poolAccessor.add(element.getObjectKey(), element.getObjectValue(), map.storedObject(element), storePinned);
        if (delta > -1) {
            final ReentrantReadWriteLock lock = map.lockFor(element.getObjectKey());
            lock.writeLock().lock();
            try {
                Element old = map.put(element.getObjectKey(), element, delta);
                if (writerManager != null) {
                    try {
                        writerManager.put(element);
                    } catch (RuntimeException e) {
                        throw new StoreUpdateException(e, old != null);
                    }
                }
                checkCapacity(element);
                return old == null;
            } finally {
                lock.writeLock().unlock();
            }
        } else {
            notifyDirectEviction(element);
            return true;
        }
    }

    /**
     * Gets an item from the cache.
     * <p/>
     * The last access time in {@link net.sf.ehcache.Element} is updated.
     *
     * @param key the key of the Element
     * @return the element, or null if there was no match for the key
     */
    public final Element get(final Object key) {
        getObserver.begin();
        if (key == null) {
            getObserver.end(GetOutcome.MISS);
            return null;
        } else {
            final Element e = map.get(key);
            if (e == null) {
                getObserver.end(GetOutcome.MISS);
                return null;
            } else {
                getObserver.end(GetOutcome.HIT);
                return e;
            }
        }
    }

    /**
     * Gets an item from the cache, without updating statistics.
     *
     * @param key the cache key
     * @return the element, or null if there was no match for the key
     */
    public final Element getQuiet(Object key) {
        return map.get(key);
    }

    /**
     * Removes an Element from the store.
     *
     * @param key the key of the Element, usually a String
     * @return the Element if one was found, else null
     */
    public Element remove(final Object key) {
        if (key == null) {
            return null;
        }
        removeObserver.begin();
        try {
            return map.remove(key);
        } finally {
            removeObserver.end(RemoveOutcome.SUCCESS);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        if (key == null) {
            return null;
        }

        // remove single item.
        Element element;
        final ReentrantReadWriteLock.WriteLock writeLock = map.lockFor(key).writeLock();
        writeLock.lock();
        try {
            element = map.remove(key);
            if (writerManager != null) {
                writerManager.remove(new CacheEntry(key, element));
            }
        } finally {
            writeLock.unlock();
        }
        if (element == null && LOG.isDebugEnabled()) {
            LOG.debug(cache.getName() + "Cache: Cannot remove entry as key " + key + " was not found");
        }
        return element;
    }

    /**
     * Memory stores are never backed up and always return false
     */
    public final boolean bufferFull() {
        return false;
    }

    /**
     * Expire all elements.
     * <p/>
     * This is a default implementation which does nothing. Expiration on demand is only implemented for disk stores.
     */
    public void expireElements() {
        for (Object key : keySet()) {
            final Element element = expireElement(key);
            if (element != null) {
                cache.getCacheEventNotificationService()
                    .notifyElementExpiry(copyStrategyHandler.copyElementForReadIfNeeded(element), false);
            }
        }
    }

    /**
     * Evicts the element for the given key, if it exists and is expired
     * @param key the key
     * @return the evicted element, if any. Otherwise null
     */
    protected Element expireElement(final Object key) {
        Element value = get(key);
        return value != null && value.isExpired() && map.remove(key, value) ? value : null;
    }

    /**
     * Chooses the Policy from the cache configuration
     * @param cache the cache
     * @return the chosen eviction policy
     */
    static Policy determineEvictionPolicy(Ehcache cache) {
        MemoryStoreEvictionPolicy policySelection = cache.getCacheConfiguration().getMemoryStoreEvictionPolicy();

        if (policySelection.equals(MemoryStoreEvictionPolicy.LRU)) {
            return new LruPolicy();
        } else if (policySelection.equals(MemoryStoreEvictionPolicy.FIFO)) {
            return new FifoPolicy();
        } else if (policySelection.equals(MemoryStoreEvictionPolicy.LFU)) {
            return new LfuPolicy();
        } else if (policySelection.equals(MemoryStoreEvictionPolicy.CLOCK)) {
            return null;
        }

        throw new IllegalArgumentException(policySelection + " isn't a valid eviction policy");
    }

    /**
     * Remove all of the elements from the store.
     */
    public final void removeAll() throws CacheException {
        for (Object key : map.keySet()) {
            remove(key);
        }
    }

    /**
     * Prepares for shutdown.
     */
    public synchronized void dispose() {
        if (status.equals(Status.STATUS_SHUTDOWN)) {
            return;
        }
        status = Status.STATUS_SHUTDOWN;
        flush();
        poolAccessor.unlink();
    }

    /**
     * Flush to disk only if the cache is diskPersistent.
     */
    public void flush() {
        if (cache.getCacheConfiguration().isClearOnFlush()) {
            removeAll();
        }
    }

    /**
     * Gets an Array of the keys for all elements in the memory cache.
     * <p/>
     * Does not check for expired entries
     *
     * @return An List
     */
    public final List<?> getKeys() {
        return new ArrayList<Object>(map.keySet());
    }

    /**
     * Returns the keySet for this store
     * @return keySet
     */
    protected Set<?> keySet() {
        return map.keySet();
    }

    /**
     * Returns the current store size.
     *
     * @return The size value
     */
    public final int getSize() {
        return map.size();
    }

    /**
     * Returns nothing since a disk store isn't clustered
     *
     * @return returns 0
     */
    public final int getTerracottaClusteredSize() {
        return 0;
    }

    /**
     * A check to see if a key is in the Store. No check is made to see if the Element is expired.
     *
     * @param key The Element key
     * @return true if found. If this method return false, it means that an Element with the given key is definitely not
     *         in the MemoryStore. If it returns true, there is an Element there. An attempt to get it may return null if
     *         the Element has expired.
     */
    public final boolean containsKey(final Object key) {
        return map.containsKey(key);
    }

    /**
     * Before eviction elements are checked.
     *
     * @param element the element to notify about its expiry
     */
    private void notifyExpiry(final Element element) {
        cache.getCacheEventNotificationService().notifyElementExpiry(copyStrategyHandler.copyElementForReadIfNeeded(element), false);
    }

    /**
     * Called when an element is evicted even before it could be installed inside the store
     *
     * @param element the evicted element
     */
    protected void notifyDirectEviction(final Element element) {
        evictionObserver.begin();
        evictionObserver.end(EvictionOutcome.SUCCESS);
        cache.getCacheEventNotificationService().notifyElementEvicted(copyStrategyHandler.copyElementForReadIfNeeded(element), false);
    }

    /**
     * An algorithm to tell if the MemoryStore is at or beyond its carrying capacity.
     *
     * @return true if the store is full, false otherwise
     */
    public final boolean isFull() {
        return maximumSize > 0 && map.quickSize() >= maximumSize;
    }

    /**
     * Check if adding an element won't provoke an eviction.
     *
     * @param element the element
     * @return true if the element can be added without provoking an eviction.
     */
    public final boolean canPutWithoutEvicting(Element element) {
        if (element == null) {
            return true;
        }

        return !isFull() && poolAccessor.canAddWithoutEvicting(element.getObjectKey(), element.getObjectValue(), map.storedObject(element));
    }

    /**
     * If the store is over capacity, evict elements until capacity is reached
     *
     * @param elementJustAdded the element added by the action calling this check
     */
    private void checkCapacity(final Element elementJustAdded) {
        if (maximumSize > 0 && !isClockEviction()) {
            int evict = Math.min(map.quickSize() - maximumSize, MAX_EVICTION_RATIO);
            for (int i = 0; i < evict; i++) {
                removeElementChosenByEvictionPolicy(elementJustAdded);
            }
        }
    }

    /**
     * Removes the element chosen by the eviction policy
     *
     * @param elementJustAdded it is possible for this to be null
     * @return true if an element was removed, false otherwise.
     */
    private boolean removeElementChosenByEvictionPolicy(final Element elementJustAdded) {

        if (policy == null) {
            return map.evict();
        }

        Element element = findEvictionCandidate(elementJustAdded);
        if (element == null) {
            LOG.debug("Eviction selection miss. Selected element is null");
            return false;
        }

        // If the element is expired, remove
        if (element.isExpired()) {
            remove(element.getObjectKey());
            notifyExpiry(element);
            return true;
        }

        if (storePinned) {
            return false;
        }

        return evict(element);
    }

    /**
     * Find a "relatively" unused element.
     *
     * @param elementJustAdded the element added by the action calling this check
     * @return the element chosen as candidate for eviction
     */
    private Element findEvictionCandidate(final Element elementJustAdded) {
        Object objectKey = elementJustAdded != null ? elementJustAdded.getObjectKey() : null;
        Element[] elements = sampleElements(objectKey);
        // this can return null. Let the cache get bigger by one.
        return policy.selectedBasedOnPolicy(elements, elementJustAdded);
    }

    /**
     * Uses random numbers to sample the entire map.
     * <p/>
     * This implemenation uses a key array.
     *
     * @param keyHint a key used as a hint indicating where the just added element is
     * @return a random sample of elements
     */
    private Element[] sampleElements(Object keyHint) {
        int size = AbstractPolicy.calculateSampleSize(map.quickSize());
        return map.getRandomValues(size, keyHint);
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        if (lockProvider != null) {
            return lockProvider;
        } else {
            lockProvider = new LockProvider();
            return lockProvider;
        }
    }

    /**
     * Gets the status of the MemoryStore.
     */
    public final Status getStatus() {
        return status;
    }

    /**
     * {@inheritDoc}
     */
    public void timeToIdleChanged(long oldTti, long newTti) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void timeToLiveChanged(long oldTtl, long newTtl) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void diskCapacityChanged(int oldCapacity, int newCapacity) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void loggingChanged(boolean oldValue, boolean newValue) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void memoryCapacityChanged(int oldCapacity, int newCapacity) {
        maximumSize = newCapacity;
        if (isClockEviction() && !storePinned) {
            map.setMaxSize(maximumSize);
        }
    }

    private boolean isClockEviction() {
        return policy == null;
    }

    /**
     * {@inheritDoc}
     */
    public void registered(CacheConfiguration config) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void deregistered(CacheConfiguration config) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void maxBytesLocalHeapChanged(final long oldValue, final long newValue) {
        this.poolAccessor.setMaxSize(newValue);
    }

    /**
     * {@inheritDoc}
     */
    public void maxBytesLocalDiskChanged(final long oldValue, final long newValue) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void maxEntriesInCacheChanged(final long oldValue, final long newValue) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(Object key) {
        return containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOffHeap(Object key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOnDisk(Object key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        return policy;
    }

    /**
     * {@inheritDoc}
     */
    @Statistic(name = "size", tags = "local-heap")
    public int getInMemorySize() {
        return getSize();
    }

    /**
     * {@inheritDoc}
     */
    @Statistic(name = "size-in-bytes", tags = "local-heap")
    public long getInMemorySizeInBytes() {
        if (poolAccessor.getSize() < 0) {
            SizeOfEngine defaultSizeOfEngine = SizeOfEngineLoader.newSizeOfEngine(SizeOfPolicyConfiguration.resolveMaxDepth(cache),
                SizeOfPolicyConfiguration.resolveBehavior(cache)
                    .equals(SizeOfPolicyConfiguration.MaxDepthExceededBehavior.ABORT),
                true);
            long sizeInBytes = 0;
            for (Object o : map.values()) {
                Element element = (Element) o;
                if (element != null) {
                    Size size = defaultSizeOfEngine.sizeOf(element.getObjectKey(), element, map.storedObject(element));
                    sizeInBytes += size.getCalculated();
                }
            }
            return sizeInBytes;
        }
        return poolAccessor.getSize();
    }

    /**
     * {@inheritDoc}
     */
    public int getOffHeapSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapSizeInBytes() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getOnDiskSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskSizeInBytes() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAbortedSizeOf() {
        return poolAccessor.hasAbortedSizeOf();
    }

    /**
     * {@inheritDoc}
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        this.policy = policy;
    }

    @Override
    public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
        super.setAttributeExtractors(extractors);
        Set<Attribute<?>> attrs = new HashSet<Attribute<?>>(attributeExtractors.size());

        for (String name : extractors.keySet()) {
            attrs.add(new Attribute(name));
        }
        ((BruteForceSearchManager)searchManager).addSearchAttributes(attrs);
    }

    /**
     * {@inheritDoc}
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        if (element == null) {
            return null;
        }
        if (searchManager != null) {
            searchManager.put(cache.getName(), -1, element, null, attributeExtractors, cache.getCacheConfiguration().getDynamicExtractor());
        }
        long delta = poolAccessor.add(element.getObjectKey(), element.getObjectValue(), map.storedObject(element), storePinned);
        if (delta > -1) {
            Element old = map.putIfAbsent(element.getObjectKey(), element, delta);
            if (old == null) {
              checkCapacity(element);
            } else {
              poolAccessor.delete(delta);
            }
            return old;
        } else {
            notifyDirectEviction(element);
            return null;
        }
    }

    /**
     * Evicts the element from the store
     * @param element the element to be evicted
     * @return true if succeeded, false otherwise
     */
    protected boolean evict(final Element element) {
        final ReentrantReadWriteLock.WriteLock lock = map.lockFor(element.getObjectKey()).writeLock();
        if (lock.tryLock()) {
            evictionObserver.begin();
            Element remove;
            try {
                remove = remove(element.getObjectKey());
            } finally {
                lock.unlock();
            }
            if (remove != null) {
                evictionObserver.end(EvictionOutcome.SUCCESS);
                cache.getCacheEventNotificationService().notifyElementEvicted(copyStrategyHandler.copyElementForReadIfNeeded(remove), false);
            }
            return remove != null;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        if (element == null || element.getObjectKey() == null) {
            return null;
        }

        Object key = element.getObjectKey();

        Lock lock = getWriteLock(key);
        lock.lock();
        try {
            Element toRemove = map.get(key);
            if (comparator.equals(element, toRemove)) {
                map.remove(key);
                return toRemove;
            } else {
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
            IllegalArgumentException {
        if (element == null || element.getObjectKey() == null) {
            return false;
        }

        if (searchManager != null) {
            searchManager.put(cache.getName(), -1, element, null, attributeExtractors, cache.getCacheConfiguration().getDynamicExtractor());
        }
        Object key = element.getObjectKey();

        long delta = poolAccessor.add(element.getObjectKey(), element.getObjectValue(), map.storedObject(element), storePinned);
        if (delta > -1) {
            Lock lock = getWriteLock(key);
            lock.lock();
            try {
                Element toRemove = map.get(key);
                if (comparator.equals(old, toRemove)) {
                    map.put(key, element, delta);
                    return true;
                } else {
                    poolAccessor.delete(delta);
                    return false;
                }
            } finally {
                lock.unlock();
            }
        } else {
            notifyDirectEviction(element);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element replace(Element element) throws NullPointerException {
        if (element == null || element.getObjectKey() == null) {
            return null;
        }
        if (searchManager != null) {
            searchManager.put(cache.getName(), -1, element, null, attributeExtractors, cache.getCacheConfiguration().getDynamicExtractor());
        }
        Object key = element.getObjectKey();

        long delta = poolAccessor.add(element.getObjectKey(), element.getObjectValue(), map.storedObject(element), storePinned);
        if (delta > -1) {
            Lock lock = getWriteLock(key);
            lock.lock();
            try {
                Element toRemove = map.get(key);
                if (toRemove != null) {
                    map.put(key, element, delta);
                    return toRemove;
                } else {
                    poolAccessor.delete(delta);
                    return null;
                }
            } finally {
                lock.unlock();
            }
        } else {
            notifyDirectEviction(element);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return null;
    }

    private Lock getWriteLock(Object key) {
        return map.lockFor(key).writeLock();
    }

    /**
     * Get a collection of the elements in this store
     *
     * @return element collection
     */
    public Collection<Element> elementSet() {
        return map.values();
    }

    /**
     * LockProvider implementation that uses the segment locks.
     */
    private class LockProvider implements CacheLockProvider {

        /**
         * {@inheritDoc}
         */
        public Sync getSyncForKey(Object key) {
            return new ReadWriteLockSync(map.lockFor(key));
        }
    }

    private static boolean getAdvancedBooleanConfigProperty(String property, String cacheName, boolean defaultValue) {
        String globalPropertyKey = "net.sf.ehcache.store.config." + property;
        String cachePropertyKey = "net.sf.ehcache.store." + cacheName + ".config." + property;
        return Boolean.parseBoolean(System.getProperty(cachePropertyKey, System.getProperty(globalPropertyKey, Boolean.toString(defaultValue))));
    }

    @Override
    public void recalculateSize(Object key) {
        if (key == null) {
            return;
        }
        map.recalculateSize(key);
    }

    /**
     * PoolParticipant that is used with the HeapPool.
     */
    private final class Participant implements PoolParticipant {

        private final EventRateSimpleMovingAverage hitRate = new EventRateSimpleMovingAverage(1, TimeUnit.SECONDS);
        private final EventRateSimpleMovingAverage missRate = new EventRateSimpleMovingAverage(1, TimeUnit.SECONDS);

        private Participant() {
            OperationStatistic<GetOutcome> getStatistic = StatisticsManager.getOperationStatisticFor(getObserver);
            getStatistic.addDerivedStatistic(new OperationResultFilter<GetOutcome>(EnumSet.of(GetOutcome.HIT), hitRate));
            getStatistic.addDerivedStatistic(new OperationResultFilter<GetOutcome>(EnumSet.of(GetOutcome.MISS), missRate));
        }

        @Override
        public boolean evict(int count, long size) {
            if (storePinned) {
                return false;
            }

            for (int i = 0; i < count; i++) {
                boolean removed = removeElementChosenByEvictionPolicy(null);
                if (!removed) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public float getApproximateHitRate() {
            return hitRate.rate(TimeUnit.SECONDS).floatValue();
        }

        @Override
        public float getApproximateMissRate() {
            return missRate.rate(TimeUnit.SECONDS).floatValue();
        }

        @Override
        public long getApproximateCountSize() {
            return map.quickSize();
        }
    }

    /**
     * Factory interface to create a MemoryStore backing.
     */
    protected interface BackingFactory {
        /**
         * Create a MemoryStore backing map.
         *
         * @param poolAccessor on-heap pool accessor
         * @param initialCapacity initial store capacity
         * @param loadFactor map load factor
         * @param concurrency map concurrency
         * @param maximumCapacity maximum store capacity
         * @param eventListener event listener (or {@code null} for no notifications)
         * @return a backing map
         */
        @Deprecated
        SelectableConcurrentHashMap newBackingMap(PoolAccessor poolAccessor, int initialCapacity,
                float loadFactor, int concurrency, int maximumCapacity, RegisteredEventListeners eventListener);

        /**
         * Create a MemoryStore backing map.
         *
         * @param poolAccessor on-heap pool accessor
         * @param concurrency map concurrency
         * @param maximumCapacity maximum store capacity
         * @param eventListener event listener (or {@code null} for no notifications)
         * @return a backing map
         */
        SelectableConcurrentHashMap newBackingMap(PoolAccessor poolAccessor, int concurrency,
                int maximumCapacity, RegisteredEventListeners eventListener);
    }

    /**
     * Simple backing map factory.
     */
    static class BasicBackingFactory implements BackingFactory {

        @Override
        public SelectableConcurrentHashMap newBackingMap(PoolAccessor poolAccessor, int concurrency,
                int maximumCapacity, RegisteredEventListeners eventListener) {
            return new SelectableConcurrentHashMap(poolAccessor, concurrency, maximumCapacity, eventListener);
        }

        @Override
        public SelectableConcurrentHashMap newBackingMap(PoolAccessor poolAccessor, int initialCapacity,
                float loadFactor, int concurrency, int maximumCapacity, RegisteredEventListeners eventListener) {
            return new SelectableConcurrentHashMap(poolAccessor, initialCapacity,
                    loadFactor, concurrency, maximumCapacity, eventListener);
        }
    }
}

