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
package com.jadice.barcode;


/**
 * This interface is the main SPI for implementations of bar code symbologies.
 */
public interface Symbology {
  /**
   * Return the {@link Settings} implementation used by this symbology.
   * 
   * @return the Settings implementation class or <code>null</code> if this symbology doesn't have
   *         any private settings.
   */
  Class<? extends Settings> getSettingsClass();

  /**
   * Return the name of this symbology.
   * 
   * @return the name
   */
  String name();

  /**
   * Return whether this symbology is able to decode bar codes. Currently this library is
   * decode-only. Therefore all implementations must return <code>true</code>.
   * 
   * @return whether this symbology is able to decode bar codes
   */
  boolean canDecode();

  /**
   * Create a {@link Decoder} instance for this symbology. Only implementations which returned
   * <code>true</code> from {@link #canDecode()} are required to support this method.
   * 
   * @return a {@link Decoder}
   */
  Decoder createDecoder();
}
