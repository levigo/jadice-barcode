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
package com.jadice.barcode.linear.c128;

import java.util.Arrays;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.jadice.barcode.AbstractDecodeTest;
import com.jadice.barcode.BaseSettings;
import com.jadice.barcode.Configurer;
import com.jadice.barcode.Options;
import com.jadice.barcode.Symbology;
import com.jadice.barcode.linear.c128.Code128;

@RunWith(Parameterized.class)
public class Code128Test extends AbstractDecodeTest {
  //@formatter:off
  private static Object PARAMS[][] = { //
    { "c128-1.png", "HI345678", null }, //
    { "freigestellt.png", "levigo test barcode 128B", null }, //
    { "levigo test.png", "levigo test barcode 128B", null }, //
    { "levigo-auftrag.png", "K21022", null }, //
    { "nimerisch.png", "12801234567890", null }, //
    { "rotationsartefakte.png", "levigo test barcode 128B", null }, //
    { "skaliert, rotiert, noch kleiner.png", "levigo test barcode 128B", null }, //
    { "skaliert, rotiert.png", "levigo test barcode 128B", new Configurer() {
      @Override
      public void configure(Options o) {
        o.getSettings(BaseSettings.class).setThreshold(50);
      }
    } }, //
    // FIXME: why?
    { "skaliert.png", null, null }, //
    { "weniger fieses rauschen.png", "levigo test barcode 128B", null }, //
    { "fieses rauschen.png", "levigo test barcode 128B", new Configurer() {
      @Override
      public void configure(Options o) {
        o.getSettings(BaseSettings.class).setThreshold(60);
      }
    } }, //
    { "einschusslöcher.png", "levigo test barcode 128B", null }, //
    { "Barcode_Code128_wikipedia.png", "wikipedia", null }, //
    { "angefressene ecke.png", "levigo test barcode 128B", null }, //
    { "alle richtungen.png", "wikipedia" , null }, //
  };
  // @formatter:on

  @Parameters
  public static List<Object[]> parameters() {
    return Arrays.asList(PARAMS);
  }

  public Code128Test(String imageName, String expectedResult, Configurer c) {
    super(imageName, expectedResult, c);
  }

  @Override
  protected Symbology createSymbology() {
    return new Code128();
  }
}
