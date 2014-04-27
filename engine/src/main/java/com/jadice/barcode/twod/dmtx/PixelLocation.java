/*
 * This file is part of a java-port of the libdmtx library.
 * 
 * Copyright (C) 2014 levigo solutions gmbh Contact: solutions@levigo.de
 * 
 * 
 * The original library's copyright statement follows:
 * 
 * libdmtx - Data Matrix Encoding/Decoding Library
 * 
 * Copyright (C) 2011 Mike Laughton
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
 * Contact: mike@dragonflylogic.com
 */
package com.jadice.barcode.twod.dmtx;

/**
 * @struct DmtxPixelLoc
 * @brief DmtxPixelLoc
 */
public class PixelLocation {
  int x;
  int y;

  public PixelLocation() {
  }

  public PixelLocation(PixelLocation loc) {
    x = loc.x;
    y = loc.y;
  }

  @Override
  public PixelLocation clone() {
    return new PixelLocation(this);
  }

  /**
   *
   *
   */
  public long distanceSquared(PixelLocation b) {
    int xDelta = x - b.x;
    int yDelta = y - b.y;
    return xDelta * xDelta + yDelta * yDelta;
  }

  @Override
  public String toString() {
    return "(" + x + "," + y + ")";
  }
}