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
package com.jadice.barcode.grid;

import java.awt.geom.AffineTransform;

/**
 * An abstract implementation of {@link BinaryGrid} that turns a {@link LuminanceGrid} into a binary
 * one to make it usable for detection.
 */
public abstract class Binarizer implements BinaryGrid {
  protected final LuminanceGrid grid;

  protected final int channels;

  protected Binarizer(LuminanceGrid grid) {
    this.grid = grid;
    this.channels = grid.getNumChannels();
  }

  @Override
  public AffineTransform getInverseTransform() {
    return grid.getInverseTransform();
  }

  @Override
  public boolean samplePixel(int x, int y) {
    return grid.getLuminance(x, y) < getThreshold(x, y);
  }

  protected abstract int getThreshold(int x, int y);

  @Override
  public int getWidth() {
    return grid.getWidth();
  }

  @Override
  public int getHeight() {
    return grid.getHeight();
  }
}
