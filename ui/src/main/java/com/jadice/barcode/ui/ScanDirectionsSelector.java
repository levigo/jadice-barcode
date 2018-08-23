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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

import com.jadice.barcode.Options;
import com.jadice.barcode.linear.LinearCodeSettings;
import com.jadice.barcode.linear.LinearCodeSettings.Direction;

public class ScanDirectionsSelector extends JComponent {
  class DirectionCheckBox extends JCheckBox {
    private static final long serialVersionUID = 1L;
    private final Direction direction;

    public DirectionCheckBox(final Direction direction) {
      super(direction.toString());
      this.direction = direction;

      addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          final boolean s = ((JCheckBox) e.getSource()).isSelected();
          options.getSettings(LinearCodeSettings.class).setDirectionEnabled(direction, s);
          ScanDirectionsSelector.this.firePropertyChange("enabledDirection", !s, s);
        }
      });
    }

    void syncFromModel() {
      setSelected(options.getSettings(LinearCodeSettings.class).isDirectionEnabled(direction));
    }
  }

  private static final long serialVersionUID = 1L;

  final Options options;

  public ScanDirectionsSelector(final Options options) {
    this.options = options;
    setLayout(new BorderLayout());

    add(new DirectionCheckBox(Direction.EAST), BorderLayout.EAST);
    DirectionCheckBox s = new DirectionCheckBox(Direction.SOUTH);
    add(s, BorderLayout.SOUTH);
    s.setHorizontalAlignment(SwingConstants.CENTER);
    add(new DirectionCheckBox(Direction.WEST), BorderLayout.WEST);
    DirectionCheckBox n = new DirectionCheckBox(Direction.NORTH);
    n.setHorizontalAlignment(SwingConstants.CENTER);
    add(n, BorderLayout.NORTH);

    updateFromModel();
  }

  public void updateFromModel() {
    for (int i = 0; i < getComponentCount(); i++)
      ((DirectionCheckBox) getComponent(i)).syncFromModel();
  }
}
