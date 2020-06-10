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

package net.sf.ehcache.store.compound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.ElementIdHelper;
import net.sf.ehcache.util.PreferredLoaderObjectInputStream;

/**
 * A copy strategy that can use partial (if both copy on read and copy on write are set) or full Serialization to copy the object graph
 *
 * @author Alex Snaps
 * @author Ludovic Orban
 */
public class ReadWriteSerializationCopyStrategy implements ReadWriteCopyStrategy<Element> {

    private static final long serialVersionUID = 2659269742281205622L;
    
    /**
     * @inheritDoc
     */
    public Element copyForWrite(Element value, ClassLoader loader) {
        if (value == null) {
            return null;
        } else {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oos = null;

            if (value.getObjectValue() == null) {
                return duplicateElementWithNewValue(value, null);
            }

            try {
                oos = new ObjectOutputStream(bout);
                oos.writeObject(value.getObjectValue());
            } catch (Exception e) {
                throw new CacheException("When configured copyOnRead or copyOnWrite, a Store will only accept Serializable values", e);
            } finally {
                try {
                    if (oos != null) {
                        oos.close();
                    }
                } catch (Exception e) {
                    //
                }
            }

            return duplicateElementWithNewValue(value, bout.toByteArray());
        }
    }

    /**
     * @inheritDoc
     */
    public Element copyForRead(Element storedValue, ClassLoader loader) {
        if (storedValue == null) {
            return null;
        } else {
            if (storedValue.getObjectValue() == null) {
                return duplicateElementWithNewValue(storedValue, null);
            }

            ByteArrayInputStream bin = new ByteArrayInputStream((byte[]) storedValue.getObjectValue());
            ObjectInputStream ois = null;
            try {
                ois = new PreferredLoaderObjectInputStream(bin, loader);
                return duplicateElementWithNewValue(storedValue, ois.readObject());
            } catch (Exception e) {
                throw new CacheException("When configured copyOnRead or copyOnWrite, a Store will only accept Serializable values", e);
            } finally {
                try {
                    if (ois != null) {
                        ois.close();
                    }
                } catch (Exception e) {
                    //
                }
            }
        }
    }

    /**
     * Make a duplicate of an element but using the specified value
     *
     * @param element  the element to duplicate
     * @param newValue the new element's value
     * @return the duplicated element
     */
    public Element duplicateElementWithNewValue(final Element element, final Object newValue) {
        Element newElement;
        if (element.usesCacheDefaultLifespan()) {
            newElement = new Element(element.getObjectKey(), newValue, element.getVersion(),
                    element.getCreationTime(), element.getLastAccessTime(), element.getHitCount(), element.usesCacheDefaultLifespan(),
                    Integer.MIN_VALUE, Integer.MIN_VALUE, element.getLastUpdateTime());
        } else {
            newElement = new Element(element.getObjectKey(), newValue, element.getVersion(),
                    element.getCreationTime(), element.getLastAccessTime(), element.getHitCount(), element.usesCacheDefaultLifespan(),
                    element.getTimeToLive(), element.getTimeToIdle(), element.getLastUpdateTime());
        }
        if (ElementIdHelper.hasId(element)) {
            ElementIdHelper.setId(newElement, ElementIdHelper.getId(element));
        }
        return newElement;
    }

}
