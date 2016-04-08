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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jadice.barcode.Settings;
import com.jadice.barcode.Symbology;
import com.jadice.barcode.linear.LinearCodeSettings;
import com.jadice.barcode.linear.OneDDecoder;

/**
 * A detector for Code 128 bar codes.
 */
public class Code128Decoder extends OneDDecoder {

  public static final String NAME = "Code 128";

  /** Start code type A */
  private static final int START_A = 103; // 211412

  /** Start code type A */
  private static final int START_B = 104; // 211214

  /** Start code type C */
  private static final int START_C = 105; // 211232

  /** Stop code */
  private static final int STOP = 106; // 2331112

  /**
   * Code 128 code table. Represented as integers with the relative bar widths at the digit
   * locations.
   */
  private static final int PATTERNS[] = {//
      // "            VALUE   CODE A  CODE B  CODE C"
      212222, // "0       SP      SP      0       "
      222122, // "1       !       !       1       "
      222221, // "2       " "       2       "
      121223, // "3       #       #       3       "
      121322, // "4       $       $       4       "
      131222, // "5       %       %       5       "
      122213, // "6       &       &       6       "
      122312, // "7       '       '       7       "
      132212, // "8       (       (       8       "
      221213, // "9       )       )       9       "
      221312, // "10      *       *       10      "
      231212, // "11      +       +       11      "
      112232, // "12      ,       ,       12      "
      122132, // "13      -       -       13      "
      122231, // "14      .       .       14      "
      113222, // "15      /       /       15      "
      123122, // "16      0       0       16      "
      123221, // "17      1       1       17      "
      223211, // "18      2       2       18      "
      221132, // "19      3       3       19      "
      221231, // "20      4       4       20      "
      213212, // "21      5       5       21      "
      223112, // "22      6       6       22      "
      312131, // "23      7       7       23      "
      311222, // "24      8       8       24      "
      321122, // "25      9       9       25      "
      321221, // "26      :       :       26      "
      312212, // "27      ;       ;       27      "
      322112, // "28      <       <       28      "
      322211, // "29      =       =       29      "
      212123, // "30      >       >       30      "
      212321, // "31      ?       ?       31      "
      232121, // "32      @       @       32      "
      111323, // "33      A       A       33      "
      131123, // "34      B       B       34      "
      131321, // "35      C       C       35      "
      112313, // "36      D       D       36      "
      132113, // "37      E       E       37      "
      132311, // "38      F       F       38      "
      211313, // "39      G       G       39      "
      231113, // "40      H       H       40      "
      231311, // "41      I       I       41      "
      112133, // "42      J       J       42      "
      112331, // "43      K       K       43      "
      132131, // "44      L       L       44      "
      113123, // "45      M       M       45      "
      113321, // "46      N       N       46      "
      133121, // "47      O       O       47      "
      313121, // "48      P       P       48      "
      211331, // "49      Q       Q       49      "
      231131, // "50      R       R       50      "
      213113, // "51      S       S       51      "
      213311, // "52      T       T       52      "
      213131, // "53      U       U       53      "
      311123, // "54      V       V       54      "
      311321, // "55      W       W       55      "
      331121, // "56      X       X       56      "
      312113, // "57      Y       Y       57      "
      312311, // "58      Z       Z       58      "
      332111, // "59      [       [       59      "
      314111, // "60 \ \ 60 "
      221411, // "61      ]       ]       61      "
      431111, // "62      ^       ^       62      "
      111224, // "63      _       _       63      "
      111422, // "64      NUL             64      "
      121124, // "65      SOH     a       65      "
      121421, // "66      STX     b       66      "
      141122, // "67      ETX     c       67      "
      141221, // "68      EOT     d       68      "
      112214, // "69      ENQ     e       69      "
      112412, // "70      ACK     f       70      "
      122114, // "71      BEL     g       71      "
      122411, // "72      BS      h       72      "
      142112, // "73      HT      i       73      "
      142211, // "74      LF      j       74      "
      241211, // "75      VT      k       75      "
      221114, // "76      FF      l       76      "
      413111, // "77      CR      m       77      "
      241112, // "78      SO      n       78      "
      134111, // "79      SI      o       79      "
      111242, // "80      DLE     p       80      "
      121142, // "81      DC1     q       81      "
      121241, // "82      DC2     r       82      "
      114212, // "83      DC3     s       83      "
      124112, // "84      DC4     t       84      "
      124211, // "85      NAK     u       85      "
      411212, // "86      SYN     v       86      "
      421112, // "87      ETB     w       87      "
      421211, // "88      CAN     x       88      "
      212141, // "89      EM      y       89      "
      214121, // "90      SUB     z       90      "
      412121, // "91      ESC     {       91      "
      111143, // "92      FS      |       92      "
      111341, // "93      GS      }       93      "
      131141, // "94      RS      ~       94      "
      114113, // "95      US      DEL     95      "
      114311, // "96      FNC 3   FNC 3   96      "
      411113, // "97      FNC 2   FNC 2   97      "
      411311, // "98      SHIFT   SHIFT   98      "
      113141, // "99      CODE C  CODE C  99      "
      114131, // "100     CODE B  FNC 4   CODE B  "
      311141, // "101     FNC 4   CODE A  CODE A  "
      411131, // "102     FNC 1   FNC 1   FNC 1   "
      211412, // "103     START A                 "
      211214, // "104     START B                 "
      211232, // "105     START C                 "
      2331112
  // "106     STOP                    
  };

  /** The code table to decode code indices into characters. */
  private static final String CODE_TABLE_B = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_'abcdefghijklmnopqrstuvwxyz{|}~";

  private static final Map<Integer, Symbol> symbolsByPattern = new HashMap<Integer, Symbol>();
  private static final List<Symbol> symbolByValue = new ArrayList<Symbol>(PATTERNS.length);

  // build/initialize the node tree
  static {
    for (int i = 0; i < PATTERNS.length; i++) {
      final Symbol s = new Symbol(splitCodeString(PATTERNS[i], 7), i);
      symbolsByPattern.put(PATTERNS[i], s);
      symbolByValue.add(s);
    }
  }

  /** The code geometry descriptor */
  private static CodeGeometry geometry = new CodeGeometry(6, 7, 6, 11, 13, 11, 10, true);

  /*
   * @see com.levigo.barcode.LinearCode#getCodeGeometry()
   */
  @Override
  protected CodeGeometry getCodeGeometry() {
    return geometry;
  }

  /*
   * @see com.levigo.barcode.LinearCode#decodeCodeString(com.levigo.barcode.AbstractCode
   * .CodeString)
   */
  @Override
  protected String decodeCodeString(CodeString codeString) {
    StringBuffer decodedData = new StringBuffer();
    int codes[] = codeString.getCodes();

    int codeMode = START_A;
    int fallBackToMode = -1;
    for (int i = 0; i < codes.length; i++) {
      int code = codes[i];

      // skip the checksum character
      if (i == codes.length - 2 && codes[codes.length - 1] == STOP)
        continue;

      // giant lookup/state machine for characters
      switch (code){
        case START_A :
        case START_B :
        case START_C :
          codeMode = code;
          break;

        case STOP :
          if (i != codes.length - 1)
            decodedData.append('\uffff');
          break;

        case 100 :
          if (codeMode == START_B)
            decodedData.append("<FNC4>");
          else
            codeMode = START_B;
          break;

        case 101 :
          if (codeMode == START_A)
            decodedData.append("<FNC4>");
          else
            codeMode = START_A;
          break;

        case 102 :
          decodedData.append("<FNC1>");
          break;

        default :
          switch (codeMode){
            case START_A :
              if (code >= 0 && code <= 63)
                decodedData.append(CODE_TABLE_B.charAt(code));
              else if (code >= 64 && code <= 95)
                decodedData.append((char) (code - 64));
              else
                switch (code){
                  case 95 :
                    decodedData.append('\u001f');
                    break;
                  case 96 :
                    decodedData.append("<FNC3>");
                    break;
                  case 97 :
                    decodedData.append("<FNC2>");
                    break;
                  case 98 :
                    codeMode = START_B;
                    fallBackToMode = START_A;
                    continue;
                  case 99 :
                    codeMode = START_C;
                    break;
                  default :
                    decodedData.append('\uffff');
                }
              break;

            case START_B :
              if (code >= 0 && code < CODE_TABLE_B.length())
                decodedData.append(CODE_TABLE_B.charAt(code));
              else
                switch (code){
                  case 95 :
                    decodedData.append("<DEL>");
                    break;
                  case 96 :
                    decodedData.append("<FNC3>");
                    break;
                  case 97 :
                    decodedData.append("<FNC2>");
                    break;
                  case 98 :
                    codeMode = START_A;
                    fallBackToMode = START_B;
                    continue;
                  case 99 :
                    codeMode = START_C;
                    break;
                  default :
                    decodedData.append('\uffff');
                }
              break;

            case START_C :
              if (code >= 0 && code <= 99) {
                if (code < 10)
                  decodedData.append('0');
                decodedData.append(code);
              } else
                decodedData.append('\uffff');
              break;
          }
      }

      if (fallBackToMode != -1) {
        codeMode = fallBackToMode;
        fallBackToMode = -1;
      }
    }

    return decodedData.toString();
  }

  /**
   * Detect one Code 128 symbol at the given offset.
   * 
   * @param barWidths
   * @param offset
   * @param overprintEstimate
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

      relativeBarWidth = clamp(relativeBarWidth, 1, 4);

      relativeBarWidths[i] = relativeBarWidth;

      detectedCode *= 10;
      detectedCode += Math.round(relativeBarWidth);
    }

    if (symbolsByPattern.containsKey(detectedCode))
      return symbolsByPattern.get(detectedCode);

    if (!tryHard)
      return null;

    float leastMismatchSQ = Float.MAX_VALUE;
    Symbol bestMatch = null;
    for (Map.Entry<Integer, Symbol> e : symbolsByPattern.entrySet()) {
      Symbol s = e.getValue();

      // calculate mismatch for this code
      float mismatchSQ = 0f;
      float maxBarMismatchSQ = 0f;
      for (int j = 0; j < windowSize; j++) {
        int w = s.widths[j];
        final float barMismatch = Math.abs(relativeBarWidths[j] - w);
        mismatchSQ += barMismatch * barMismatch;
        maxBarMismatchSQ = Math.max(maxBarMismatchSQ, barMismatch);
      }

      if (mismatchSQ < leastMismatchSQ) {
        bestMatch = s;
        leastMismatchSQ = mismatchSQ;
      }
    }

    // if(maxBarMismatchSQ > 1 + options.getBarWidthTolerance() / 100f)

    return bestMatch;
  }

  /**
   * Detect one Code 128 symbol at the given offset.
   * 
   * @param barWidths
   * @param offset
   * @param overprintEstimate
   * @return
   */
  private Symbol guessStartCode(int[] barWidths, int offset) {
    float pixelsPerModule = (float) sumWidths(barWidths, offset, geometry.startCodeWindowSize, 1)
        / (float) geometry.moduleWidthStart;

    float relativeBarWidths[] = new float[geometry.startCodeWindowSize];

    for (int i = 0; i < geometry.startCodeWindowSize; i++) {
      float relativeBarWidth = clamp(barWidths[offset + i] / pixelsPerModule, 1, 4);

      relativeBarWidths[i] = relativeBarWidth;
    }

    float leastMismatchSQ = Float.MAX_VALUE;
    Symbol bestMatch = null;
    for (int code = START_A; code <= START_C; code++) {
      Symbol s = symbolByValue.get(code);

      // calculate mismatch for this code
      float mismatchSQ = 0f;
      float maxBarMismatchSQ = 0f;
      for (int j = 0; j < geometry.startCodeWindowSize; j++) {
        int w = s.widths[j];
        final float barMismatch = Math.abs(relativeBarWidths[j] - w);
        mismatchSQ += barMismatch * barMismatch;
        maxBarMismatchSQ = Math.max(maxBarMismatchSQ, barMismatch);
      }

      if (mismatchSQ < leastMismatchSQ) {
        bestMatch = s;
        leastMismatchSQ = mismatchSQ;
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

    // ok, can we see a start code right at the start?
    if (offset + geometry.startCodeWindowSize < barCount && detectStart(barWidths, offset, 0, 0, 0) != null) {
      // re-detect start code using regular decoding
      Symbol s = detectCodeInWindow(barWidths, offset, geometry.startCodeWindowSize, geometry.moduleWidthData, true,
          overprintEstimate);

      // this decoding might fail, due to differences in the algorithms
      // used to detect start symbols vs. regular decoding.
      if (null != s && s.value >= START_A && s.value <= START_C) {
        confidence++; // a start code is, well, a good start :-)
        result.add(s.value);

        offset += geometry.startCodeWindowSize;

        // Step 2: detect codes (payload data)
        while (offset + geometry.dataCodeWindowSize < barCount) {
          s = detectCodeInWindow(barWidths, offset, geometry.startCodeWindowSize, geometry.moduleWidthData, true,
              overprintEstimate);

          if (null != s) {
            confidence++;
            result.add(s.value);

            if (s.value == STOP) {
              confidence++; // add extra boost for codes ending in STOP
              break;
            }
          }
          offset += geometry.dataCodeWindowSize;
        }

        // code holds the last code now. it should be the checksum.
        Code128Settings settings = options.getSettings(Code128Settings.class);
        if (settings.getChecksumVerifier().verifyChecksum(result)) {
          confidence += 5;
        }
      }
    }

    result.setConfidence(confidence);

    return result;
  }

  /*
   * @see com.levigo.barcode.LinearCode#detectStop(int[], int, int, int, int, int, int) 2331112
   */
  @Override
  protected EdgeDetection detectStop(int[] barWidths, int offset, int barCount, int x, int y) {
    // black bars alone
    if (!barRatioOk(barWidths[offset], barWidths[offset + 2], 2, 3, true, 1))
      return null;
    if (!barRatioOk(barWidths[offset], barWidths[offset + 4], 2, 1, true, 1))
      return null;
    if (!barRatioOk(barWidths[offset], barWidths[offset + 6], 2, 2, true, 1))
      return null;

    // white bars alone
    if (!barRatioOk(barWidths[offset + 1], barWidths[offset + 3], 3, 1, true, 1))
      return null;
    if (!barRatioOk(barWidths[offset + 1], barWidths[offset + 5], 3, 1, true, 1))
      return null;

    float overprintEstimate = estimateOverprint(barWidths, offset, geometry.stopCodeWindowSize,
        symbolByValue.get(STOP));

    if (!within(overprintEstimate, 0f, baseSettings.getOverprintTolerance()))
      return null;

    int stopCodeWidth = sumWidths(barWidths, offset, geometry.stopCodeWindowSize, 1);
    EdgeDetection edgeDetection = new EdgeDetection(x + stopCodeWidth - 1, y, stopCodeWidth, overprintEstimate);

    return edgeDetection;
  }

  /*
   * @see com.levigo.barcode.LinearCode#detectStart(int[], int, int, int, int, int, int)
   */
  @Override
  protected EdgeDetection detectStart(int[] barWidths, int offset, int barCount, int x, int y) {
    final int b0 = barWidths[offset];
    final int b1 = barWidths[offset + 1];
    final int b2 = barWidths[offset + 2];
    final int b3 = barWidths[offset + 3];
    // final int b4 = barWidths[offset + 4];
    final int b5 = barWidths[offset + 5];

    // first to third bar must be roughly 2:1
    if (!barRatioOk(b0, b2, 2, 1, true, 1))
      return null;

    // sixth and/or fourth bar must be 2 units wide
    int twoWhite = Math.min(b3, b5);
    if (!barRatioOk(b1, twoWhite, 1, 2, true, 1))
      return null;

    final int startCodeWidth = sumWidths(barWidths, offset, geometry.startCodeWindowSize, 1);

    // Make a rough guess at which kind of start code this is:
    // START_A (103): 211412
    // START_B (104): 211214
    // START_C (105): 211232
    Symbol guess = guessStartCode(barWidths, offset);

    // estimate the overprint, based on the guess
    float overprintEstimate = estimateOverprint(barWidths, offset, geometry.startCodeWindowSize, guess);
    if (!within(overprintEstimate, 0f, baseSettings.getOverprintTolerance()))
      return null;

    // now simply decode one character and check whether it is a valid start
    // code
    Symbol s = detectCodeInWindow(barWidths, offset, geometry.startCodeWindowSize, geometry.moduleWidthStart, false,
        overprintEstimate);

    if (s == guess)
      return new EdgeDetection(x, y, //
          startCodeWidth,
          // update overprint estimate to be more precise
          estimateOverprint(barWidths, offset, geometry.startCodeWindowSize, s));

    return null;
  }

  private float estimateOverprint(int[] barWidths, int offset, int windowSize, Symbol s) {
    int split[] = s.widths;
    if (null == split)
      throw new IllegalArgumentException("Can't estimate overprint for illegal code");

    int blackUnits = 0;
    int blackPixels = 0;
    for (int i = 0; i < windowSize; i += 2) {
      blackUnits += split[i];
      blackPixels += barWidths[i + offset];
    }

    int whiteUnits = 0;
    int whitePixels = 0;
    for (int i = 1; i < windowSize; i += 2) {
      whiteUnits += split[i];
      whitePixels += barWidths[i + offset];
    }

    float pixelsPerUnit = (float) (blackPixels + whitePixels) / (blackUnits + whiteUnits);

    final float b = blackPixels / pixelsPerUnit;
    final float w = whitePixels / pixelsPerUnit;

    float bo = b - blackUnits; // black overprint
    float wo = w - whiteUnits; // white overprint (usually negative)

    // total overprint estimate averages black/white overprint
    return (bo - wo) / (windowSize / 2f) / 2f;
  }

  public Class<? extends Settings> getSettingsClass() {
    return LinearCodeSettings.class;
  }

  @Override
  public Symbology getSymbology() {
    return new Code128();
  }
}
