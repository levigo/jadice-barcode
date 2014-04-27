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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.BoxLayout;

/**
 * A Rack is like a {@link BoxLayout}, except that for a horizontal/vertical layout it forces all
 * cells to be as high/wide as the cell provided by the enclosing container. Thus, with respect to
 * the fill behaviour more closely resembles a {@link BorderLayout}.
 */
public final class RackLayout extends BoxLayout {
  private static final long serialVersionUID = 1L;

  private final int axis;

  /**
   * @param target
   * @param axis
   * @param position
   */
  public RackLayout(Container target, int axis) {
    super(target, axis);
    this.axis = axis;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.BoxLayout#layoutContainer(java.awt.Container)
   */
  @Override
  public void layoutContainer(Container target) {
    super.layoutContainer(target);

    Insets in = target.getInsets();
    int nChildren = target.getComponentCount();
    for (int i = 0; i < nChildren; i++) {
      Component c = target.getComponent(i);
      Rectangle b = c.getBounds();

      if (axis == BoxLayout.X_AXIS)
        c.setBounds(b.x, in.top, b.width, target.getHeight() - in.top - in.bottom);
      else
        c.setBounds(in.left, b.y, target.getWidth() - in.left - in.right, b.height);
    }
  }
}