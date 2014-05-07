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
package com.jadice.barcode.linear.tofi;

import java.util.Arrays;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.jadice.barcode.AbstractDecodeTest;
import com.jadice.barcode.Configurer;
import com.jadice.barcode.Options;
import com.jadice.barcode.Symbology;
import com.jadice.barcode.linear.LinearCodeSettings;
import com.jadice.barcode.linear.LinearCodeSettings.Direction;
import com.jadice.barcode.linear.tofi.TwoOfFiveInterleaved;

@RunWith(Parameterized.class)
public class Code2of5interleaved extends AbstractDecodeTest {
  //@formatter:off
  private static Object PARAMS[][] = { //
    { "118_1818.jpg", "200168", new Configurer() {
      @Override
      public void configure(Options o) {
        o.getSettings(LinearCodeSettings.class).setQuietZoneTolerance(30);
      }
    }}, //
    { "118_1819.jpg", "200168", new Configurer() {
      @Override
      public void configure(Options o) {
        o.getSettings(LinearCodeSettings.class).setQuietZoneTolerance(30);
      }
    }}, //
    { "2iof5.png", "1022049452", null }, //
    { "baglab.png", "1022049452", null }, //
    { "barcode-east.png", "04657467", null }, //
    { "barcode-north.png", "04657467", null }, //
    { "barcode-south.png", "04657467", null }, //
    { "barcode-west.png", "04657467", null }, //
    { "IMG_0577.jpg", "HI345678", null }, //
    { "IMG_0578.jpg", "HI345678", null }, //
    { "nur2of5.png", "1022049452", null }, //
  };
  // @formatter:on

  @Parameters
  public static List<Object[]> parameters() {
    return Arrays.asList(PARAMS);
  }

  public Code2of5interleaved(String imageName, String expectedResult, Configurer c) {
    super(imageName, expectedResult, c);
  }

  @Override
  protected void initOptions(Options options) {
    options.getSettings(LinearCodeSettings.class).setScanInterval(20);
    options.getSettings(LinearCodeSettings.class).setDirectionEnabled(Direction.NORTH, true);
    options.getSettings(LinearCodeSettings.class).setDirectionEnabled(Direction.SOUTH, true);
    options.getSettings(LinearCodeSettings.class).setDirectionEnabled(Direction.WEST, true);

    super.initOptions(options);
  }

  @Override
  protected Symbology createSymbology() {
    return new TwoOfFiveInterleaved();
  }
}
