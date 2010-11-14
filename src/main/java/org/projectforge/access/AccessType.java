/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2010 Kai Reinhard (k.reinhard@me.com)
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

package org.projectforge.access;

import org.projectforge.core.I18nEnum;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public enum AccessType implements I18nEnum
{
  /** Needed by some hasAccess methods if only association with a group is checked. Used for constructor of UserException. */
  GROUP("group"),
  /** Access right for select, insert, update, delete of tasks. */
  TASKS("tasks"), TASK_ACCESS_MANAGEMENT("accessManagement"), TIMESHEETS("timesheets"), OWN_TIMESHEETS("ownTimesheets");

  private String key;

  /**
   * @return The key suffix will be used e. g. for i18n.
   */
  public String getKey()
  {
    return key;
  }

  /**
   * @return The full i18n key including the i18n prefix "fibu.auftrag.status.".
   */
  public String getI18nKey()
  {
    return "access.type." + key;
  }

  AccessType(final String key)
  {
    this.key = key;
  }
}
