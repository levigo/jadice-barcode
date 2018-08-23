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


/**
 * @struct DmtxMessage
 * @brief DmtxMessage
 */
public class Message {
  public int outputIdx; /* Internal index used to store output progress */
  int padCount;
  byte array[]; /* Pointer to internal representation of Data Matrix modules */

  /*
   * Pointer to internal storage of code words (data and error)
   */
  byte code[];

  /* Pointer to internal storage of decoded output */
  public byte output[];

  public enum Format {
    DmtxFormatMatrix, DmtxFormatMosaic
  }

  /**
   * \brief Allocate memory for message \param sizeIdx \param symbolFormat DmtxFormatMatrix |
   * DmtxFormatMosaic \return Address of allocated memory
   */
  Message(SymbolSize sizeIdx, Format symbolFormat) {
    final int mappingRows = sizeIdx.mappingMatrixRows;
    final int mappingCols = sizeIdx.mappingMatrixCols;

    int arraySize = mappingRows * mappingCols;

    array = new byte[arraySize];

    int codeSize = sizeIdx.symbolDataWords + sizeIdx.symbolErrorWords;

    if (symbolFormat == Format.DmtxFormatMosaic)
      codeSize *= 3;

    code = new byte[codeSize];

    /*
     * XXX not sure if this is the right place or even the right approach. Trying to allocate memory
     * for the decoded data stream and will initially assume that decoded data will not be larger
     * than 2x encoded data
     */
    int outputSize = codeSize * 10;
    output = new byte[outputSize];
  }

}