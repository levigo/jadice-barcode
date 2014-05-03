package com.jadice.barcode.twod.dmtx;

public enum SymbolSize {
  RectAuto(10, 10, 8, 8, 1, 1, 8, 8, 1, 5, 2, 3, 5, 2), //
  SquareAuto(12, 12, 10, 10, 1, 1, 10, 10, 1, 7, 3, 5, 7, 3), //
  ShapeAuto(14, 14, 12, 12, 1, 1, 12, 12, 1, 10, 5, 8, 10, 5), //
  Fixed10x10(16, 16, 14, 14, 1, 1, 14, 14, 1, 12, 6, 12, 12, 6), //
  Fixed12x12(18, 18, 16, 16, 1, 1, 16, 16, 1, 14, 7, 18, 14, 7), //
  Fixed14x14(20, 20, 18, 18, 1, 1, 18, 18, 1, 18, 9, 22, 18, 9), //
  Fixed16x16(22, 22, 20, 20, 1, 1, 20, 20, 1, 20, 10, 30, 20, 10), //
  Fixed18x18(24, 24, 22, 22, 1, 1, 22, 22, 1, 24, 12, 36, 24, 12), //
  Fixed20x20(26, 26, 24, 24, 1, 1, 24, 24, 1, 28, 14, 44, 28, 14), //
  Fixed22x22(32, 32, 14, 14, 2, 2, 28, 28, 1, 36, 18, 62, 36, 18), //
  Fixed24x24(36, 36, 16, 16, 2, 2, 32, 32, 1, 42, 21, 86, 42, 21), //
  Fixed26x26(40, 40, 18, 18, 2, 2, 36, 36, 1, 48, 24, 114, 48, 24), //
  Fixed32x32(44, 44, 20, 20, 2, 2, 40, 40, 1, 56, 28, 144, 56, 28), //
  Fixed36x36(48, 48, 22, 22, 2, 2, 44, 44, 1, 68, 34, 174, 68, 34), //
  Fixed40x40(52, 52, 24, 24, 2, 2, 48, 48, 2, 42, 21, 204, 84, 42), //
  Fixed44x44(64, 64, 14, 14, 4, 4, 56, 56, 2, 56, 28, 280, 112, 56), //
  Fixed48x48(72, 72, 16, 16, 4, 4, 64, 64, 4, 36, 18, 368, 144, 72), //
  Fixed52x52(80, 80, 18, 18, 4, 4, 72, 72, 4, 48, 24, 456, 192, 96), //
  Fixed64x64(88, 88, 20, 20, 4, 4, 80, 80, 4, 56, 28, 576, 224, 112), //
  Fixed72x72(96, 96, 22, 22, 4, 4, 88, 88, 4, 68, 34, 696, 272, 136), //
  Fixed80x80(104, 104, 24, 24, 4, 4, 96, 96, 6, 56, 28, 816, 336, 168), //
  Fixed88x88(120, 120, 18, 18, 6, 6, 108, 108, 6, 68, 34, 1050, 408, 204), //
  Fixed96x96(132, 132, 20, 20, 6, 6, 120, 120, 8, 62, 31, 1304, 496, 248), //
  Fixed104x104(144, 144, 22, 22, 6, 6, 132, 132, 10, 62, 31, 1558, 620, 310), //
  Fixed120x120(8, 18, 6, 16, 1, 1, 6, 16, 1, 7, 3, 5, 7, 3), //
  Fixed132x132(8, 32, 6, 14, 2, 1, 6, 28, 1, 11, 5, 10, 11, 5), //
  Fixed144x144(12, 26, 10, 24, 1, 1, 10, 24, 1, 14, 7, 16, 14, 7), //
  Fixed8x18(12, 36, 10, 16, 2, 1, 10, 32, 1, 18, 9, 22, 18, 9), //
  Fixed8x32(16, 36, 14, 16, 2, 1, 14, 32, 1, 24, 12, 32, 24, 12), //
  Fixed12x26(16, 48, 14, 22, 2, 1, 14, 44, 1, 28, 14, 49, 28, 14), //
  Fixed12x36(-1, -1, -1, -1, -1, 1, -1, -1, -1, -1, -1, -1, -1, -1), //
  Fixed16x36(-1, -1, -1, -1, -1, 1, -1, -1, -1, -1, -1, -1, -1, -1), //
  Fixed16x48(-1, -1, -1, -1, -1, 1, -1, -1, -1, -1, -1, -1, -1, -1); //

  SymbolSize(int symbolRows, int symbolCols, int regionRows, int regionCols, int horizDataRegions, int vertDataRegions,
      int mappingMatrixRows, int mappingMatrixCols, int interleavedBlocks, int blockErrorWords,
      int blockMaxCorrectable, int symbolDataWords, int symbolErrorWords, int symbolMaxCorrectable) {
    this.rows = symbolRows;
    this.columns = symbolCols;
    this.regionRows = regionRows;
    this.regionCols = regionCols;
    this.horizDataRegions = horizDataRegions;
    this.vertDataRegions = vertDataRegions;
    this.mappingMatrixRows = mappingMatrixRows;
    this.mappingMatrixCols = mappingMatrixCols;
    this.interleavedBlocks = interleavedBlocks;
    this.blockErrorWords = blockErrorWords;
    this.blockMaxCorrectable = blockMaxCorrectable;
    this.symbolDataWords = symbolDataWords;
    this.symbolErrorWords = symbolErrorWords;
    this.symbolMaxCorrectable = symbolMaxCorrectable;
  }

  public final int rows;
  public final int columns;
  public final int regionRows;
  public final int regionCols;
  public final int horizDataRegions;
  public final int vertDataRegions;
  public final int mappingMatrixRows;
  public final int mappingMatrixCols;
  public final int interleavedBlocks;
  public final int blockErrorWords;
  public final int blockMaxCorrectable;
  public final int symbolDataWords;
  public final int symbolErrorWords;
  public final int symbolMaxCorrectable;
}
