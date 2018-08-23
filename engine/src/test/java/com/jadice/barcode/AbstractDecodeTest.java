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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;

import com.jadice.barcode.j2d.BufferedImageLuminanceSource;

public abstract class AbstractDecodeTest {
  private final String imageName;
  private final String expectedResult;
  private static int testNo = 0;
  private final Configurer configurer;

  public AbstractDecodeTest(String imageName, String expectedResult) {
    this.imageName = imageName;
    this.expectedResult = expectedResult;
    this.configurer = null;
  }

  public AbstractDecodeTest(String imageName, String expectedResult, Configurer c) {
    this.imageName = imageName;
    this.expectedResult = expectedResult;
    this.configurer = c;
  }

  @Test
  public void testDecode() throws Exception {
    try {
      System.out.println((testNo++) + ": " + imageName);

      BufferedImage bi = loadImage();

      BufferedImageLuminanceSource luminanceSource = new BufferedImageLuminanceSource(bi);

      long start = System.currentTimeMillis();

      Options options = new Options();
      initOptions(options);
      options.getSettings(BaseSettings.class).setSymbologyEnabled(createSymbology().getClass(), true);

      List<Result> results = Detector.decode(options, luminanceSource);

      System.out.println(imageName + " took " + (System.currentTimeMillis() - start) + "ms");
      if (expectedResult != null) {
        if (results.isEmpty())
          Assert.fail(imageName.replaceAll(".*/", "") + ": nothing found");

        Result result = results.get(0);

        System.out.println("output: " + result.getCodeString());
        Assert.assertEquals("Decode " + imageName.replaceAll(".*/", ""), expectedResult, result.getCodeString());
      }
    } catch (AssertionError e) {
      throw e;
    } catch (Exception e) {
      AssertionError f = new AssertionError("Failed to decode " + imageName.replaceAll(".*/", ""));
      f.initCause(e);
      throw f;
    }
  }

  protected abstract Symbology createSymbology();

  protected void initOptions(Options options) {
    options.getSettings(BaseSettings.class).setThresholds(Arrays.asList(new Integer[]{50, 10, 25, 75}));
    options.getSettings(BaseSettings.class).setBarcodeCountLimit(1);
    if (null != configurer)
      configurer.configure(options);
  }

  protected BufferedImage loadImage() throws IOException {
    InputStream is = getClass().getResourceAsStream(imageName);
    if (null == is)
      Assert.fail("Input not found for " + imageName);

    BufferedImage bi = ImageIO.read(is);
    BufferedImage biCopy = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
    biCopy.createGraphics().drawImage(bi, 0, 0, null);
    return biCopy;
  }
}
