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

package org.projectforge.web.mobile;

import org.apache.wicket.PageParameters;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * li of an ul-Panel which is used for most content areas.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ListViewItemPanel extends Panel
{
  public static final String LINK_ID = "link";

  private static final long serialVersionUID = -3635473474541275092L;

  private boolean listDivider;

  private Link< ? > link;

  private String label, comment;

  public ListViewItemPanel(final String id, final Class< ? extends WebPage> linkClass, final String label)
  {
    this(id, new BookmarkablePageLink<String>(LINK_ID, linkClass), label);
  }

  public ListViewItemPanel(final String id, final Class< ? extends WebPage> linkClass, final PageParameters params, final String label)
  {
    this(id, new BookmarkablePageLink<String>(LINK_ID, linkClass, params), label);
  }

  public ListViewItemPanel(final String id, final Link< ? > link, final String label)
  {
    super(id);
    this.link = link;
    this.label = label;
  }

  public ListViewItemPanel(final String id, final String label)
  {
    this(id, label, null);
  }

  public ListViewItemPanel(final String id, final String label, final String comment)
  {
    super(id);
    this.label = label;
    this.comment = comment;
  }

  public ListViewItemPanel setListDivider()
  {
    this.listDivider = true;
    return this;
  }

  public ListViewItemPanel setComment(String comment)
  {
    this.comment = comment;
    return this;
  }

  public ListViewItemPanel init()
  {
    if (listDivider == true) {
      add(new SimpleAttributeModifier("data-role", "list-divider"));
    }
    if (link != null) {
      add(new Label("label", "[invisible]").setVisible(false));
      add(new Label("comment", "[invisible]").setVisible(false));
      add(link);
      link.add(new Label("linkLabel", label));
      if (comment != null) {
        link.add(new Label("linkComment", comment));
      } else {
        link.add(new Label("linkComment", "[invisible]").setVisible(false));
      }
    } else {
      add(new Label("label", label));
      if (comment != null) {
        add(new Label("comment", comment));
      } else {
        add(new Label("comment", "[invisible]").setVisible(false));
      }
      add(new Label(LINK_ID, "[invisible]").setVisible(false));
    }
    return this;
  }
}
