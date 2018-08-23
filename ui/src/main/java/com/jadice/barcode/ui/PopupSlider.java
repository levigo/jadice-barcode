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
import java.awt.Color;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Dictionary;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.metal.MetalComboBoxIcon;

public class PopupSlider extends JPanel {
  private static final long serialVersionUID = 1L;
  private final Hyperlink label;
  private final JSlider slider;
  private final String title;

  PopupSlider(String title, int min, int max, int value) {
    super(new BorderLayout());

    this.title = title;

    this.label = new Hyperlink();
    this.label.setHorizontalAlignment(SwingConstants.LEADING);
    this.label.setIcon(new MetalComboBoxIcon());
    this.label.setHorizontalTextPosition(JLabel.LEADING);

    add(this.label, BorderLayout.LINE_START);

    this.label.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final JWindow popupFrame = new JWindow((Frame) PopupSlider.this.getRootPane().getParent());

        popupFrame.setContentPane(slider);
        slider.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseExited(MouseEvent e) {
            slider.removeMouseListener(this);
            popupFrame.dispose();
          }
        });

        slider.setBorder(new LineBorder(Color.BLACK));
        popupFrame.setAlwaysOnTop(true);
        popupFrame.pack();

        Point los = PopupSlider.this.getLocationOnScreen();
        popupFrame.setLocation(los.x, los.y + PopupSlider.this.getHeight());

        popupFrame.setVisible(true);
      }
    });

    slider = new JSlider(min, max, value);

    slider.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        updateText();
      }
    });

    updateText();
  }

  protected String formatValue(int value) {
    return Integer.toString(value);
  }

  public String getTitle() {
    return title;
  }

  public void addValueChangeListener(ChangeListener l) {
    slider.addChangeListener(l);
  }

  public void removeValueChangeListener(ChangeListener l) {
    slider.removeChangeListener(l);
  }

  public int getMinimum() {
    return slider.getMinimum();
  }

  public void setMinimum(int minimum) {
    slider.setMinimum(minimum);
  }

  public int getMaximum() {
    return slider.getMaximum();
  }

  public void setMaximum(int maximum) {
    slider.setMaximum(maximum);
  }

  public int getOrientation() {
    return slider.getOrientation();
  }

  public void setLabelTable(@SuppressWarnings("rawtypes") Dictionary labels) {
    slider.setLabelTable(labels);
  }

  public boolean getInverted() {
    return slider.getInverted();
  }

  public void setInverted(boolean b) {
    slider.setInverted(b);
  }

  public int getMajorTickSpacing() {
    return slider.getMajorTickSpacing();
  }

  public void setMajorTickSpacing(int n) {
    slider.setMajorTickSpacing(n);
  }

  public int getMinorTickSpacing() {
    return slider.getMinorTickSpacing();
  }

  public void setMinorTickSpacing(int n) {
    slider.setMinorTickSpacing(n);
  }

  public boolean getSnapToTicks() {
    return slider.getSnapToTicks();
  }

  public void setSnapToTicks(boolean b) {
    slider.setSnapToTicks(b);
  }

  public boolean getPaintTicks() {
    return slider.getPaintTicks();
  }

  public void setPaintTicks(boolean b) {
    slider.setPaintTicks(b);
  }

  public boolean getPaintTrack() {
    return slider.getPaintTrack();
  }

  public void setPaintTrack(boolean b) {
    slider.setPaintTrack(b);
  }

  public boolean getPaintLabels() {
    return slider.getPaintLabels();
  }

  public void setPaintLabels(boolean b) {
    slider.setPaintLabels(b);
  }

  public int getValue() {
    return slider.getValue();
  }

  public void setValue(int value) {
    slider.setValue(value);
    updateText();
  }

  private void updateText() {
    label.setText(PopupSlider.this.title + ": " + formatValue(slider.getValue()));
  }
}
