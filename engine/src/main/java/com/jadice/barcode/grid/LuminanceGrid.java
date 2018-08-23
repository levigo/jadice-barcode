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

/**
 * A LuminanceGrid is a {@link Grid} extension which supplies normalized luminance values for
 * pixels.
 */
public interface LuminanceGrid extends Grid {
  /**
   * Return the luminance value of the pixel at the given location normalized to the range [0,255].
   * It is assumed that 0 represents black. However, most decoding algorithms should be insensitive
   * to image inversion.
   * 
   * @param x
   * @param y
   * @return
   */
  int getLuminance(int x, int y);

  /**
   * Return the number of color channels of the base image. This is currently FYI only.
   * 
   * @return
   */
  int getNumChannels();
}
