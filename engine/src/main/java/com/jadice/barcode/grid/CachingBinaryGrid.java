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

public class CachingBinaryGrid implements BinaryGrid {

  private final int width;
  private final int height;

  private final byte[] pixelCache;
  private final int scanlineStride;

  public CachingBinaryGrid(BinaryGrid grid) {
    this.width = grid.getWidth();
    this.height = grid.getHeight();

    this.scanlineStride = (width + 7) / 8;
    this.pixelCache = new byte[scanlineStride * height];

    for (int y = 0; y < height; y++)
      for (int x = 0; x < width; x++)
        if (grid.samplePixel(x, y))
          pixelCache[y * scanlineStride + (x >> 3)] |= 1 << (x & 7);
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public AffineTransform getInverseTransform() {
    return new AffineTransform();
  }

  public boolean samplePixel(int x, int y) {
    if (x < 0 || y < 0 || x >= width || y >= height)
      return false;

    return (pixelCache[y * scanlineStride + (x >> 3)] & (1 << (x & 7))) != 0;
  }

  public int getPixelLuminance(int x, int y) {
    return samplePixel(x, y) ? 0 : 255;
  }
}
