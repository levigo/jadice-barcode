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
package com.jadice.barcode.linear.ean;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.jadice.barcode.Settings;
import com.jadice.barcode.Symbology;
import com.jadice.barcode.linear.LinearCodeSettings;
import com.jadice.barcode.linear.OneDDecoder;

/**
 * A detector for UPC and EAN Codes.
 * 
 */
public class EANDecoder extends OneDDecoder {

  public static final String NAME = "EAN/UPC-A";

  private static final int START = 10;
  private static final int STOP = 10;

  /**
   * EAN/UPC code table. Represented as integers with the relative bar widths at the digit
   * locations.
   */
  private static final int PATTERNS[] = {//
      3211, // 0
      2221, // 1
      2122, // 2
      1411, // 3
      1132, // 4
      1231, // 5
      1114, // 6
      1312, // 7
      1213, // 8
      3112, // 9
  };

  private static final int PARITY_PATTERNS[] = new int[]{//
      11111, // 0
      10100, // 1
      10010, // 2
      10001, // 3
      1100, // 4
      110, // 5
      11, // 6
      1010, // 7
      1001, // 8
      101
  // 9
  };

  private static int PARITY_TABLE[] = new int[32];

  private static final Map<Integer, Symbol> symbolsByPattern = new HashMap<Integer, Symbol>();

  // build/initialize the node tree
  static {
    for (int i = 0; i < PATTERNS.length; i++) {
      final int[] w = splitCodeString(PATTERNS[i], 4);
      // even parity
      symbolsByPattern.put(PATTERNS[i], new Symbol(w, i));
      // odd parity
      symbolsByPattern.put(PATTERNS[i], new Symbol(reverse(w), i + 10));
    }

    // init parity table
    Arrays.fill(PARITY_TABLE, -1);
    for (int i = 0; i < PARITY_PATTERNS.length; i++) {
      int pattern = PARITY_PATTERNS[i];
      int divident = 10000;
      int entry = 0;

      while (divident > 0) {
        int bit = pattern / divident % 10;
        if (bit < 0 || bit > 1)
          logger.fatal("Illegal parity pattern while building table " + pattern);
        else {
          entry <<= 1;
          entry |= bit;
        }
        divident /= 10;
      }

      PARITY_TABLE[entry] = i;
    }
  }

  /** The code geometry descriptor */
  private static CodeGeometry geometry = new CodeGeometry(3, 3, 4, 3, 3, 7, 9, true);

  /*
   * @see com.levigo.barcode.LinearCode#getCodeGeometry()
   */
  @Override
  protected CodeGeometry getCodeGeometry() {
    return geometry;
  }

  private static int[] reverse(int[] w) {
    int[] r = new int[w.length];
    for (int i = 0; i < w.length; i++)
      r[w.length - i - 1] = w[i];

    return r;
  }

  /*
   * @see com.levigo.barcode.LinearCode#decodeCodeString(com.levigo.barcode.AbstractCode
   * .CodeString)
   */
  @Override
  protected String decodeCodeString(CodeString codeString) {
    StringBuffer decodedData = new StringBuffer();
    int codes[] = codeString.getCodes();

    for (int code : codes)
      if (code == START || code == STOP)
        continue;
      else if (code >= 0)
        decodedData.append((char) ('0' + code));
      else
        decodedData.append(('?'));

    return decodedData.toString();
  }

  protected void verifyChecksum(CodeString codeString) {
    codeString.setChecksumVerificationOK(false);
    int[] codes = codeString.getCodes();
    if (codes.length == 14) {
      int sum = 0;
      for (int i = 1; i < 14; i++) {
        int code = codes[i];
        if (code < 0 || code > 9)
          return;
        sum += code * ((i & 1) == 0 ? 3 : 1);
      }

      if ((sum % 10) == 0)
        codeString.setChecksumVerificationOK(true);
    }
  }

  /**
   * Detect one EAN/UPC symbol at the given offset.
   * 
   * @param barWidths
   * @param offset
   * @param overprintEstimate TODO
   * @return
   */
  private Symbol detectCodeInWindow(int[] barWidths, int offset, int windowSize, int totalCodeModules, boolean tryHard,
      float overprintEstimate) {
    float pixelsPerModule = (float) sumWidths(barWidths, offset, windowSize, 1) / (float) totalCodeModules;

    float relativeBarWidths[] = new float[windowSize];

    int detectedCode = 0;
    for (int i = 0; i < windowSize; i++) {
      float relativeBarWidth = barWidths[offset + i] / pixelsPerModule;

      if ((i % 2) == 0) // black bar?
        relativeBarWidth -= overprintEstimate;
      else
        relativeBarWidth += overprintEstimate;

      relativeBarWidth = Math.round(relativeBarWidth);

      if ((int) relativeBarWidth < 1)
        relativeBarWidth = 1;
      if ((int) relativeBarWidth > 4)
        relativeBarWidth = 4;

      relativeBarWidths[i] = relativeBarWidth;

      detectedCode *= 10;
      detectedCode += relativeBarWidth;
    }

    if (symbolsByPattern.containsKey(detectedCode))
      return symbolsByPattern.get(detectedCode);

    if (!tryHard)
      return null;

    float leastMismatch = Float.MAX_VALUE;
    Symbol bestMatch = null;
    for (Map.Entry<Integer, Symbol> e : symbolsByPattern.entrySet()) {
      Symbol s = e.getValue();

      // calculate mismatch for this code
      float mismatch = 0f;
      float maxBarMismatch = 0f;
      for (int j = 0; j < windowSize; j++) {
        int w = s.widths[j];
        final float barMismatch = Math.abs(relativeBarWidths[j] - w);
        mismatch += barMismatch;
        maxBarMismatch = Math.max(maxBarMismatch, barMismatch);
      }

      if (mismatch < leastMismatch && maxBarMismatch < linearCodeSettings.getBarWidthTolerance() / 100f) {
        bestMatch = s;
        leastMismatch = mismatch;
      }
    }

    return bestMatch;
  }

  /*
   * @see com.levigo.barcode.LinearCode#decodeBarsToCodeString(int[], int)
   */
  @Override
  protected CodeString decodeBarsToCodeString(int[] barWidths, int offset, int barCount, float overprintEstimate) {
    CodeString result = new CodeString();
    int confidence = 0;

    int symbolPosition = 0;
    int position = 1;
    int parityBits = 0;
    decodeLoop : while ((symbolPosition <= 13 && offset + geometry.dataCodeWindowSize < barCount)
        || (symbolPosition == 14 && offset + geometry.stopCodeWindowSize < barCount)) {
      switch (symbolPosition){
        case 0 : // start bars
          while (offset + geometry.startCodeWindowSize < barCount)
            if (detectStart(barWidths, offset, barCount, 0, 0) != null)
              break;
            else
              offset++;

          if (offset + geometry.startCodeWindowSize >= barCount)
            break decodeLoop;

          result.add(START);
          result.add(-1); // this is where the number system digit will go
          confidence++; // a start code is, well, a good start :-)
          offset += geometry.startCodeWindowSize;
          break;

        case 14 : // stop bars
          if (detectStop(barWidths, offset, barCount, 0, 0) != null) {
            result.add(STOP);
            confidence++;
          }
          break;

        case 7 : // guard bars
          if (barRatioOk(barWidths[offset], barWidths[offset + 2], 1, 1, false, 1)
              && barRatioOk(barWidths[offset], barWidths[offset + 4], 1, 1, false, 1))
            confidence++;
          offset += 5;
          break;

        default :
          Symbol s = detectCodeInWindow(barWidths, offset, geometry.dataCodeWindowSize, geometry.moduleWidthData, true,
              overprintEstimate);

          if (null != s) {
            int code = s.value;

            parityBits <<= 1;
            if (code >= 10)
              code -= 10;
            else
              parityBits |= 0x1;

            confidence++;
            position++;

            result.add(code);
          }
          offset += geometry.dataCodeWindowSize;
      }

      symbolPosition++;
    }

    // determine number system
    int numberSystemDigit = PARITY_TABLE[(parityBits >> 6) & 0x1f];
    if (numberSystemDigit != -1)
      result.set(1, numberSystemDigit);
    else
      confidence -= 4;

    // the number of digits has to be 13 for EAN
    if (position == 13)
      confidence += 2;

    verifyChecksum(result);
    if (result.isChecksumVerificationOK())
      confidence += 10;

    result.setConfidence(confidence);

    return result;
  }

  /*
   * @see com.levigo.barcode.LinearCode#detectStop(int[], int, int, int, int, int, int)
   */
  @Override
  protected EdgeDetection detectStop(int[] barWidths, int offset, int barCount, int x, int y) {
    int totalWidthInWindow = sumWidths(barWidths, offset, geometry.stopCodeWindowSize, 1);

    // detecting a stop code is a bit easier than the start code.

    // We need a stop code, but also a the data code before it.
    if (offset < geometry.dataCodeWindowSize)
      return null;

    // calc total width of data code up to stop code
    int totalWidthOfPreviousCharacter = 0;
    for (int i = offset - geometry.dataCodeWindowSize; i < offset; i++)
      totalWidthOfPreviousCharacter += barWidths[i];

    // the start code width to data code width sould be around 3/7
    if (totalWidthOfPreviousCharacter * 3 > totalWidthInWindow * 9
        || totalWidthOfPreviousCharacter * 3 < totalWidthInWindow * 5)
      return null;

    if (!checkBasicStartStopGeometry(offset, barWidths))
      return null;

    // calculate and verify overprint
    float overprintEstimate = estimateOverprint(barWidths, offset);
    if (!within(overprintEstimate, 0f, baseSettings.getOverprintTolerance()))
      return null;

    return new EdgeDetection(x + totalWidthInWindow, y, totalWidthInWindow, overprintEstimate);
  }

  /*
   * @see com.levigo.barcode.LinearCode#detectStart(int[], int, int, int, int, int, int)
   */
  @Override
  protected EdgeDetection detectStart(int[] barWidths, int offset, int barCount, int x, int y) {
    int widthInWindow = sumWidths(barWidths, offset, geometry.startCodeWindowSize, 1);

    // detecting a start for UPC is not so easy, since the start pattern (nnn)
    // is fairly common and would produce far too many false hits without
    // additional
    // checks.

    // We will need at least a start code and one data code!
    if (barCount < offset - geometry.startCodeWindowSize - geometry.dataCodeWindowSize)
      return null;

    // calc total width of next character
    int widthOfNextCharacter = sumWidths(barWidths, offset + geometry.startCodeWindowSize, geometry.dataCodeWindowSize,
        1);

    // the start code width to data code width sould be around 3/7
    if (!barRatioOk(widthInWindow, widthOfNextCharacter, 3, 7, false, 3))
      return null;

    if (!checkBasicStartStopGeometry(offset, barWidths))
      return null;

    // calculate and verify overprint
    float overprintEstimate = estimateOverprint(barWidths, offset);
    if (!within(overprintEstimate, 0f, baseSettings.getOverprintTolerance()))
      return null;

    // final check: the next symbol must decode
    if (detectCodeInWindow(barWidths, offset + geometry.startCodeWindowSize, geometry.dataCodeWindowSize,
        geometry.moduleWidthData, true, 0f) == null)
      return null;

    return new EdgeDetection(x, y, widthInWindow, overprintEstimate);
  }

  /**
   * @param barWidths
   * @param offset
   * @return
   */
  private float estimateOverprint(int[] barWidths, int offset) {
    // the overprint estimate is calculated in units per bar, so we
    // divide the pixel difference by two bars
    int twoBlackUnits = barWidths[offset] + barWidths[offset + 2];
    int twoWhiteUnits = barWidths[offset + 1] + barWidths[offset + 3];
    float pixelsPerUnit = (float) (twoBlackUnits + twoWhiteUnits) / 4;
    float overprintEstimate = (twoBlackUnits - twoWhiteUnits) / pixelsPerUnit / 2f;
    return overprintEstimate;
  }

  private boolean checkBasicStartStopGeometry(int offset, int barWidths[]) {
    // the black bars should have approximately the same width.
    if (!barRatioOk(barWidths[offset], barWidths[offset + 2], 1, 1, false, 1))
      return false;

    // Although bars and spaces should be of the same width, we take the
    // bar-to-space
    // ratio liberally, because of possible ink spread. It should still be
    // reasonable,
    // i.e. not larger than 1:5
    if (!barRatioOk(barWidths[offset], barWidths[offset + 1], 1, 1, true, 1))
      return false;

    return true;
  }

  public Class<? extends Settings> getSettingsClass() {
    return LinearCodeSettings.class;
  }

  public String name() {
    return NAME;
  }

  @Override
  public Symbology getSymbology() {
    return new EAN();
  }
}