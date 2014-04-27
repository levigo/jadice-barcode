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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

import com.jadice.barcode.BaseSettings;
import com.jadice.barcode.Options;
import com.jadice.barcode.Symbology;

public class SymbologySelector extends JComponent {
  class SymbologyCheckBox extends JCheckBox {
    private static final long serialVersionUID = 1L;
    private final Class<? extends Symbology> symbology;

    public SymbologyCheckBox(final Class<? extends Symbology> symbology) throws InstantiationException,
        IllegalAccessException {
      super(symbology.newInstance().name());
      this.symbology = symbology;

      addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          final boolean s = ((JCheckBox) e.getSource()).isSelected();
          options.getOptions(BaseSettings.class).setSymbologyEnabled(symbology, s);
          SymbologySelector.this.firePropertyChange("enabledSymbology", !s, s);
        }
      });
    }

    void syncFromModel() {
      setSelected(options.getOptions(BaseSettings.class).isSymbologyEnabled(symbology));
    }
  }

  private static final long serialVersionUID = 1L;

  final Options options;

  public SymbologySelector(final Options options) {
    this.options = options;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    try {
      for (Class<? extends Symbology> s : options.getOptions(BaseSettings.class).getAvailableSymbologies())
        add(new SymbologyCheckBox(s));
    } catch (Exception e) {
      throw new RuntimeException("Should not happen", e);
    }
  }

  public void updateFromModel() {
    for (int i = 0; i < getComponentCount(); i++)
      ((SymbologyCheckBox) getComponent(i)).syncFromModel();
  }
}
