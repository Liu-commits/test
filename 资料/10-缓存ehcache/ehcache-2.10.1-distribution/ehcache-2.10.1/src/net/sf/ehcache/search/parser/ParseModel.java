/**
 * Copyright Terracotta, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package net.sf.ehcache.search.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Direction;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.parser.MAggregate.AggOp;
import net.sf.ehcache.store.StoreQuery;

/**
 * The Class ParseModel.
 */
public class ParseModel {

    /**
     * The criteria.
     */
    private MCriteria criteria = null;

    /**
     * The targets.
     */
    private List<MTarget> targets = new ArrayList<MTarget>();

    /**
     * The limit.
     */
    private int limit = 0;

    /**
     * The is limited.
     */
    private boolean isLimited = false;

    /**
     * The order by list.
     */
    private List<MOrderBy> orderBy = new LinkedList<MOrderBy>();

    /**
     * The group by.
     */
    private List<MAttribute> groupBy = new LinkedList<MAttribute>();

    private boolean includeKeys = false;

    private boolean includeValues = false;

    private List<MAttribute> includedAttributes = new LinkedList<MAttribute>();

    private List<MAggregate> includedAggregators = new LinkedList<MAggregate>();

    private boolean includeStar = false;

    private boolean isCountStar = false;

    private String cacheName;

    private String cacheManagerName;

    private boolean cacheManagerNameWasAttempted = false;

    /**
     * Instantiates a new query parse model.
     */
    public ParseModel() {
    }

    public void includeTargetKeys() {
        this.includeKeys = true;
    }

    public void includeTargetValues() {
        this.includeValues = true;
    }

    public void includeCountStar() {
        this.isCountStar = true;
    }

    public void includeTargetAttribute(MAttribute ma) {
        if (ma.isKey()) {
            includeTargetKeys();
        } else if (ma.isValue()) {
            includeTargetValues();
        } else {
            this.includedAttributes.add(ma);
        }
        this.targets.add(new MTarget(ma));
    }

    public void includeTargetAggregator(MAggregate ma) {
        this.includedAggregators.add(ma);
        this.targets.add(new MTarget(ma));
    }

    public void includeTargetStar() {
        this.includeStar = true;
        this.targets.add(new MTarget());
    }

    /**
     * Sets the criteria.
     *
     * @param crit the new criteria
     */
    public void setCriteria(MCriteria crit) {
        criteria = crit;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        boolean first = true;
        for (MTarget ma : targets) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(ma.toString());
        }
        if (criteria != null) {
            sb.append(" where " + criteria);
        }
        for (MAttribute m : groupBy) {
            sb.append(" group by " + m);
        }
        for (MOrderBy ord : orderBy) {
            sb.append(" " + ord);
        }
        if (isLimited) {
            sb.append(" limit " + limit);
        }
        return sb.toString();
    }

    /**
     * Adds an order by.
     *
     * @param attr the attr
     * @param asc  the asc
     */
    public void addOrderBy(MAttribute attr, boolean asc) {
        orderBy.add(new MOrderBy(attr, asc));
    }

    /**
     * set the limit.
     *
     * @param lim the lim
     */
    public void setLimit(int lim) {
        isLimited = true;
        limit = lim;
    }

    /**
     * Adds the group by.
     *
     * @param attr the attr
     */
    public void addGroupBy(MAttribute attr) {
        groupBy.add(attr);
    }

    /**
     * Gets the criteria.
     *
     * @return the criteria
     */
    public MCriteria getCriteria() {
        return criteria;
    }

    /**
     * Gets the targets.
     *
     * @return the targets
     */
    public MTarget[] getTargets() {
        return targets.toArray(new MTarget[0]);
    }

    public boolean isIncludedTargetKeys() {
        return includeKeys;
    }

    public boolean isIncludedTargetValues() {
        return includeValues;
    }

    public List<MAttribute> getIncludedTargetAttributes() {
        return Collections.unmodifiableList(includedAttributes);
    }

    public List<MAggregate> getIncludedTargetAgregators() {
        return Collections.unmodifiableList(includedAggregators);
    }

    public boolean isIncludedTargetStar() {
        return includeStar;
    }

    /**
     * Gets the limit.
     *
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Checks if has a limit.
     *
     * @return true, if is limited
     */
    public boolean isLimited() {
        return isLimited;
    }

    /**
     * Gets the order by.
     *
     * @return the order by
     */
    public List<MOrderBy> getOrderBy() {
        return orderBy;
    }

    /**
     * Gets the group by.
     *
     * @return the group by
     */
    public List<MAttribute> getGroupBy() {
        return groupBy;
    }

    /**
     * Gets the query as an instantiated ehcache query object.
     *
     * @param ehcache the ehcache
     * @return the query
     */
    @SuppressWarnings("rawtypes")
    public Query getQuery(Ehcache ehcache) {
    	ClassLoader loader = ehcache.getCacheConfiguration().getClassLoader();    	
    	
        Query q = ehcache.createQuery();

        // single criteria
        if (criteria != null) {
            q.addCriteria(criteria.asEhcacheObject(loader));
        }

        // limit.
        if (isLimited) {
            q.maxResults(limit);
        }

        List<String> targetList = new ArrayList<String>();
        for (MTarget target : targets) {
        	if (target.isAttribute()) {
        		targetList.add(target.getAttribute().getName());
        	} else if (target.isAggregate()) {
        		MAggregate agg = target.getAggregate();
        		AggOp op = agg.getOp();
        		MAttribute ma = agg.getAttribute();

        		targetList.add(op.toString().toLowerCase() + "(" + ma.getName() + ")");
        	} else {
                for (Attribute attr : getAttributesImpliedByStar(ehcache)) {
                    if (Query.KEY.equals(attr) || Query.VALUE.equals(attr)) continue; // TODO
            		targetList.add(attr.getAttributeName());
                }
        	}
        }
        ((StoreQuery)q).targets(targetList.toArray(new String[0]));
        
        // targets. what to retrieve
        for (MAttribute ma : getIncludedTargetAttributes()) {
            q.includeAttribute(ma.asEhcacheObject(loader));
        }

        for (MAggregate ma : getIncludedTargetAgregators()) {
            q.includeAggregator(ma.asEhcacheObject(loader));
        }
        if (isIncludedTargetKeys()) {
            q.includeKeys();
        }
        if (isIncludedTargetValues()) {
            q.includeValues();
        }
        if (isIncludedTargetStar()) {
            for (Attribute attr : getAttributesImpliedByStar(ehcache)) {
                if (Query.KEY.equals(attr) || Query.VALUE.equals(attr)) continue; // TODO
                q.includeAttribute(attr);
            }
        }


        // group by
        for (MAttribute ma : groupBy) {
            q.addGroupBy(ma.asEhcacheObject(loader));
        }

        // order by
        for (MOrderBy o : orderBy) {
            q.addOrderBy(o.getAttribute().asEhcacheObject(loader), o.isOrderAscending() ? Direction.ASCENDING
                : Direction.DESCENDING);
        }

        return q;
    }

    private Collection<Attribute> getAttributesImpliedByStar(Ehcache cache) {
        return isIncludedTargetStar() ? cache.getSearchAttributes() : Collections.<Attribute>emptySet();
    }

    public void setCacheName(String cacheName) {
        String[] tokens = cacheName.split("\\.");
        if (tokens.length > 2) {
            throw new SearchException("Cache manager name not specified.");
        } else if (tokens.length == 2) {
            this.cacheManagerName = tokens[0];
            this.cacheName = tokens[1];
            this.cacheManagerNameWasAttempted = true;
        } else {
            this.cacheName = cacheName;
        }
    }

    public String getCacheName() {
        return this.cacheName;
    }

    public String getCacheManagerName() {
        return this.cacheManagerName;
    }
}
