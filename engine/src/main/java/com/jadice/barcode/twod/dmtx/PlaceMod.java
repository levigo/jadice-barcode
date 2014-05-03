package com.jadice.barcode.twod.dmtx;


public class PlaceMod {
  private static final int DmtxMaskBit8 = 0x01 << 0;
  private static final int DmtxMaskBit7 = 0x01 << 1;
  private static final int DmtxMaskBit6 = 0x01 << 2;
  private static final int DmtxMaskBit5 = 0x01 << 3;
  private static final int DmtxMaskBit4 = 0x01 << 4;
  private static final int DmtxMaskBit3 = 0x01 << 5;
  private static final int DmtxMaskBit2 = 0x01 << 6;
  private static final int DmtxMaskBit1 = 0x01 << 7;

  public static class ByteP {
    byte b[];
    int offset;

    public ByteP(byte[] b, int offset) {
      this.b = b;
      this.offset = offset;
    }

    public byte get() {
      return b[offset];
    }

    public void set(byte val) {
      b[offset] = val;
    }

    public void set(int val) {
      b[offset] = (byte) val;
    }
  }

  /**
   * receives symbol row and col and returns status DmtxModuleOn / !DmtxModuleOn (DmtxModuleOff)
   * DmtxModuleAssigned DmtxModuleVisited DmtxModuleData / !DmtxModuleData (DmtxModuleAlignment) row
   * and col are expressed in symbol coordinates, so (0,0) is the intersection of the "L"
   */
  @SuppressWarnings("unused")
  private int dmtxSymbolModuleStatus(Message message, SymbolSize sizeIdx, int symbolRow, int symbolCol) {
    int symbolRowReverse;
    int mappingRow, mappingCol;
    int dataRegionRows, dataRegionCols;
    int symbolRows, mappingCols;

    dataRegionRows = sizeIdx.regionRows;
    dataRegionCols = sizeIdx.regionCols;
    symbolRows = sizeIdx.rows;
    mappingCols = sizeIdx.mappingMatrixCols;

    symbolRowReverse = symbolRows - symbolRow - 1;
    mappingRow = symbolRowReverse - 1 - 2 * (symbolRowReverse / (dataRegionRows + 2));
    mappingCol = symbolCol - 1 - 2 * (symbolCol / (dataRegionCols + 2));

    /* Solid portion of alignment patterns */
    if (symbolRow % (dataRegionRows + 2) == 0 || symbolCol % (dataRegionCols + 2) == 0)
      // return (DmtxModuleOnRGB | (!DmtxModuleData));
      return Region.DmtxModuleOnRGB;

    /* Horinzontal calibration bars */
    if ((symbolRow + 1) % (dataRegionRows + 2) == 0)
      // return (((symbolCol & 0x01) ? 0 : DmtxModuleOnRGB) |
      // (!DmtxModuleData));
      return (symbolCol & 0x01) != 0 ? 0 : Region.DmtxModuleOnRGB;

    /* Vertical calibration bars */
    if ((symbolCol + 1) % (dataRegionCols + 2) == 0)
      // return (((symbolRow & 0x01) ? 0 : DmtxModuleOnRGB) |
      // (!DmtxModuleData));
      return (symbolRow & 0x01) != 0 ? 0 : Region.DmtxModuleOnRGB;

    /* Data modules */
    // return (message->array[mappingRow * mappingCols + mappingCol] |
    // DmtxModuleData);
    return message.array[mappingRow * mappingCols + mappingCol];
  }

  /**
   * \brief Logical relationship between bit and module locations \param modules \param codewords
   * \param sizeIdx \param moduleOnColor \return Number of codewords read
   */
  static int modulePlacementEcc200(byte modules[], byte codewords[], SymbolSize sizeIdx, int moduleOnColor) {
    int row, col, chr;
    int mappingRows, mappingCols;

    assert ((moduleOnColor & (Region.DmtxModuleOnRed | Region.DmtxModuleOnGreen | Region.DmtxModuleOnBlue)) != 0);

    mappingRows = sizeIdx.mappingMatrixRows;
    mappingCols = sizeIdx.mappingMatrixCols;

    /* Start in the nominal location for the 8th bit of the first character */
    chr = 0;
    row = 4;
    col = 0;

    do {
      /* Repeatedly first check for one of the special corner cases */
      if ((row == mappingRows) && (col == 0))
        patternShapeSpecial1(modules, mappingRows, mappingCols, new ByteP(codewords, chr++), moduleOnColor);
      else if ((row == mappingRows - 2) && (col == 0) && (mappingCols % 4 != 0))
        patternShapeSpecial2(modules, mappingRows, mappingCols, new ByteP(codewords, chr++), moduleOnColor);
      else if ((row == mappingRows - 2) && (col == 0) && (mappingCols % 8 == 4))
        patternShapeSpecial3(modules, mappingRows, mappingCols, new ByteP(codewords, chr++), moduleOnColor);
      else if ((row == mappingRows + 4) && (col == 2) && (mappingCols % 8 == 0))
        patternShapeSpecial4(modules, mappingRows, mappingCols, new ByteP(codewords, chr++), moduleOnColor);

      /* Sweep upward diagonally, inserting successive characters */
      do {
        if ((row < mappingRows) && (col >= 0) && (modules[row * mappingCols + col] & Region.DmtxModuleVisited) == 0)
          patternShapeStandard(modules, mappingRows, mappingCols, row, col, new ByteP(codewords, chr++), moduleOnColor);
        row -= 2;
        col += 2;
      } while ((row >= 0) && (col < mappingCols));
      row += 1;
      col += 3;

      /* Sweep downward diagonally, inserting successive characters */
      do {
        if ((row >= 0) && (col < mappingCols) && (modules[row * mappingCols + col] & Region.DmtxModuleVisited) == 0)
          patternShapeStandard(modules, mappingRows, mappingCols, row, col, new ByteP(codewords, chr++), moduleOnColor);
        row += 2;
        col -= 2;
      } while ((row < mappingRows) && (col >= 0));
      row += 3;
      col += 1;
      /* ... until the entire modules array is scanned */
    } while ((row < mappingRows) || (col < mappingCols));

    /* If lower righthand corner is untouched then fill in the fixed pattern */
    if ((modules[mappingRows * mappingCols - 1] & Region.DmtxModuleVisited) == 0) {

      modules[mappingRows * mappingCols - 1] |= moduleOnColor;
      modules[(mappingRows * mappingCols) - mappingCols - 2] |= moduleOnColor;
    } /* XXX should this fixed pattern also be used in reading somehow? */

    /* XXX compare that chr == region->dataSize here */
    return chr; /* XXX number of codewords read off */
  }

  /**
   * \brief XXX \param modules \param mappingRows \param mappingCols \param row \param col \param
   * codeword \param moduleOnColor \return void
   */
  private static void patternShapeStandard(byte modules[], int mappingRows, int mappingCols, int row, int col,
      ByteP codeword, int moduleOnColor) {
    placeModule(modules, mappingRows, mappingCols, row - 2, col - 2, codeword, DmtxMaskBit1, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, row - 2, col - 1, codeword, DmtxMaskBit2, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, row - 1, col - 2, codeword, DmtxMaskBit3, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, row - 1, col - 1, codeword, DmtxMaskBit4, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, row - 1, col, codeword, DmtxMaskBit5, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, row, col - 2, codeword, DmtxMaskBit6, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, row, col - 1, codeword, DmtxMaskBit7, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, row, col, codeword, DmtxMaskBit8, moduleOnColor);
  }

  /**
   * \brief XXX \param modules \param mappingRows \param mappingCols \param codeword \param
   * moduleOnColor \return void
   */
  private static void patternShapeSpecial1(byte modules[], int mappingRows, int mappingCols, ByteP codeword,
      int moduleOnColor) {
    placeModule(modules, mappingRows, mappingCols, mappingRows - 1, 0, codeword, DmtxMaskBit1, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, mappingRows - 1, 1, codeword, DmtxMaskBit2, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, mappingRows - 1, 2, codeword, DmtxMaskBit3, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 0, mappingCols - 2, codeword, DmtxMaskBit4, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 0, mappingCols - 1, codeword, DmtxMaskBit5, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 1, mappingCols - 1, codeword, DmtxMaskBit6, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 2, mappingCols - 1, codeword, DmtxMaskBit7, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 3, mappingCols - 1, codeword, DmtxMaskBit8, moduleOnColor);
  }

  /**
   * \brief XXX \param modules \param mappingRows \param mappingCols \param codeword \param
   * moduleOnColor \return void
   */
  private static void patternShapeSpecial2(byte modules[], int mappingRows, int mappingCols, ByteP codeword,
      int moduleOnColor) {
    placeModule(modules, mappingRows, mappingCols, mappingRows - 3, 0, codeword, DmtxMaskBit1, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, mappingRows - 2, 0, codeword, DmtxMaskBit2, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, mappingRows - 1, 0, codeword, DmtxMaskBit3, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 0, mappingCols - 4, codeword, DmtxMaskBit4, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 0, mappingCols - 3, codeword, DmtxMaskBit5, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 0, mappingCols - 2, codeword, DmtxMaskBit6, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 0, mappingCols - 1, codeword, DmtxMaskBit7, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 1, mappingCols - 1, codeword, DmtxMaskBit8, moduleOnColor);
  }

  /**
   * \brief XXX \param modules \param mappingRows \param mappingCols \param codeword \param
   * moduleOnColor \return void
   */
  private static void patternShapeSpecial3(byte modules[], int mappingRows, int mappingCols, ByteP codeword,
      int moduleOnColor) {
    placeModule(modules, mappingRows, mappingCols, mappingRows - 3, 0, codeword, DmtxMaskBit1, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, mappingRows - 2, 0, codeword, DmtxMaskBit2, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, mappingRows - 1, 0, codeword, DmtxMaskBit3, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 0, mappingCols - 2, codeword, DmtxMaskBit4, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 0, mappingCols - 1, codeword, DmtxMaskBit5, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 1, mappingCols - 1, codeword, DmtxMaskBit6, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 2, mappingCols - 1, codeword, DmtxMaskBit7, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 3, mappingCols - 1, codeword, DmtxMaskBit8, moduleOnColor);
  }

  /**
   * \brief XXX \param modules \param mappingRows \param mappingCols \param codeword \param
   * moduleOnColor \return void
   */
  private static void patternShapeSpecial4(byte modules[], int mappingRows, int mappingCols, ByteP codeword,
      int moduleOnColor) {
    placeModule(modules, mappingRows, mappingCols, mappingRows - 1, 0, codeword, DmtxMaskBit1, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, mappingRows - 1, mappingCols - 1, codeword, DmtxMaskBit2,
        moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 0, mappingCols - 3, codeword, DmtxMaskBit3, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 0, mappingCols - 2, codeword, DmtxMaskBit4, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 0, mappingCols - 1, codeword, DmtxMaskBit5, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 1, mappingCols - 3, codeword, DmtxMaskBit6, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 1, mappingCols - 2, codeword, DmtxMaskBit7, moduleOnColor);
    placeModule(modules, mappingRows, mappingCols, 1, mappingCols - 1, codeword, DmtxMaskBit8, moduleOnColor);
  }

  /**
   * \brief XXX \param modules \param mappingRows \param mappingCols \param row \param col \param
   * codeword \param mask \param moduleOnColor \return void
   */
  private static void placeModule(byte modules[], int mappingRows, int mappingCols, int row, int col, ByteP codeword,
      int mask, int moduleOnColor) {
    if (row < 0) {
      row += mappingRows;
      col += 4 - ((mappingRows + 4) % 8);
    }
    if (col < 0) {
      col += mappingCols;
      row += 4 - ((mappingCols + 4) % 8);
    }

    /*
     * If module has already been assigned then we are decoding the pattern into codewords
     */
    if ((modules[row * mappingCols + col] & Region.DmtxModuleAssigned) != 0) {
      if ((modules[row * mappingCols + col] & moduleOnColor) != 0)
        codeword.set(codeword.get() | mask);
      else
        codeword.set(codeword.get() & (0xff ^ mask));
    }
    /* Otherwise we are encoding the codewords into a pattern */
    else {
      if ((codeword.get() & mask) != 0x00)
        modules[row * mappingCols + col] |= moduleOnColor;

      modules[row * mappingCols + col] |= Region.DmtxModuleAssigned;
    }

    modules[row * mappingCols + col] |= Region.DmtxModuleVisited;
  }
}
