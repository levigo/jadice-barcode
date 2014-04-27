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
package com.jadice.barcode.grid.j2d;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import com.jadice.barcode.grid.BinaryGrid;

public class Utils {
  public static BufferedImage createBinaryGridImage(BinaryGrid grid) {
    BufferedImage img = new BufferedImage(grid.getWidth(), grid.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

    extractPixels(grid, 0, 0, grid.getWidth(), grid.getHeight(),
        ((DataBufferByte) img.getRaster().getDataBuffer()).getData());

    return img;
  }

  private static void extractPixels(BinaryGrid grid, int x0, int y0, int width, int height, byte dst[]) {
    for (int y = 0; y < height; y++)
      for (int x = 0; x < width; x++)
        if (!grid.samplePixel(x, y))
          dst[(y + y0) * width + x + x0] = (byte) 0xff;
  }
}
