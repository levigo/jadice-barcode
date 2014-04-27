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


import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

/**
 * A button which looks vaguely hyperlinky without the hassles of an actual
 * HTML-in-JTextComponent hyperlinks.
 */
public class Hyperlink extends JButton {
	private static final long serialVersionUID = 1L;

	protected boolean isUnderlineActive = false;

	protected boolean enableLink = false;

	private class MouseOverListener extends MouseAdapter {
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.MouseAdapter#mouseEntered(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseEntered(MouseEvent e) {
			if (!isUnderlineActive) {
				isUnderlineActive = true;
				repaint();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.MouseAdapter#mouseExited(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseExited(MouseEvent e) {
			if (isUnderlineActive) {
				isUnderlineActive = false;
				repaint();
			}
		}
	}

	/**
	 * 
	 */
	public Hyperlink() {
		super();
	}

	/**
	 * @param a
	 */
	public Hyperlink(Action a) {
		super(a);
	}

	/**
	 * @param icon
	 */
	public Hyperlink(Icon icon) {
		super(icon);
	}

	/**
	 * @param text
	 * @param icon
	 */
	public Hyperlink(String text, Icon icon) {
		super(text, icon);
	}

	/**
	 * @param text
	 */
	public Hyperlink(String text) {
		super(text);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.AbstractButton#init(java.lang.String, javax.swing.Icon)
	 */
	@Override
	protected void init(String text, Icon icon) {
		super.init(text, icon);

		setBorderPainted(false);
		setContentAreaFilled(false);
		setOpaque(false);
		setBorder(new EmptyBorder(0, 0, 0, 0));

		addMouseListener(new MouseOverListener());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (!isUnderlineActive || !enableLink)
			return;

		String text = getText();
		Icon icon = isEnabled() ? getIcon() : getDisabledIcon();
		if (icon == null && text == null)
			return;

		FontMetrics fm = getFontMetrics(g.getFont());
		Insets insets = getInsets();

		Rectangle paintViewR = new Rectangle();
		paintViewR.x = insets.left;
		paintViewR.y = insets.top;
		paintViewR.width = getWidth() - (insets.left + insets.right);
		paintViewR.height = getHeight() - (insets.top + insets.bottom);

		Rectangle paintIconR = new Rectangle();
		paintIconR.x = paintIconR.y = paintIconR.width = paintIconR.height = 0;

		Rectangle paintTextR = new Rectangle();
		paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;

		SwingUtilities.layoutCompoundLabel(this, fm, text, icon,
				getVerticalAlignment(), getHorizontalAlignment(),
				getVerticalTextPosition(), getHorizontalTextPosition(), paintViewR,
				paintIconR, paintTextR, getIconTextGap());

		if (text != null) {
			if (((View) getClientProperty(BasicHTML.propertyKey)) != null) {
				System.err.println("HyperlinkLabel doesn't support HTML");
			} else {
				int x = paintTextR.x;
				// just some stupid heuristic, since we don't know better:
				// underline should appear at half the descent.
				int y = paintTextR.y + fm.getAscent() + fm.getDescent() / 2;
				g.drawLine(x, y, x + paintTextR.width, y);
			}
		}
	}

	/**
	 * @return the enableLink
	 */
	public boolean isEnableLink() {
		return enableLink;
	}

	/**
	 * @param enableLink the enableLink to set
	 */
	public void setLinkEnabled(boolean enableLink) {
		this.enableLink = enableLink;
	}

	/**
	 * @param enableLink the enableLink to set
	 */
	public void setUnderlineActive(boolean active) {
		this.isUnderlineActive = active;
	}
}
