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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jadice.barcode.Settings;
import com.jadice.barcode.Symbology;
import com.jadice.barcode.linear.OneDDecoder;

/**
 * A detector implementatopn for a 2-of-5 interleaved code. Could be easily extended to cover 2-of-5
 * non-interleaved as well.
 * 
 */
public class TwoOfFiveInterleavedDecoder extends OneDDecoder {

  public static final String NAME = "2 of 5 interleaved";

  /** The 2-of-5 interleaved code table */
  private static final int[] PATTERNS = new int[]{ //
      11221, // 0
      21112, // 1
      12112, // 2
      22111, // 3
      11212, // 4
      21211, // 5
      12211, // 6
      11122, // 7
      21121, // 8
      12121, // 9
  };

  /** The start code pattern */
  private static final int STOP = 211;

  /** The stop code pattern */
  private static final int START = 1111;

  private static final Map<Integer, Symbol> symbolsByPattern = new HashMap<Integer, Symbol>();
  private static final List<Symbol> symbolByValue = new ArrayList<Symbol>(PATTERNS.length);

  // build/initialize the node tree
  static {
    for (int i = 0; i < PATTERNS.length; i++) {
      final Symbol s = new Symbol(splitCodeString(PATTERNS[i], 5), i);
      symbolsByPattern.put(PATTERNS[i], s);
      symbolByValue.add(s);
    }
  }

  /** The code geometry descriptor */
  private static CodeGeometry geometry = new CodeGeometry(4, 3, 5, 4, 4, 7, 7, false);

  /*
   * @see com.levigo.barcode.LinearCode#decodeBarsToCodeString(int[], int)
   */
  @Override
  protected CodeString decodeBarsToCodeString(int[] barWidths, int offset, int barCount, float overprintEstimate) {
    CodeString result = new CodeString();
    int confidence = 0;

    // ok, did we find a start code?
    if (offset + geometry.startCodeWindowSize < barCount && detectStart(barWidths, offset, 0, 0, 0) != null) {
      confidence++; // a start code is, well, a good start :-)

      // store start code
      result.add(START);

      offset += geometry.startCodeWindowSize;

      // Step 2: detect codes (payload data)
      final int dataStartOffset = offset;
      while (offset + geometry.stopCodeWindowSize <= barCount) {
        boolean isBlack = ((offset - dataStartOffset) & 1) == 0;

        // are there enough bars for a data code?
        if (offset + geometry.dataCodeWindowSize * 2 - 1 <= barCount) {
          // try to decode a symbol
          final Symbol symbol = detectCodeInWindow(barWidths, offset, geometry.dataCodeWindowSize,
              geometry.moduleWidthData, sumWidths(barWidths, offset, geometry.dataCodeWindowSize, 2), true);

          if (symbol != null) {
            int code = symbol.value;
            confidence++;
            result.add(code);
          } else
            confidence--;

          offset += isBlack ? 1 : geometry.dataCodeWindowSize * 2 - 1;

          if (null != symbol)
            continue;
        }

        // stop codes are expected at black bars
        if (isBlack && detectStop(barWidths, offset, barCount, 0, 0) != null) {
          result.add(STOP);
          confidence++;
          break;
        } else
          offset++;
      }

      // the number of digits has to be even for 2I0f5
      if ((result.size() & 1) == 0)
        confidence += 4;

      if (verifyChecksum(result))
        confidence += 4;
    }

    result.setConfidence(confidence);
    return result;
  }

  /**
   * Verifies the checksum of the passed result.
   * 
   * @param result
   * @return
   */
  private boolean verifyChecksum(CodeString result) {
    Code2of5Settings code2Of5Settings = options.getSettings(Code2of5Settings.class);
    if (!code2Of5Settings.isEnableChecksumVerification()) {
      result.setChecksumVerificationOK(true);
      return true;
    }
    return code2Of5Settings.getChecksumVerifier().verifyChecksum(result);
  }

  /*
   * @see com.levigo.barcode.LinearCode#decodeCodeString(com.levigo.barcode.AbstractCode
   * .CodeString)
   */
  @Override
  protected String decodeCodeString(CodeString codeString) {
    StringBuffer decodedData = new StringBuffer();
    int codes[] = codeString.getCodes();

    for (int i = 0; i < codes.length; i++) {
      int code = codes[i];

      // giant lookup/state machine for characters
      switch (code){
        case START :
          break;

        case STOP :
          if (i != codes.length - 1)
            decodedData.append('\uffff');
          break;

        default :
          if (code >= 0 && code <= 9)
            decodedData.append(code);
          else
            decodedData.append('\uffff');
          break;
      }
    }

    return decodedData.toString();
  }

  private Symbol detectCodeInWindow(int[] barWidths, int offset, int windowSize, int totalCodeModules,
      int totalWidthInWindow, boolean tryHard) {
    final int mytwiw = sumWidths(barWidths, offset, windowSize, 2);
    if (mytwiw != totalWidthInWindow)
      new Exception("TWIW mismatch").printStackTrace();

    totalWidthInWindow = mytwiw;

    float pixelsPerModule = (float) totalWidthInWindow / (float) totalCodeModules;

    float relativeBarWidths[] = new float[windowSize];

    int detectedCode = 0;
    for (int i = 0, p = offset; i < windowSize; i++, p += 2) {
      float relativeBarWidth = barWidths[p] / pixelsPerModule;
      relativeBarWidth = Math.round(relativeBarWidth);

      if ((int) relativeBarWidth < 1)
        relativeBarWidth = 1;
      if ((int) relativeBarWidth > 2)
        relativeBarWidth = 2;

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
      for (int j = 0; j < geometry.dataCodeWindowSize * 2 / 2; j++) {
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
   * Detecting a start for 2of5i is not so easy, since the start pattern (nnnn) is fairly common and
   * would produce far too many false hits without additional checks.
   * 
   * @see com.levigo.barcode.LinearCode#detectStart(int[], int, int, int, int, int)
   */
  @Override
  protected EdgeDetection detectStart(int[] barWidths, int offset, int barCount, int x, int y) {
    // We will need at least a start code and one data code!
    if (barCount < offset - geometry.startCodeWindowSize - geometry.dataCodeWindowSize * 2)
      return null;

    /*
     * the white and black bars should have approximately the same width (among their color).
     */
    if (!barRatioOk(barWidths[offset], barWidths[offset + 2], 1, 1, false, 1))
      return null;
    if (!barRatioOk(barWidths[offset + 1], barWidths[offset + 3], 1, 1, false, 1))
      return null;

    /*
     * Check ratios of start symbol to next data symbol widths, but, just like above, only among
     * bars of the same color.
     */
    int startBlackTotal = barWidths[offset] + barWidths[offset + 2];
    int nextDataBlackTotal = sumWidths(barWidths, offset + geometry.startCodeWindowSize, geometry.dataCodeWindowSize,
        2);
    if (!barRatioOk(startBlackTotal, nextDataBlackTotal, 2, geometry.moduleWidthData, false, 4))
      return null;

    int startWhiteTotal = barWidths[offset + 1] + barWidths[offset + 3];
    int nextDataWhiteTotal = sumWidths(barWidths, offset + geometry.startCodeWindowSize + 1,
        geometry.dataCodeWindowSize, 2);
    if (!barRatioOk(startWhiteTotal, nextDataWhiteTotal, 2, geometry.moduleWidthData, false, 4))
      return null;

    // Final check: the next character should decode
    if (detectCodeInWindow(barWidths, offset + geometry.startCodeWindowSize, geometry.dataCodeWindowSize,
        geometry.moduleWidthData, nextDataBlackTotal, false) == null)
      return null;
    if (detectCodeInWindow(barWidths, offset + geometry.startCodeWindowSize + 1, geometry.dataCodeWindowSize,
        geometry.moduleWidthData, nextDataWhiteTotal, false) == null)
      return null;

    /*
     * we just calculate the overprint based on the adjacent data code, but don't limit it at all.
     */
    float pixelsPerUnit = (float) (nextDataBlackTotal + nextDataWhiteTotal) / (2 * geometry.dataCodeWindowSize);
    float overprintEstimate = (nextDataBlackTotal - nextDataWhiteTotal) / pixelsPerUnit / 7;

    int startCodeWidth = sumWidths(barWidths, offset, geometry.startCodeWindowSize, 1);
    return new EdgeDetection(x, y, startCodeWidth, overprintEstimate);
  }

  // private int detectNonInterleavedCode(int[] barWidths, int offset,
  // int windowSize, int totalModules, int totalWidthInWindow) {
  // int code = 0;
  // // calculate total window width if not yet known
  // int windowEndOffset = offset + windowSize;
  // if (totalWidthInWindow < 0 || true) {
  // int twiwWas = totalWidthInWindow;
  // totalWidthInWindow = 0;
  // for (int i = offset; i < windowEndOffset; i++)
  // totalWidthInWindow += barWidths[i];
  // if (twiwWas != totalWidthInWindow && twiwWas != -1)
  // logger.warn("Supplied TWIW was bogus!");
  // }
  //
  // for (int i = 0; i < windowSize; i++) {
  // double relativeBarWidth = ((double) barWidths[offset + i] * totalModules)
  // / totalWidthInWindow;
  //
  // relativeBarWidth = Math.round(relativeBarWidth);
  // if (relativeBarWidth > 2)
  // relativeBarWidth = 2;
  // if (relativeBarWidth < 1)
  // relativeBarWidth = 1;
  //
  // code = code * 10 + (int) relativeBarWidth;
  // }
  // return code;
  // }

  /*
   * @see com.levigo.barcode.LinearCode#detectStop(int[], int, int, int, int, int)
   */
  @Override
  protected EdgeDetection detectStop(int[] barWidths, int offset, int barCount, int x, int y) {
    // detecting a stop code is a bit easier than the start code.

    // We need a stop code, but also a the start and data code before it.
    if (offset < geometry.startCodeWindowSize + geometry.dataCodeWindowSize * 2)
      return null;

    /*
     * the black bars should be at least 2:1
     */
    float ratio = (float) barWidths[offset] / barWidths[offset + 2];
    if (ratio < 2f - linearCodeSettings.getBarWidthTolerance() / 100f)
      return null;

    /*
     * Check ratios of start symbol to next data symbol widths, but, just like above, only among
     * bars of the same color.
     */
    int stopBlackTotal = barWidths[offset] + barWidths[offset + 2];
    int prevDataBlackTotal = sumWidths(barWidths, offset - geometry.dataCodeWindowSize * 2, geometry.dataCodeWindowSize,
        2);
    if (!barRatioOk(stopBlackTotal, prevDataBlackTotal, 3, geometry.moduleWidthData, false, 4))
      return null;

    int stopWhiteTotal = barWidths[offset + 1];
    int prevDataWhiteTotal = sumWidths(barWidths, offset - geometry.dataCodeWindowSize * 2 + 1,
        geometry.dataCodeWindowSize, 2);
    if (!barRatioOk(stopWhiteTotal, prevDataWhiteTotal, 1, geometry.moduleWidthData, false, 4))
      return null;

    // Final check: the previous character should decode
    if (detectCodeInWindow(barWidths, offset - geometry.dataCodeWindowSize * 2, geometry.dataCodeWindowSize,
        geometry.moduleWidthData, prevDataBlackTotal, false) == null)
      return null;

    /*
     * we just calculate the overprint based on the adjacent data code, but don't limit it at all.
     */
    float pixelsPerUnit = (float) (prevDataBlackTotal + prevDataWhiteTotal) / (2 * geometry.moduleWidthData);
    float overprintEstimate = (prevDataBlackTotal - prevDataWhiteTotal) / pixelsPerUnit / 7;

    int stopCodeWidth = stopBlackTotal + stopWhiteTotal;
    return new EdgeDetection(x + stopCodeWidth - 1, y, stopCodeWidth, overprintEstimate);
  }

  /*
   * @see com.levigo.barcode.LinearCode#getCodeGeometry()
   */
  @Override
  protected CodeGeometry getCodeGeometry() {
    return geometry;
  }

  public Class<? extends Settings> getSettingsClass() {
    return Code2of5Settings.class;
  }

  public String name() {
    return NAME;
  }

  @Override
  public Symbology getSymbology() {
    return new TwoOfFiveInterleaved();
  }
}
