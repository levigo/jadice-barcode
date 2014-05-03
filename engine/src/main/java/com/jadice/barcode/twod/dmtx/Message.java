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