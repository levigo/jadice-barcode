/*
 * This file is part of a java-port of the libdmtx library.
 * 
 * Copyright (C) 2014 levigo solutions gmbh Contact: solutions@levigo.de
 * 
 * 
 * The original library's copyright statement follows:
 * 
 * libdmtx - Data Matrix Encoding/Decoding Library
 * 
 * Copyright (C) 2011 Mike Laughton
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
 * Contact: mike@dragonflylogic.com
 */
package com.jadice.barcode.twod.dmtx;

import static java.lang.Math.max;

import java.util.Arrays;

public class ReedSolomon {

  public static class ByteList {
    private int length;
    private final byte b[];

    public ByteList(int capacity) {
      b = new byte[capacity];
    }

    public void append(int val, int length) {
      Arrays.fill(b, (byte) val);
      this.length = length;
    }

    public void clear() {
      Arrays.fill(b, (byte) 0);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
      final ByteList clone = new ByteList(b.length);
      System.arraycopy(b, 0, clone.b, 0, b.length);
      clone.length = length;
      return clone;
    }

    public void append(int val) {
      if (length >= b.length)
        throw new ArrayIndexOutOfBoundsException();
      b[length++] = (byte) val;
    }

    public int pop() {
      if (length <= 0)
        throw new ArrayIndexOutOfBoundsException();
      return b[--length] & 0xff;
    }

    public int get(int index) {
      return b[index] & 0xff;
    }

    public void checkBounds(int i) {
      if (i < 0 || i >= length || length > b.length)
        throw new ArrayIndexOutOfBoundsException("bounds check");
    }

    public void copyFrom(ByteList src) {
      if (b.length < src.length)
        throw new ArrayIndexOutOfBoundsException();

      /*
       * Copy as many bytes as dst can hold or src can provide (smaller of two)
       */
      System.arraycopy(src.b, 0, b, 0, Math.min(b.length, src.b.length));
      length = src.length;
    }

    public void set(int i, int val) {
      b[i] = (byte) val;
    }

    public void setLength(int length) {
      this.length = length;
    }

    public int getLength() {
      return length;
    }
  }

  private static final int NN = 255;
  private static final int MAX_ERROR_WORD_COUNT = 68;
  /* GF(256) log values using primitive polynomial 301 */
  private static int log301[] = {
      255, 0, 1, 240, 2, 225, 241, 53, 3, 38, 226, 133, 242, 43, 54, 210, 4, 195, 39, 114, 227, 106, 134, 28, 243, 140,
      44, 23, 55, 118, 211, 234, 5, 219, 196, 96, 40, 222, 115, 103, 228, 78, 107, 125, 135, 8, 29, 162, 244, 186, 141,
      180, 45, 99, 24, 49, 56, 13, 119, 153, 212, 199, 235, 91, 6, 76, 220, 217, 197, 11, 97, 184, 41, 36, 223, 253,
      116, 138, 104, 193, 229, 86, 79, 171, 108, 165, 126, 145, 136, 34, 9, 74, 30, 32, 163, 84, 245, 173, 187, 204,
      142, 81, 181, 190, 46, 88, 100, 159, 25, 231, 50, 207, 57, 147, 14, 67, 120, 128, 154, 248, 213, 167, 200, 63,
      236, 110, 92, 176, 7, 161, 77, 124, 221, 102, 218, 95, 198, 90, 12, 152, 98, 48, 185, 179, 42, 209, 37, 132, 224,
      52, 254, 239, 117, 233, 139, 22, 105, 27, 194, 113, 230, 206, 87, 158, 80, 189, 172, 203, 109, 175, 166, 62, 127,
      247, 146, 66, 137, 192, 35, 252, 10, 183, 75, 216, 31, 83, 33, 73, 164, 144, 85, 170, 246, 65, 174, 61, 188, 202,
      205, 157, 143, 169, 82, 72, 182, 215, 191, 251, 47, 178, 89, 151, 101, 94, 160, 123, 26, 112, 232, 21, 51, 238,
      208, 131, 58, 69, 148, 18, 15, 16, 68, 17, 121, 149, 129, 19, 155, 59, 249, 70, 214, 250, 168, 71, 201, 156, 64,
      60, 237, 130, 111, 20, 93, 122, 177, 150
  };
  /* GF(256) antilog values using primitive polynomial 301 */
  private static int antilog301[] = {
      1, 2, 4, 8, 16, 32, 64, 128, 45, 90, 180, 69, 138, 57, 114, 228, 229, 231, 227, 235, 251, 219, 155, 27, 54, 108,
      216, 157, 23, 46, 92, 184, 93, 186, 89, 178, 73, 146, 9, 18, 36, 72, 144, 13, 26, 52, 104, 208, 141, 55, 110,
      220, 149, 7, 14, 28, 56, 112, 224, 237, 247, 195, 171, 123, 246, 193, 175, 115, 230, 225, 239, 243, 203, 187, 91,
      182, 65, 130, 41, 82, 164, 101, 202, 185, 95, 190, 81, 162, 105, 210, 137, 63, 126, 252, 213, 135, 35, 70, 140,
      53, 106, 212, 133, 39, 78, 156, 21, 42, 84, 168, 125, 250, 217, 159, 19, 38, 76, 152, 29, 58, 116, 232, 253, 215,
      131, 43, 86, 172, 117, 234, 249, 223, 147, 11, 22, 44, 88, 176, 77, 154, 25, 50, 100, 200, 189, 87, 174, 113,
      226, 233, 255, 211, 139, 59, 118, 236, 245, 199, 163, 107, 214, 129, 47, 94, 188, 85, 170, 121, 242, 201, 191,
      83, 166, 97, 194, 169, 127, 254, 209, 143, 51, 102, 204, 181, 71, 142, 49, 98, 196, 165, 103, 206, 177, 79, 158,
      17, 34, 68, 136, 61, 122, 244, 197, 167, 99, 198, 161, 111, 222, 145, 15, 30, 60, 120, 240, 205, 183, 67, 134,
      33, 66, 132, 37, 74, 148, 5, 10, 20, 40, 80, 160, 109, 218, 153, 31, 62, 124, 248, 221, 151, 3, 6, 12, 24, 48,
      96, 192, 173, 119, 238, 241, 207, 179, 75, 150, 0
  };

  /**
   * \brief Retrieve data size for a specific symbol size and block number \param sizeIdx \param
   * blockIdx \return Attribute value
   */
  private static int dmtxGetBlockDataSize(SymbolSize size, int blockIdx) {
    if (size.symbolDataWords < 1 || size.interleavedBlocks < 1)
      return DatamatrixDecoder.DmtxUndefined;

    int count = size.symbolDataWords / size.interleavedBlocks;

    return size == SymbolSize.Fixed144x144 && blockIdx < 8 ? count + 1 : count;
  }

  /**
   * \brief Determine symbol size based on data size and requested properties \param dataWords
   * \param sizeIdxRequest \return Symbol size index (or DmtxUndefined if none)
   */
  @SuppressWarnings("unused")
  private static int FindSymbolSize(int dataWords, SymbolSize sizeIdxRequest) {
    int sizeIdx;
    int idxBeg, idxEnd;

    if (dataWords <= 0)
      return DatamatrixDecoder.DmtxUndefined;

    if (sizeIdxRequest == SymbolSize.SquareAuto || sizeIdxRequest == SymbolSize.RectAuto) {
      if (sizeIdxRequest == SymbolSize.SquareAuto) {
        idxBeg = 0;
        idxEnd = Region.DmtxSymbolSquareCount;
      } else {
        idxBeg = Region.DmtxSymbolSquareCount;
        idxEnd = Region.DmtxSymbolSquareCount + Region.DmtxSymbolRectCount;
      }

      for (sizeIdx = idxBeg; sizeIdx < idxEnd; sizeIdx++)
        if (SymbolSize.values()[sizeIdx].symbolDataWords >= dataWords)
          break;

      if (sizeIdx == idxEnd)
        return DatamatrixDecoder.DmtxUndefined;
    } else
      sizeIdx = sizeIdxRequest.ordinal();

    if (dataWords > SymbolSize.values()[sizeIdx].symbolDataWords)
      return DatamatrixDecoder.DmtxUndefined;

    return sizeIdx;
  }

  /* GF add (a + b) */
  private static int gfAdd(int a, int b) {
    return (a ^ b) & 0xff;
  }

  /* GF multiply (a * b) */
  private static int gfMult(int a, int b) {
    return (a == 0 || b == 0 ? 0 : antilog301[(log301[a] + log301[b]) % NN]) & 0xff;
  }

  /* GF multiply by antilog (a * alpha**b) */
  private static int gfMultAntilog(int a, int b) {
    return (a == 0 ? 0 : antilog301[(log301[a] + b) % NN]) & 0xff;
  }

  /**
   * Encode xyz. More detailed description. \param message \param sizeIdx \return Function success
   * (DmtxPass|DmtxFail)
   */
  static void encode(Message message, SymbolSize sizeIdx) {
    final ByteList gen = new ByteList(MAX_ERROR_WORD_COUNT);
    final ByteList ecc = new ByteList(MAX_ERROR_WORD_COUNT);

    int blockStride = sizeIdx.interleavedBlocks;
    int blockErrorWords = sizeIdx.blockErrorWords;
    int symbolDataWords = sizeIdx.symbolDataWords;
    int symbolErrorWords = sizeIdx.symbolErrorWords;
    int symbolTotalWords = symbolDataWords + symbolErrorWords;

    /* Populate generator polynomial */
    RsGenPoly(gen, blockErrorWords);

    /* For each interleaved block... */
    for (int blockIdx = 0; blockIdx < blockStride; blockIdx++) {
      /* Generate error codewords */
      ecc.append(0, blockErrorWords);
      for (int i = blockIdx; i < symbolDataWords; i += blockStride) {
        int val = gfAdd(ecc.get(blockErrorWords - 1), message.code[i] & 0xff);

        for (int j = blockErrorWords - 1; j > 0; j--) {
          ecc.checkBounds(j);
          ecc.checkBounds(j - 1);
          gen.checkBounds(j);
          ecc.set(j, gfAdd(ecc.get(j - 1), gfMult(gen.get(j), val)));
        }

        ecc.set(0, gfMult(gen.get(0), val));
      }

      /* Copy to output message */
      int eccPtr = blockErrorWords;
      for (int i = symbolDataWords + blockIdx; i < symbolTotalWords; i += blockStride)
        message.code[i] = (byte) ecc.get(--eccPtr);

      assert 0 == eccPtr;
    }
  }

  /**
   * Decode xyz. More detailed description. \param code \param sizeIdx \param fix \return Function
   * success (DmtxPass|DmtxFail)
   */
  static boolean decode(byte code[], SymbolSize sizeIdx, int fix) {
    final ByteList elp = new ByteList(MAX_ERROR_WORD_COUNT);
    final ByteList syn = new ByteList(MAX_ERROR_WORD_COUNT + 1);
    final ByteList rec = new ByteList(NN);
    final ByteList loc = new ByteList(NN);

    int blockStride = sizeIdx.interleavedBlocks;
    int blockErrorWords = sizeIdx.blockErrorWords;
    int blockMaxCorrectable = sizeIdx.blockMaxCorrectable;
    int symbolDataWords = sizeIdx.symbolDataWords;
    int symbolErrorWords = sizeIdx.symbolErrorWords;
    int symbolTotalWords = symbolDataWords + symbolErrorWords;

    /* For each interleaved block */
    for (int blockIdx = 0; blockIdx < blockStride; blockIdx++) {
      /*
       * Data word count depends on blockIdx due to special case at 144x144
       */
      int blockDataWords = dmtxGetBlockDataSize(sizeIdx, blockIdx);
      /* Populate received list (rec) with data and error codewords */
      rec.append((byte) 0, 0);

      /* Start with final error word and work backward */
      int wordP = symbolTotalWords + blockIdx - blockStride;
      for (int i = 0; i < blockErrorWords; i++) {
        rec.append(code[wordP]);
        wordP -= blockStride;
      }

      /* Start with final data word and work backward */
      wordP = blockIdx + blockStride * (blockDataWords - 1);
      for (int i = 0; i < blockDataWords; i++) {
        rec.append(code[wordP]);
        wordP -= blockStride;
      }

      /* Compute syndromes (syn) */
      boolean error = RsComputeSyndromes(syn, rec, blockErrorWords);

      /* Error(s) detected: Attempt repair */
      if (error) {
        /* Find error locator polynomial (elp) */
        boolean repairable = RsFindErrorLocatorPoly(elp, syn, blockErrorWords, blockMaxCorrectable);
        if (!repairable)
          return false;

        /* Find error positions (loc) */
        repairable = RsFindErrorLocations(loc, elp);
        if (!repairable)
          return false;

        /* Find error values and repair */
        RsRepairErrors(rec, loc, elp, syn);
      }

      /*
       * Overwrite output with correct/corrected values
       */

      /* Start with first data word and work forward */
      wordP = blockIdx;
      for (int i = 0; i < blockDataWords; i++) {
        code[wordP] = (byte) rec.pop();
        wordP += blockStride;
      }

      /* Start with first error word and work forward */
      wordP = symbolDataWords + blockIdx;
      for (int i = 0; i < blockErrorWords; i++) {
        code[wordP] = (byte) rec.pop();
        wordP += blockStride;
      }
    }

    return true;
  }

  /**
   * Populate generator polynomial. More detailed description. \param gen \param errorWordCount
   * \return Function success (DmtxPass|DmtxFail)
   */
  private static void RsGenPoly(ByteList gen, int errorWordCount) {
    /* Initialize all coefficients to 1 */
    gen.append((byte) 1, errorWordCount);

    /* Generate polynomial */
    for (int i = 0; i < gen.getLength(); i++)
      for (int j = i; j >= 0; j--) {
        gen.set(j, gfMultAntilog(gen.get(j), i + 1));
        if (j > 0)
          gen.set(j, gfAdd(gen.get(j), gen.get(j - 1)));
      }
  }

  /**
   * Populate generator polynomial. Assume we have received bits grouped into mm-bit symbols in
   * rec[i], i=0..(nn-1), and rec[i] is index form (ie as powers of alpha). We first compute the
   * 2*tt syndromes by substituting alpha**i into rec(X) and evaluating, storing the syndromes in
   * syn[i], i=1..2tt (leave syn[0] zero). \param syn \param rec \param blockErrorWords \return Are
   * error(s) present? (DmtxPass|DmtxFail)
   */
  /*
   * XXX this CHKPASS isn't doing what we want ... really need a error reporting strategy
   */
  private static boolean RsComputeSyndromes(ByteList syn, ByteList rec, int blockErrorWords) {
    int i, j;
    boolean error = false;

    /* Initialize all coefficients to 0 */
    syn.append((byte) 0, blockErrorWords + 1);

    for (i = 1; i < syn.getLength(); i++) {
      /* Calculate syndrome at i */
      for (j = 0; j < rec.getLength(); j++)
        /* alternatively: j < blockTotalWords */
        syn.set(i, gfAdd(syn.get(i), gfMultAntilog(rec.get(j), i * j)));

      /* Non-zero syndrome indicates presence of error(s) */
      if (syn.get(i) != 0)
        error = true;
    }

    return error;
  }

  /**
   * Find the error location polynomial using Berlekamp-Massey. More detailed description. \param
   * elpOut \param syn \param errorWordCount \param maxCorrectable \return Is block repairable?
   * (DmtxTrue|DmtxFalse)
   */
  private static boolean RsFindErrorLocatorPoly(ByteList elpOut, ByteList syn, int errorWordCount, int maxCorrectable) {
    int i, iNext, j;
    int m, mCmp, lambda;
    int disTmp;
    ByteList dis;
    final ByteList elp[] = new ByteList[MAX_ERROR_WORD_COUNT + 2];
    dis = new ByteList(MAX_ERROR_WORD_COUNT + 1);

    for (i = 0; i < MAX_ERROR_WORD_COUNT + 2; i++)
      elp[i] = new ByteList(MAX_ERROR_WORD_COUNT);

    /* iNext = 0 */
    elp[0].append(1);
    dis.append(1);

    /* iNext = 1 */
    elp[1].append(1);
    dis.append(syn.get(1));

    for (iNext = 2, i = 1; /* explicit break */; i = iNext++) {
      if (dis.get(i) == 0)
        /* Simple case: Copy directly from previous iteration */
        elp[iNext].copyFrom(elp[i]);
      else {
        /* Find earlier iteration (m) that provides maximal (m - lambda) */
        for (m = 0, mCmp = 1; mCmp < i; mCmp++)
          if (dis.get(mCmp) != 0 && mCmp - elp[mCmp].getLength() >= m - elp[m].getLength())
            m = mCmp;

        /* Calculate error location polynomial elp[i] (set 1st term) */
        for (lambda = elp[m].getLength() - 1, j = 0; j <= lambda; j++)
          elp[iNext].set(j + i - m, antilog301[(NN - log301[dis.get(m)] + log301[dis.get(i)] + log301[elp[m].get(j)])
              % NN]);

        /* Calculate error location polynomial elp[i] (add 2nd term) */
        for (lambda = elp[i].getLength() - 1, j = 0; j <= lambda; j++)
          elp[iNext].set(j, gfAdd(elp[iNext].get(j), elp[i].get(j)));

        elp[iNext].setLength(max(elp[i].getLength(), elp[m].getLength() + i - m));
      }

      lambda = elp[iNext].getLength() - 1;
      if (i == errorWordCount || i >= lambda + maxCorrectable)
        break;

      /* Calculate discrepancy dis.b[i] */
      for (disTmp = syn.get(iNext), j = 1; j <= lambda; j++)
        disTmp = gfAdd(disTmp, gfMult(syn.get(iNext - j), elp[iNext].get(j)));

      assert dis.getLength() == iNext;
      dis.append(disTmp);
    }

    elpOut.copyFrom(elp[iNext]);

    return lambda <= maxCorrectable;
  }

  /**
   * Find roots of the error locator polynomial (Chien Search). If the degree of elp is <= tt, we
   * substitute alpha**i, i=1..n into the elp to get the roots, hence the inverse roots, the error
   * location numbers. If the number of errors located does not equal the degree of the elp, we have
   * more than tt errors and cannot correct them. \param loc \param elp \return Is block repairable?
   * (DmtxTrue|DmtxFalse)
   */
  private static boolean RsFindErrorLocations(ByteList loc, ByteList elp) {
    int i, j;
    final int lambda = elp.getLength() - 1;
    int q;
    final ByteList reg = new ByteList(MAX_ERROR_WORD_COUNT);

    reg.copyFrom(elp);
    loc.append((byte) 0, 0);

    for (i = 1; i <= NN; i++) {
      for (q = 1, j = 1; j <= lambda; j++) {
        reg.set(j, gfMultAntilog(reg.get(j), j));
        q = gfAdd(q, reg.get(j));
      }

      if (q == 0)
        loc.append((byte) (NN - i));
    }

    return loc.getLength() == lambda;
  }

  /**
   * Find the error values and repair. Solve for the error value at the error location and correct
   * the error. The procedure is that found in Lin and Costello. For the cases where the number of
   * errors is known to be too large to correct, the information symbols as received are output (the
   * advantage of systematic encoding is that hopefully some of the information symbols will be okay
   * and that if we are in luck, the errors are in the parity part of the transmitted codeword).
   * \param rec \param loc \param elp \param syn
   */
  private static void RsRepairErrors(ByteList rec, ByteList loc, ByteList elp, ByteList syn) {
    final int lambda = elp.getLength() - 1;
    final ByteList z = new ByteList(MAX_ERROR_WORD_COUNT + 1);

    /* Form polynomial z(x) */
    z.append((byte) 1);
    for (int i = 1; i <= lambda; i++) {
      int zVal, j;
      for (zVal = gfAdd(syn.get(i), elp.get(i)), j = 1; j < i; j++)
        zVal = gfAdd(zVal, gfMult(elp.get(i - j), syn.get(j)));
      z.append((byte) zVal);
    }

    for (int i = 0; i < lambda; i++) {
      /* Calculate numerator of error term */
      int root = NN - loc.get(i);

      int err, j;
      for (err = 1, j = 1; j <= lambda; j++)
        err = gfAdd(err, gfMultAntilog(z.get(j), j * root));

      if (err == 0)
        continue;

      /* Calculate denominator of error term */
      int q;
      for (q = 0, j = 0; j < lambda; j++)
        if (j != i)
          q += log301[1 ^ antilog301[(loc.get(j) + root) % NN]];
      q %= NN;

      err = gfMultAntilog(err, NN - q);
      rec.set(loc.get(i), gfAdd(rec.get(loc.get(i)), err));
    }
  }

}
