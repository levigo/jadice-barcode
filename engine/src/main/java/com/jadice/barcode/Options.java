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

import java.util.HashMap;
import java.util.Map;

/**
 * Options represent a collection of {@link Settings} instances (or rather implementations thereof)
 * which are specific to some particular aspect of the decoding process or a particular symbology.
 */
public class Options {
  private final Map<Class<? extends Settings>, Settings> settings = new HashMap<Class<? extends Settings>, Settings>();

  @SuppressWarnings("unchecked")
  public <S extends Settings> S getOptions(Class<S> setClass) {
    Settings s = settings.get(setClass);
    if (null == s) {
      try {
        s = setClass.newInstance();
        settings.put(setClass, s);
      } catch (Exception e) {
        // should not happen
        throw new RuntimeException("Can't instantiate the settings class " + setClass, e);
      }
    }

    return (S) s;
  }
}
