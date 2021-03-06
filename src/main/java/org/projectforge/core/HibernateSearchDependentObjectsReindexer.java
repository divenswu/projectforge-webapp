/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2013 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.core;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.projectforge.registry.Registry;
import org.projectforge.registry.RegistryEntry;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.util.CollectionUtils;

/**
 * Hotfix: Hibernate-search does not update index of dependent objects.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class HibernateSearchDependentObjectsReindexer
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HibernateSearchDependentObjectsReindexer.class);

  private static HibernateSearchDependentObjectsReindexer instance = new HibernateSearchDependentObjectsReindexer();

  public static HibernateSearchDependentObjectsReindexer getSingleton()
  {
    return instance;
  }

  /**
   * Key is the embedded class (annotated with @IndexEmbedded), value the set of all dependent objects.
   */
  final Map<Class< ? extends BaseDO< ? >>, List<Entry>> map = new HashMap<Class< ? extends BaseDO< ? >>, List<Entry>>();

  class Entry
  {
    Class< ? extends BaseDO< ? >> clazz; // The dependent class which contains the annotated field.

    String fieldName;

    boolean setOrCollection;

    Entry(final Class< ? extends BaseDO< ? >> clazz, final String fieldName, final boolean setOrCollection)
    {
      this.clazz = clazz;
      this.fieldName = fieldName;
      this.setOrCollection = setOrCollection;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
      return "Entry[clazz=" + clazz.getName() + ",fieldName=" + fieldName + "]";
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj)
    {
      if (obj instanceof Entry == false) {
        return false;
      }
      final Entry o = (Entry) obj;
      return clazz.equals(o.clazz) == true && fieldName.equals(o.fieldName) == true;
    }
  }

  public HibernateSearchDependentObjectsReindexer()
  {
    final Registry registry = Registry.instance();
    for (final RegistryEntry registryEntry : registry.getOrderedList()) {
      register(registryEntry);
    }
  }

  public void reindexDependents(final HibernateTemplate hibernateTemplate, final BaseDO< ? > obj)
  {
    new Thread() {
      @Override
      public void run()
      {
        final SessionFactory sessionFactory = hibernateTemplate.getSessionFactory();
        final HibernateTemplate template = new HibernateTemplate(sessionFactory);
        final Session session = template.getSessionFactory().openSession();
        final Set<String> alreadyReindexed = new HashSet<String>();
        final List<Entry> entryList = map.get(obj.getClass());
        reindexDependents(template, session, obj, entryList, alreadyReindexed);
        session.disconnect();
        final int size = alreadyReindexed.size();
        if (size >= 10) {
          log.info("Re-indexing of " + size + " objects done after updating " + obj.getClass().getName() + ":" + obj.getId());
        }
      }
    }.start();
  }

  private void reindexDependents(final HibernateTemplate hibernateTemplate, final Session session, final BaseDO< ? > obj,
      final List<Entry> entryList, final Set<String> alreadyReindexed)
  {
    if (CollectionUtils.isEmpty(entryList) == true) {
      // Nothing to do.
      return;
    }
    for (final Entry entry : entryList) {
      final RegistryEntry registryEntry = Registry.instance().getEntryByDO(entry.clazz);
      if (registryEntry == null) {
        // Nothing to do
        return;
      }
      final List< ? > result = getDependents(hibernateTemplate, registryEntry, entry, obj);
      if (result != null) {
        for ( Object dependentObject : result) {
          if (dependentObject instanceof Object[]) {
            dependentObject = ((Object[])dependentObject)[0];
          }
          if (dependentObject instanceof BaseDO) {
            reindexDependents(hibernateTemplate, session, (BaseDO< ? >) dependentObject, alreadyReindexed);
          }
        }
      }
    }
  }

  private void reindexDependents(final HibernateTemplate hibernateTemplate, final Session session, final BaseDO< ? > obj,
      final Set<String> alreadyReindexed)
  {
    if (alreadyReindexed.contains(getReindexId(obj)) == true) {
      if (log.isDebugEnabled() == true) {
        log.debug("Object already re-indexed (skipping): " + getReindexId(obj));
      }
      return;
    }
    session.flush(); // Needed to flush the object changes!
    final FullTextSession fullTextSession = Search.getFullTextSession(session);
    fullTextSession.setFlushMode(FlushMode.AUTO);
    fullTextSession.setCacheMode(CacheMode.IGNORE);
    try {
      BaseDO< ? > dbObj = (BaseDO< ? >) session.get(obj.getClass(), obj.getId());
      if (dbObj == null) {
        dbObj = (BaseDO< ? >) session.load(obj.getClass(), obj.getId());
      }
      fullTextSession.index(dbObj);
      alreadyReindexed.add(getReindexId(dbObj));
      if (log.isDebugEnabled() == true) {
        log.debug("Object added to index: " + getReindexId(dbObj));
      }
    } catch (final Exception ex) {
      // Don't fail if any exception while re-indexing occurs.
      log.info("Fail to re-index " + obj.getClass() + ": " + ex.getMessage());
    }
    // session.flush(); // clear every batchSize since the queue is processed
    final List<Entry> entryList = map.get(obj.getClass());
    reindexDependents(hibernateTemplate, session, obj, entryList, alreadyReindexed);
  }

  private List< ? > getDependents(final HibernateTemplate hibernateTemplate, final RegistryEntry registryEntry, final Entry entry,
      final BaseDO< ? > obj)
      {
    final String queryString;
    if (entry.setOrCollection == true) {
      queryString = "from " + registryEntry.getDOClass().getName() + " o join o." + entry.fieldName + " r where r.id=?";
    } else {
      queryString = "from " + registryEntry.getDOClass().getName() + " o where o." + entry.fieldName + ".id=?";
    }
    if (log.isDebugEnabled() == true) {
      log.debug(queryString + ", id=" + obj.getId());
    }
    final List< ? > result = hibernateTemplate.find(queryString, obj.getId());
    return result;
      }

  private String getReindexId(final BaseDO< ? > obj)
  {
    return obj.getClass() + ":" + obj.getId();
  }

  void register(final RegistryEntry registryEntry)
  {
    final Class< ? extends BaseDO< ? >> clazz = registryEntry.getDOClass();
    register(clazz);
  }

  void register(final Class< ? extends BaseDO< ? >> clazz)
  {
    final Field[] fields = clazz.getDeclaredFields();
    for (final Field field : fields) {
      if (field.isAnnotationPresent(IndexedEmbedded.class) == true ||
          field.isAnnotationPresent(ContainedIn.class) == true) {
        Class< ? > embeddedClass = field.getType();
        boolean setOrCollection = false;
        if (Set.class.isAssignableFrom(embeddedClass) == true || Collection.class.isAssignableFrom(embeddedClass) == true) {
          // Please use @ContainedIn.
          final Type type = field.getGenericType();
          if (type instanceof ParameterizedType) {
            final Type actualTypeArgument = ((ParameterizedType) type).getActualTypeArguments()[0];
            if (actualTypeArgument instanceof Class) {
              embeddedClass = (Class< ? >) actualTypeArgument;
              setOrCollection = true;
            }
          }
        }
        if (BaseDO.class.isAssignableFrom(embeddedClass) == false) {
          // Only BaseDO objects are supported.
          continue;
        }
        final String name = field.getName();
        final Entry entry = new Entry(clazz, name, setOrCollection);
        List<Entry> list = map.get(embeddedClass);
        if (list == null) {
          list = new ArrayList<Entry>();
          @SuppressWarnings("unchecked")
          final Class< ? extends BaseDO< ? >> embeddedBaseDOClass = (Class< ? extends BaseDO< ? >>) embeddedClass;
          map.put(embeddedBaseDOClass, list);
        } else {
          for (final Entry e : list) {
            if (entry.equals(e) == true) {
              log.warn("Entry already registerd: " + entry);
            }
          }
        }
        list.add(entry);
      }
    }
  }
}
