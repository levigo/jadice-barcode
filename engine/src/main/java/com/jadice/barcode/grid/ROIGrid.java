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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

/**
 * The ROIGrid extracts a region-of-interest (ROI) from a given delegate grid.
 */
public class ROIGrid implements BinaryGrid {
  private final BinaryGrid delegate;
  private final Rectangle region;

  public ROIGrid(BinaryGrid delegate, Rectangle region) {
    this.delegate = delegate;
    this.region = region;
  }

  public AffineTransform getInverseTransform() {
    return AffineTransform.getTranslateInstance(-region.x, -region.y);
  }

  public Dimension getSize() {
    return region.getSize();
  }

  public boolean samplePixel(int x, int y) {
    if (x < 0 || y < 0 || x >= region.width || y >= region.getHeight())
      return false;
    return delegate.samplePixel(x + region.x, y + region.y);
  }

  public int getWidth() {
    return delegate.getWidth();
  }

  public int getHeight() {
    return delegate.getHeight();
  }

  public int getPixelLuminance(int x, int y) {
    if (x < 0 || y < 0 || x >= region.width || y >= region.getHeight())
      return 255;
    return delegate.getPixelLuminance(x + region.x, y + region.y);
  }
}
