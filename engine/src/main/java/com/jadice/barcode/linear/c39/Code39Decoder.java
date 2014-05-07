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
import java.util.HashMap;
import java.util.Map;

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

  // @formatter:off
  private static final int[][] CODES = {
    //     | | | | |
    // one wide space, two wide bars
    {'0', 111221211}, //
    {'1', 211211112}, //
    {'2', 112211112}, //
    {'3', 212211111}, //
    {'4', 111221112}, //
    {'5', 211221111}, //
    {'6', 112221111}, //
    {'7', 111211212}, //
    {'8', 211211211}, //
    {'9', 112211211}, //
    {'A', 211112112}, //
    {'B', 112112112}, //
    {'C', 212112111}, //
    {'D', 111122112}, //
    {'E', 211122111}, //
    {'F', 112122111}, //
    {'G', 111112212}, //
    {'H', 211112211}, //
    {'I', 112112211}, //
    {'J', 111122211}, //
    {'K', 211111122}, //
    {'L', 112111122}, //
    {'M', 212111121}, //
    {'N', 111121122}, //
    {'O', 211121121}, //
    {'P', 112121121}, //
    {'Q', 111111222}, //
    {'R', 211111221}, //
    {'S', 112111221}, //
    {'T', 111121221}, //
    {'U', 221111112}, //
    {'V', 122111112}, //
    {'W', 222111111}, //
    {'X', 121121112}, //
    {'Y', 221121111}, //
    {'Z', 122121111}, //
    {'-', 121111212}, //
    {'.', 221111211}, //
    {' ', 122111211}, //
    // from here on only wide spaces...
    {'$', 121212111}, //
    {'/', 121211121}, //
    {'+', 121112121}, //
    {'%', 111212121}, //
    // ...except this beast
    {'*', 121121211}, //
  };
  
  private static final Object[][] FULL_ASCII_CODES = {
    {"%U", '\u0000'}, // 0: NUL
    {"$A", '\u0001'}, // 1: SOH
    {"$B", '\u0002'}, // 2: STX
    {"$C", '\u0003'}, // 3: ETX
    {"$D", '\u0004'}, // 4: EOT
    {"$E", '\u0005'}, // 5: ENQ
    {"$F", '\u0006'}, // 6: ACK
    {"$G", '\u0007'}, // 7: BEL
    {"$H", '\u0008'}, // 8: BS
    {"$I", '\u0009'}, // 9: HT
    {"$J", '\n'}, // 10: LF
    {"$K", '\u000b'}, // 11: VT
    {"$L", '\u000c'}, // 12: FF
    {"$M", '\r'}, // 13: CR
    {"$N", '\u000e'}, // 14: SO
    {"$O", '\u000f'}, // 15: SI
    {"$P", '\u0010'}, // 16: DLE
    {"$Q", '\u0011'}, // 17: DC1
    {"$R", '\u0012'}, // 18: DC2
    {"$S", '\u0013'}, // 19: DC3
    {"$T", '\u0014'}, // 20: DC4
    {"$U", '\u0015'}, // 21: NAK
    {"$V", '\u0016'}, // 22: SYN
    {"$W", '\u0017'}, // 23: ETB
    {"$X", '\u0018'}, // 24: CAN
    {"$Y", '\u0019'}, // 25: EM
    {"$Z", '\u001a'}, // 26: SUB
    {"%A", '\u001b'}, // 27: ESC
    {"%B", '\u001c'}, // 28: FS
    {"%C", '\u001d'}, // 29: GS
    {"%D", '\u001e'}, // 30: RS
    {"%E", '\u001f'}, // 31: US
    {"/A", '!'}, // 33: !
    {"/B", '"'}, // 34: "
    {"/C", '#'}, // 35: #
    {"/D", '$'}, // 36: $
    {"/E", '%'}, // 37: %
    {"/F", '&'}, // 38: &
    {"/G", '\''}, // 39: '
    {"/H", '('}, // 40: (
    {"/I", ')'}, // 41: )
    {"/J", '*'}, // 42: *
    {"/K", '+'}, // 43: +
    {"/L", ','}, // 44: ,
    {"/O", '/'}, // 47: /
    {"/Z", ':'}, // 58: :
    {"%F", ';'}, // 59: ;
    {"%G", '<'}, // 60: <
    {"%H", '='}, // 61: =
    {"%I", '>'}, // 62: >
    {"%J", '?'}, // 63: ?
    {"%V", '@'}, // 64: @
    {"%K", '['}, // 91: [
    {"%L", '\\'}, // 92: \
    {"%M", ']'}, // 93: ]
    {"%N", '^'}, // 94: ^
    {"%O", '_'}, // 95: _
    {"%W", '`'}, // 96: `
    {"+A", 'a'}, // 97: a
    {"+B", 'b'}, // 98: b
    {"+C", 'c'}, // 99: c
    {"+D", 'd'}, // 100: d
    {"+E", 'e'}, // 101: e
    {"+F", 'f'}, // 102: f
    {"+G", 'g'}, // 103: g
    {"+H", 'h'}, // 104: h
    {"+I", 'i'}, // 105: i
    {"+J", 'j'}, // 106: j
    {"+K", 'k'}, // 107: k
    {"+L", 'l'}, // 108: l
    {"+M", 'm'}, // 109: m
    {"+N", 'n'}, // 110: n
    {"+O", 'o'}, // 111: o
    {"+P", 'p'}, // 112: p
    {"+Q", 'q'}, // 113: q
    {"+R", 'r'}, // 114: r
    {"+S", 's'}, // 115: s
    {"+T", 't'}, // 116: t
    {"+U", 'u'}, // 117: u
    {"+V", 'v'}, // 118: v
    {"+W", 'w'}, // 119: w
    {"+X", 'x'}, // 120: x
    {"+Y", 'y'}, // 121: y
    {"+Z", 'z'}, // 122: z
    {"%P", '{'}, // 123: {
    {"%Q", '|'}, // 124: |
    {"%R", '}'}, // 125: }
    {"%S", '~'}, // 126: ~
    {"%T", '\u007f'}, // 127: DEL 
    {"%X", '\u007f'}, // 127: DEL 
    {"%Y", '\u007f'}, // 127: DEL 
    {"%Z", '\u007f'}, // 127: DEL 
  };
  // @formatter:on

  private static final Map<Integer, Character> symbolsByPattern = new HashMap<Integer, Character>();
  private static final Map<Character, Integer> indexBySymbol = new HashMap<Character, Integer>();

  private static final Map<String, Character> fullASCIICodeMap = new HashMap<String, Character>();

  // build/initialize the node tree
  static {
    for (int i = 0; i < CODES.length; i++) {
      symbolsByPattern.put(CODES[i][1], (char) CODES[i][0]);
      indexBySymbol.put((char) CODES[i][0], i);
    }

    for (int i = 0; i < FULL_ASCII_CODES.length; i++)
      fullASCIICodeMap.put((String) FULL_ASCII_CODES[i][0], (Character) FULL_ASCII_CODES[i][1]);
  }

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
        Character replacement = fullASCIICodeMap.get(sb.substring(i, i + 2));
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
        int[] codes = result.getCodes();
        if (settings.isEnableChecksumVerification()) {
          if (codes.length > 2) {
            int sum = 0;
            for (int i = 1; i < codes.length - 2; i++) {
              Integer s = indexBySymbol.get((char) codes[i]);
              if (null == s)
                break;
              sum += s;
            }

            Integer cs = indexBySymbol.get((char) codes[codes.length - 2]);
            if (null != cs && (sum % 43) == cs.intValue()) {
              result.setChecksumVerificationOK(true);
              confidence += 5;
            }
          }
        } else
          result.setChecksumVerificationOK(true);
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

    Character c = symbolsByPattern.get(pattern);

    return (char) (null != c ? c : -1);
  }
}
