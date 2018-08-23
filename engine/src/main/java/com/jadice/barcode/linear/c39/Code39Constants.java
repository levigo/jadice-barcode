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

import java.util.HashMap;
import java.util.Map;

public class Code39Constants {

  // @formatter:off
  public static final int[][] CODES = {
      
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

  public static final Map<Integer, Character> SYMBOLSBYPATTERN = new HashMap<Integer, Character>();
  public static final Map<Character, Integer> INDEXBYSYMBOL = new HashMap<Character, Integer>();

  public static final Map<String, Character> FULLASCIICODEMAP = new HashMap<String, Character>();

  // build/initialize the node tree
  static {
    for (int i = 0; i < CODES.length; i++) {
      SYMBOLSBYPATTERN.put(CODES[i][1], (char) CODES[i][0]);
      INDEXBYSYMBOL.put((char) CODES[i][0], i);
    }

    for (int i = 0; i < FULL_ASCII_CODES.length; i++)
      FULLASCIICODEMAP.put((String) FULL_ASCII_CODES[i][0], (Character) FULL_ASCII_CODES[i][1]);
  }
}
