/**
 * jadice barcode engine - a Java-based barcode decoding engine
 *
 * Copyright (C) 1995-${year} levigo holding gmbh. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 *
 * Contact: solutions@levigo.de
 */
package com.jadice.barcode.ui;
/**
 * 
 */


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * 
 */
public class CollapsibleSection extends JPanel {
	private static final long serialVersionUID = 1L;
	private final CollapsiblePane collapsible;
	private final Hyperlink link;

	public CollapsibleSection(Container contentPane, String collapsedTitle,
			String expandedTitle, boolean collapsed) {
		setLayout(new BorderLayout());

		CollapsiblePane c = new CollapsiblePane(contentPane);
		add(c, BorderLayout.CENTER);

		Action a = c.getActionMap().get(CollapsiblePane.TOGGLE_ACTION);
		a.putValue(CollapsiblePane.COLLAPSED_ICON, UIManager
				.getIcon("Tree.collapsedIcon"));
		a.putValue(CollapsiblePane.EXPANDED_ICON, UIManager
				.getIcon("Tree.expandedIcon"));
		a.putValue(CollapsiblePane.COLLAPSED_NAME, collapsedTitle);
		a.putValue(CollapsiblePane.EXPANDED_NAME, expandedTitle);

		link = new Hyperlink(a);
		link.setHorizontalAlignment(SwingConstants.LEADING);
		add(link, BorderLayout.NORTH);

		super.addImpl(Box.createHorizontalStrut(link.getIcon().getIconWidth()),
				BorderLayout.LINE_START, -1);
		
		c.setCollapsed(collapsed);

		// assign only late, to signal addImpl we're done.
		this.collapsible = c;
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		Dimension p = getMinimumSize();
		super.setBounds(x, y, Math.min(p.width, width), Math.min(p.height,
				height));
	}
	
	@Override
	public void setBackground(Color bg) {
		super.setBackground(bg);
		if (null != collapsible)
			((JComponent) collapsible.getContentPanel()).setBackground(bg);
	}

	/**
	 * Set expanded/collapsed.
	 * 
	 * @param collapsed
	 */
	public void setCollapsed(boolean collapsed) {
		collapsible.setCollapsed(collapsed);
	}

	@Override
	public void setOpaque(boolean isOpaque) {
		super.setOpaque(isOpaque);
		if (null != link)
			link.setOpaque(isOpaque);
		if (null != collapsible)
			collapsible.setOpaque(isOpaque);
	}

	/**
	 * Overriden to redirect call to the content pane.
	 */
	@Override
	public void setLayout(LayoutManager mgr) {
		if (null != collapsible)
			collapsible.setLayout(mgr);
		else
			super.setLayout(mgr);
	}

	/**
	 * Overriden to redirect call to the content pane.
	 */
	@Override
	protected void addImpl(Component comp, Object constraints, int index) {
		if (null != collapsible)
			collapsible.add(comp, constraints, index);
		else
			super.addImpl(comp, constraints, index);
	}

	/**
	 * Overriden to redirect call to the content pane
	 */
	@Override
	public void remove(Component comp) {
		collapsible.remove(comp);
	}

	/**
	 * Overriden to redirect call to the content pane.
	 */
	@Override
	public void remove(int index) {
		collapsible.remove(index);
	}

	/**
	 * Overriden to redirect call to the content pane.
	 */
	@Override
	public void removeAll() {
		collapsible.removeAll();
	}

	/**
	 * Set the collapsed title.
	 * 
	 * @param collapsedTitle
	 */
	public void setCollapsedTitle(String collapsedTitle) {
		link.getAction().putValue(CollapsiblePane.COLLAPSED_NAME, collapsedTitle);
	}

	/**
	 * Set the expanded title.
	 * 
	 * @param expandedTitle
	 */
	public void setExpandedTitle(String expandedTitle) {
		link.getAction().putValue(CollapsiblePane.EXPANDED_NAME, expandedTitle);
	}
}
