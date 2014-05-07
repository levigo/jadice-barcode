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

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import com.jadice.barcode.grid.LuminanceGrid;

/**
 * @author jh
 */
public class BufferedImageLuminanceSource implements LuminanceGrid {

  private final BufferedImage image;
  private final int numChannels;
  private final int width;
  private final int height;

  public BufferedImageLuminanceSource(BufferedImage image) {
    this.image = image;
    this.width = image.getWidth();
    this.height = image.getHeight();
    numChannels = image.getColorModel().getNumColorComponents() > 1 ? 3 : 1;
  }

  @Override
  public AffineTransform getInverseTransform() {
    return new AffineTransform();
  }

  /*
   * (non-Javadoc)
   * 
   * The pixel luminance is derived from the image's RGB pixel values using a fast approximation of
   * the CCIR 601 formula Y = 0.2126 * R + 0.7152 * G + 0.0722 * B.
   * 
   * @see com.jadice.barcode.grid.LuminanceGrid#getPixelLuminance(int, int)
   */
  @Override
  public int getLuminance(int x, int y) {
    if (x < 0 || y < 0 || x >= width || y >= height)
      return 0xff;

    int rgb = image.getRGB(x, y);

    int r = (rgb >> 16) & 0xff;
    int g = (rgb >> 8) & 0xff;
    int b = rgb & 0xff;

    return (r * 13932 + g * 46871 + b * 4731) / 65536;
  }

  @Override
  public int getNumChannels() {
    return numChannels;
  }

  @Override
  public int getWidth() {
    return image.getWidth();
  }

  @Override
  public int getHeight() {
    return image.getHeight();
  }
}
