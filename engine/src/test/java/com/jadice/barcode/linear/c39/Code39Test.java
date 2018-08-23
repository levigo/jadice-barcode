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
package com.jadice.barcode.linear.c39;

import java.util.Arrays;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.jadice.barcode.AbstractDecodeTest;
import com.jadice.barcode.Configurer;
import com.jadice.barcode.Options;
import com.jadice.barcode.Symbology;

@RunWith(Parameterized.class)
public class Code39Test extends AbstractDecodeTest {
  //@formatter:off
  private static Object PARAMS[][] = { //
    { "279584391_14f7b9e27e_o.png", "*1234567auhasuhUHAUSD*", null }, //
    { "279584391_14f7b9e27e_o.png", "*1234567+A+U+H+A+S+U+HUHAUSD*", new Configurer() {
      @Override
      public void configure(Options o) {
        o.getSettings(Code39Settings.class).setEnableFullASCII(false);
      }
    }}, //
    { "735px-Code39_Wikipedia.svg.png", "*WIKIPEDIA$*", null }, //
    { "800px-Code_3_of_9.svg.png", "*WIKIPEDIA*", null }, //
    { "800px-Code_3_of_9.svg.png", "WIKIPEDIA", new Configurer() {
      @Override
      public void configure(Options o) {
        o.getSettings(Code39Settings.class).setStripSurroundingAsterisks(true);
      }
    }}, //
  };
  // @formatter:on

  @Parameters
  public static List<Object[]> parameters() {
    return Arrays.asList(PARAMS);
  }

  public Code39Test(String imageName, String expectedResult, Configurer c) {
    super(imageName, expectedResult, c);
  }

  @Override
  protected Symbology createSymbology() {
    return new Code39();
  }
}
