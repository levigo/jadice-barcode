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
import java.lang.reflect.Method;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jadice.barcode.BaseSettings;
import com.jadice.barcode.Options;
import com.jadice.barcode.Settings;
import com.jadice.barcode.linear.LinearCodeSettings;

public class ScanController extends JComponent {
  private static final long serialVersionUID = 1L;

  final Options options;

  private class Slider extends JComponent {
    private static final long serialVersionUID = 1L;

    final PopupSlider slider;
    final Method getter;
    final Method setter;

    public Slider(int min, int max, String title, final Class<? extends Settings> settingsClass,
        final String propertyName) {
      setLayout(new BorderLayout());
      // add(new JLabel(title), BorderLayout.NORTH);

      try {
        final String ucfirst = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        getter = settingsClass.getMethod("get" + ucfirst);
        setter = settingsClass.getMethod("set" + ucfirst, int.class);

        final Settings settings = options.getOptions(settingsClass);

        slider = new PopupSlider(title, min, max, (Integer) getter.invoke(settings));
        slider.addValueChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent e) {
            try {
              setter.invoke(settings, slider.getValue());
            } catch (Exception e1) {
              e1.printStackTrace();
            }
            ScanController.this.firePropertyChange("option." + propertyName, slider.getValue() + 1, slider.getValue());
          }
        });
        slider.setMajorTickSpacing(50);
        slider.setMinorTickSpacing(10);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        add(slider, BorderLayout.SOUTH);
      } catch (Exception e1) {
        throw new RuntimeException("No getter/setter for " + propertyName, e1);
      }
    }

    public void updateFromModel() {
      try {
        slider.setValue((Integer) getter.invoke(options));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public ScanController(final Options options) {
    this.options = options;
    setLayout(new RackLayout(this, RackLayout.Y_AXIS));

    add(new Slider(-1, 100, "Pixel threshold", BaseSettings.class, "threshold"));
    Box.createVerticalStrut(5);
    add(new Slider(1, 100, "Initial scan interval", LinearCodeSettings.class, "scanInterval"));
    Box.createVerticalStrut(5);
    add(new Slider(0, 80, "Bar width tolerance", LinearCodeSettings.class, "barWidthTolerance"));
    Box.createVerticalStrut(5);
    add(new Slider(0, 150, "Overprint tolerance", BaseSettings.class, "overprintTolerance"));
    Box.createVerticalStrut(5);
    add(new Slider(30, 150, "Quiet zone tolerance", LinearCodeSettings.class, "quietZoneTolerance"));
    Box.createVerticalStrut(5);
    add(new Slider(5, 150, "Confidence radius", LinearCodeSettings.class, "confidenceRadius"));
  }

  public void updateFromModel() {
    for (int i = 0; i < getComponentCount(); i++)
      if (getComponent(i) instanceof Slider)
        ((Slider) getComponent(i)).updateFromModel();
  }
}
