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

import com.jadice.barcode.BaseSettings;
import com.jadice.barcode.Symbology;
import com.jadice.barcode.linear.LinearCodeSettings;
import com.jadice.barcode.linear.OneDDecoder;

/**
 * A detector for Code 39 bar codes.
 */
public class Code39Decoder extends OneDDecoder {

  /** The code geometry descriptor */
  private static final CodeGeometry geometry = new CodeGeometry(9, 9, 9, 12, 12, 12, 10, true);

  @Override
  public Symbology getSymbology() {
    return new Code39();
  }

  @Override
  protected String decodeCodeString(CodeString string) {
    int[] codes = string.getCodes();

    Code39Settings settings = options.getSettings(Code39Settings.class);
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < codes.length; i++)
      sb.append((char) codes[i]);

    if (settings.isEnableFullASCII()) {
      for (int i = 0; i < sb.length() - 1; i++) {
        Character replacement = Code39Constants.FULLASCIICODEMAP.get(sb.substring(i, i + 2));
        if (null != replacement) {
          sb.delete(i, i + 2);
          sb.insert(i, replacement.charValue());
        }
      }
    }

    if (settings.isStripSurroundingAsterisks())
      sb.deleteCharAt(0).deleteCharAt(sb.length() - 1);

    return sb.toString();
  }

  @Override
  protected EdgeDetection detectStart(int[] barWidths, int offset, int barCount, int x, int y) {
    return detectAsterisk(barWidths, offset, barCount, x, y, false);
  }

  @Override
  protected EdgeDetection detectStop(int[] barWidths, int offset, int barCount, int x, int y) {
    return detectAsterisk(barWidths, offset, barCount, x, y, true);
  }

  private EdgeDetection detectAsterisk(int[] widths, int offset, int barCount, int x, int y, boolean isStop) {
    if (widths.length - offset < 9)
      return null;

    // verify bars
    if (classifyElements(widths, offset, 3, 2) != 6)
      return null;

    // verify spaces
    if (classifyElements(widths, offset + 1, 3, 1) != 8)
      return null;

    float avgBarWidthNarrow = (widths[offset + 0] + widths[offset + 2] + widths[offset + 8]) / 3f;
    // float avgBarWidthWide = (widths[4] + widths[6]) / 2f;

    float avgSpaceWidthNarrow = (widths[offset + 3] + widths[offset + 5] + widths[offset + 7]) / 3f;
    // float avgSpaceWidthWide = widths[1];

    float overprint = avgBarWidthNarrow / avgSpaceWidthNarrow;
    float overprintTolerance = options.getSettings(BaseSettings.class).getOverprintTolerance() / 100f;
    if (overprint > 1 + overprintTolerance || overprint < 1 - overprintTolerance)
      return null;

    int w = sumWidths(widths, offset, 9, 1);

    return new EdgeDetection(x + (isStop ? w : 0), y, w, overprint);
  }

  private int classifyElements(int[] barWidths, int offset, int narrow, int wide) {
    int width[] = new int[narrow + wide];
    for (int i = 0; i < width.length; i++)
      width[i] = barWidths[offset + 2 * i];

    int sortedWidths[] = new int[width.length];
    System.arraycopy(width, 0, sortedWidths, 0, width.length);
    Arrays.sort(sortedWidths);

    // check whether we have the correct wide/narrow elements
    if (!barRatioOk(sortedWidths[0], sortedWidths[narrow - 1], 1, 1, false, 1))
      return -1;
    if (!barRatioOk(sortedWidths[narrow], sortedWidths[narrow + wide - 1], 2, 2, false, 1))
      return -1;

    float wideAvgWidth = sumWidths(sortedWidths, narrow, wide, 1) / (float) wide;
    float narrowAvgWidth = sumWidths(sortedWidths, 0, narrow, 1) / (float) narrow;

    float wideToNarrowRatio = wideAvgWidth / narrowAvgWidth;
    float t = options.getSettings(LinearCodeSettings.class).getBarWidthTolerance() / 100f;
    if (clamp(wideToNarrowRatio, 2f - t, 3f + t) != wideToNarrowRatio)
      return -1;

    // assemble pattern
    float watershed = (wideAvgWidth + narrowAvgWidth) / 2;
    int result = 0;
    for (int i = 0; i < narrow + wide; i++) {
      result <<= 1;
      if (width[i] > watershed)
        result |= 1;
    }

    return result;
  }

  @Override
  protected CodeGeometry getCodeGeometry() {
    return geometry;
  }

  @Override
  protected CodeString decodeBarsToCodeString(int[] widths, int offset, int barCount, float overprintEstimate) {
    CodeString result = new CodeString();
    int confidence = 0;

    // ok, did we find a start code?
    if (offset + geometry.startCodeWindowSize < barCount && detectStart(widths, offset, 0, 0, 0) != null) {
      confidence++; // a start code is, well, a good start :-)

      // determine some geometric features
      float narrowBarWidth = (widths[offset] + widths[offset + 2] + widths[offset + 8]) / 3f;
      float wideBarWidth = (widths[offset + 4] + widths[offset + 6]) / 2f;
      float narrowSpaceWidth = (widths[offset + 3] + widths[offset + 5] + widths[offset + 7]) / 3f;
      float wideSpaceWidth = widths[offset + 1];

      float watershedBar = (wideBarWidth + narrowBarWidth) / 2;
      float watershedSpace = (wideSpaceWidth + narrowSpaceWidth) / 2;

      // store start code
      result.add('*');

      offset += geometry.startCodeWindowSize;
      offset++;


      // Step 2: detect codes (payload data)
      char symbol = (char) -1;
      while (offset + geometry.dataCodeWindowSize <= barCount) {
        // try to decode a symbol
        symbol = decodeCodeInWindow(widths, offset, watershedBar, watershedSpace);

        if (symbol >= 0) {
          confidence++;
          result.add(symbol);
          offset += 9 + 1;
        } else {
          confidence--;
          offset += 2;
        }
      }

      // expect terminating '*'
      if (symbol == '*') {
        confidence++;

        // determine/verify checksum
        Code39Settings settings = options.getSettings(Code39Settings.class);
        if (settings.isEnableChecksumVerification()) {
          if (settings.getChecksumVerifier().verifyChecksum(result)) {
            confidence += 5;
          }
        } else {
          result.setChecksumVerificationOK(true);
        }
      }

      // FIXME: check equal inter-symbol gaps
    }

    result.setConfidence(confidence);
    return result;
  }

  private char decodeCodeInWindow(int[] barWidths, int offset, float watershedBar, float watershedSpace) {
    int pattern = 0;
    for (int i = 0; i < 9; i += 2) {
      pattern *= 10;
      pattern += barWidths[offset + i] > watershedBar ? 2 : 1;
      if (i < 8) {
        pattern *= 10;
        pattern += barWidths[offset + i + 1] > watershedSpace ? 2 : 1;
      }
    }

    Character c = Code39Constants.SYMBOLSBYPATTERN.get(pattern);

    return (char) (null != c ? c : -1);
  }
}
