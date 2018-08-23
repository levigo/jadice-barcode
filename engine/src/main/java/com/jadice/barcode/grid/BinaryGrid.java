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

import com.jadice.barcode.Detector;


/**
 * This interface represents a grid of binary pixels. {@link Detector}s don't usually care about
 * what is black and what is white, but only look at the transitions.
 */
public interface BinaryGrid extends Grid {
  /**
   * Sample pixel at coordinate and return whether the pixel is black.
   * 
   * @param x
   * @param y
   * @return
   */
  public abstract boolean samplePixel(int x, int y);
}