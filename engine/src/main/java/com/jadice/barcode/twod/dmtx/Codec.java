package com.jadice.barcode.twod.dmtx;


class Codec {
  private enum DmtxScheme {
    DmtxSchemeAutoFast,
    DmtxSchemeAutoBest,
    DmtxSchemeAscii,
    DmtxSchemeC40,
    DmtxSchemeText,
    DmtxSchemeX12,
    DmtxSchemeEdifact,
    DmtxSchemeBase256
  }

  private static class C40TextState {
    int shift;
    boolean upperShift;
  }

  private static final int DmtxValueC40Latch = 230;
  private static final int DmtxValueTextLatch = 239;
  private static final int DmtxValueX12Latch = 238;
  private static final int DmtxValueEdifactLatch = 240;
  private static final int DmtxValueBase256Latch = 231;
  private static final int DmtxValueCTXUnlatch = 254;
  private static final int DmtxValueEdifactUnlatch = 31;
  private static final int DmtxValueAsciiPad = 129;
  private static final int DmtxValueAsciiUpperShift = 235;
  @SuppressWarnings("unused")
  private static final int DmtxValueCTXShift1 = 0;
  @SuppressWarnings("unused")
  private static final int DmtxValueCTXShift2 = 1;
  @SuppressWarnings("unused")
  private static final int DmtxValueCTXShift3 = 2;
  @SuppressWarnings("unused")
  private static final int DmtxValueFNC1 = 232;
  @SuppressWarnings("unused")
  private static final int DmtxValueStructuredAppend = 233;
  private static final int DmtxValue05Macro = 236;
  private static final int DmtxValue06Macro = 237;
  @SuppressWarnings("unused")
  private static final int DmtxValueECI = 241;
  private static final int DmtxC40TextBasicSet = 0;
  private static final int DmtxC40TextShift1 = 1;
  private static final int DmtxC40TextShift2 = 2;
  private static final int DmtxC40TextShift3 = 3;

  private final Message msg;

  Codec(Message msg) {
    this.msg = msg;
  }

  /**
   * \brief Translate encoded data stream into final output \param msg \param sizeIdx \param
   * outputStart \return void
   */
  void DecodeDataStream(SymbolSize sizeIdx, byte outputStart[]) {
    boolean macro = false;
    DmtxScheme encScheme;
    int idx = 0, dataEnd;

    msg.output = outputStart == null ? msg.output : outputStart;
    msg.outputIdx = 0;

    // ptr = msg.code;
    dataEnd = idx + sizeIdx.symbolDataWords;

    /* Print macro header if first codeword triggers it */
    final int code = msg.code[idx] & 0xff;
    if (code == DmtxValue05Macro || code == DmtxValue06Macro) {
      PushOutputMacroHeader(code);
      macro = true;
    }

    while (idx < dataEnd) {
      encScheme = GetEncodationScheme(msg.code[idx] & 0xff);
      if (encScheme != DmtxScheme.DmtxSchemeAscii)
        idx++;

      switch (encScheme){
        case DmtxSchemeAscii :
          idx = DecodeSchemeAscii(idx, dataEnd);
          break;
        case DmtxSchemeC40 :
        case DmtxSchemeText :
          idx = DecodeSchemeC40Text(idx, dataEnd, encScheme);
          break;
        case DmtxSchemeX12 :
          idx = DecodeSchemeX12(idx, dataEnd);
          break;
        case DmtxSchemeEdifact :
          idx = DecodeSchemeEdifact(idx, dataEnd);
          break;
        case DmtxSchemeBase256 :
          idx = DecodeSchemeBase256(idx, dataEnd);
          break;
        default :
          /* error */
          break;
      }
    }

    /* Print macro trailer if required */
    if (macro)
      PushOutputMacroTrailer();
  }

  /**
   * \brief Determine next encodation scheme \param encScheme \param cw \return Pointer to next
   * undecoded codeword
   */
  private DmtxScheme GetEncodationScheme(int cw) {
    DmtxScheme encScheme;

    switch (cw){
      case DmtxValueC40Latch :
        encScheme = DmtxScheme.DmtxSchemeC40;
        break;
      case DmtxValueTextLatch :
        encScheme = DmtxScheme.DmtxSchemeText;
        break;
      case DmtxValueX12Latch :
        encScheme = DmtxScheme.DmtxSchemeX12;
        break;
      case DmtxValueEdifactLatch :
        encScheme = DmtxScheme.DmtxSchemeEdifact;
        break;
      case DmtxValueBase256Latch :
        encScheme = DmtxScheme.DmtxSchemeBase256;
        break;
      default :
        encScheme = DmtxScheme.DmtxSchemeAscii;
        break;
    }

    return encScheme;
  }

  /**
   *
   *
   */
  private void PushOutputWord(int value) {
    assert value >= 0 && value < 256;

    msg.output[msg.outputIdx++] = (byte) value;
  }

  /**
   *
   *
   */
  private void PushOutputC40TextWord(C40TextState state, int value) {
    assert value >= 0 && value < 256;

    msg.output[msg.outputIdx] = (byte) value;

    if (state.upperShift) {
      assert value < 128;
      msg.output[msg.outputIdx] += 128;
    }

    msg.outputIdx++;

    state.shift = DmtxC40TextBasicSet;
    state.upperShift = false;
  }

  /**
   *
   *
   */
  private void PushOutputMacroHeader(int macroType) {
    PushOutputWord('[');
    PushOutputWord(')');
    PushOutputWord('>');
    PushOutputWord(30); /* ASCII RS */
    PushOutputWord('0');

    assert macroType == DmtxValue05Macro || macroType == DmtxValue06Macro;
    if (macroType == DmtxValue05Macro)
      PushOutputWord('5');
    else
      PushOutputWord('6');

    PushOutputWord(29); /* ASCII GS */
  }

  /**
   *
   *
   */
  private void PushOutputMacroTrailer() {
    PushOutputWord(30); /* ASCII RS */
    PushOutputWord(4); /* ASCII EOT */
  }

  /**
   * \brief Decode stream assuming standard ASCII encodation \param msg \param ptr \param dataEnd
   * \return Pointer to next undecoded codeword
   */
  private int DecodeSchemeAscii(int idx, int dataEnd) {
    boolean upperShift;
    int codeword, digits;

    upperShift = false;

    while (idx < dataEnd) {
      codeword = msg.code[idx] & 0xff;

      if (GetEncodationScheme(codeword) != DmtxScheme.DmtxSchemeAscii)
        return idx;
      else
        idx++;

      if (upperShift) {
        PushOutputWord(codeword + 127);
        upperShift = false;
      } else if (codeword == DmtxValueAsciiUpperShift)
        upperShift = true;
      else if (codeword == DmtxValueAsciiPad) {
        assert dataEnd >= idx;
        assert dataEnd - idx <= Integer.MAX_VALUE;
        msg.padCount = dataEnd - idx;
        return dataEnd;
      } else if (codeword <= 128)
        PushOutputWord(codeword - 1);
      else if (codeword <= 229) {
        digits = codeword - 130;
        PushOutputWord(digits / 10 + '0');
        PushOutputWord(digits - digits / 10 * 10 + '0');
      }
    }

    return idx;
  }

  /**
   * \brief Decode stream assuming C40 or Text encodation \param msg \param ptr \param dataEnd
   * \param encScheme \return Pointer to next undecoded codeword
   */
  private int DecodeSchemeC40Text(int ptr, int dataEnd, DmtxScheme encScheme) {
    int i;
    int packed;
    final int c40Values[] = new int[3];
    final C40TextState state = new C40TextState();

    state.shift = DmtxC40TextBasicSet;
    state.upperShift = false;

    assert encScheme == DmtxScheme.DmtxSchemeC40 || encScheme == DmtxScheme.DmtxSchemeText;

    /* Unlatch is implied if only one codeword remains */
    if (dataEnd - ptr < 2)
      return ptr;

    while (ptr < dataEnd) {
      /* FIXME Also check that ptr+1 is safe to access */
      packed = (msg.code[ptr] & 0xff) << 8 | msg.code[ptr + 1] & 0xff;
      c40Values[0] = (packed - 1) / 1600;
      c40Values[1] = (packed - 1) / 40 % 40;
      c40Values[2] = (packed - 1) % 40;
      ptr += 2;

      for (i = 0; i < 3; i++)
        if (state.shift == DmtxC40TextBasicSet) { /* Basic set */
          if (c40Values[i] <= 2)
            state.shift = c40Values[i] + 1;
          else if (c40Values[i] == 3)
            PushOutputC40TextWord(state, ' ');
          else if (c40Values[i] <= 13)
            PushOutputC40TextWord(state, c40Values[i] - 13 + '9'); /* 0-9 */
          else if (c40Values[i] <= 39)
            if (encScheme == DmtxScheme.DmtxSchemeC40)
              PushOutputC40TextWord(state, c40Values[i] - 39 + 'Z'); /* A-Z */
            else if (encScheme == DmtxScheme.DmtxSchemeText)
              PushOutputC40TextWord(state, c40Values[i] - 39 + 'z'); /* a-z */
        } else if (state.shift == DmtxC40TextShift1)
          PushOutputC40TextWord(state, c40Values[i]); /*
                                                       * ASCII 0 - 31
                                                       */
        else if (state.shift == DmtxC40TextShift2) { /* Shift 2 set */
          if (c40Values[i] <= 14)
            PushOutputC40TextWord(state, c40Values[i] + 33); /*
                                                              * ASCII 33 - 47
                                                              */
          else if (c40Values[i] <= 21)
            PushOutputC40TextWord(state, c40Values[i] + 43); /*
                                                              * ASCII 58 - 64
                                                              */
          else if (c40Values[i] <= 26)
            PushOutputC40TextWord(state, c40Values[i] + 69); /*
                                                              * ASCII 91 - 95
                                                              */
          else if (c40Values[i] == 27)
            PushOutputC40TextWord(state, 0x1d); /*
                                                 * FNC1 -- XXX depends on position?
                                                 */
          else if (c40Values[i] == 30) {
            state.upperShift = true;
            state.shift = DmtxC40TextBasicSet;
          }
        } else if (state.shift == DmtxC40TextShift3)
          if (encScheme == DmtxScheme.DmtxSchemeC40)
            PushOutputC40TextWord(state, c40Values[i] + 96);
          else if (encScheme == DmtxScheme.DmtxSchemeText)
            if (c40Values[i] == 0)
              PushOutputC40TextWord(state, c40Values[i] + 96);
            else if (c40Values[i] <= 26)
              PushOutputC40TextWord(state, c40Values[i] - 26 + 'Z'); /* A-Z */
            else
              PushOutputC40TextWord(state, c40Values[i] - 31 + 127); /* { | } ~ DEL */

      /*
       * Unlatch if codeword 254 follows 2 codewords in C40/Text encodation
       */
      if ((msg.code[ptr] & 0xff) == DmtxValueCTXUnlatch)
        return ptr + 1;

      /* Unlatch is implied if only one codeword remains */
      if (dataEnd - ptr < 2)
        return ptr;
    }

    return ptr;
  }

  /**
   * \brief Decode stream assuming X12 encodation \param msg \param ptr \param dataEnd \return
   * Pointer to next undecoded codeword
   */
  private int DecodeSchemeX12(int ptr, int dataEnd) {
    int i;
    int packed;
    final int x12Values[] = new int[3];

    /* Unlatch is implied if only one codeword remains */
    if (dataEnd - ptr < 2)
      return ptr;

    while (ptr < dataEnd) {

      /* FIXME Also check that ptr+1 is safe to access */
      packed = (msg.code[ptr] & 0xff) << 8 | msg.code[ptr + 1] & 0xff;
      x12Values[0] = (packed - 1) / 1600;
      x12Values[1] = (packed - 1) / 40 % 40;
      x12Values[2] = (packed - 1) % 40;
      ptr += 2;

      for (i = 0; i < 3; i++)
        if (x12Values[i] == 0)
          PushOutputWord(13);
        else if (x12Values[i] == 1)
          PushOutputWord(42);
        else if (x12Values[i] == 2)
          PushOutputWord(62);
        else if (x12Values[i] == 3)
          PushOutputWord(32);
        else if (x12Values[i] <= 13)
          PushOutputWord(x12Values[i] + 44);
        else if (x12Values[i] <= 90)
          PushOutputWord(x12Values[i] + 51);

      /*
       * Unlatch if codeword 254 follows 2 codewords in C40/Text encodation
       */
      if ((msg.code[ptr] & 0xff) == DmtxValueCTXUnlatch)
        return ptr + 1;

      /* Unlatch is implied if only one codeword remains */
      if (dataEnd - ptr < 2)
        return ptr;
    }

    return ptr;
  }

  /**
   * \brief Decode stream assuming EDIFACT encodation \param msg \param ptr \param dataEnd \return
   * Pointer to next undecoded codeword
   */
  private int DecodeSchemeEdifact(int ptr, int dataEnd) {
    int i;
    final int unpacked[] = new int[4];

    /* Unlatch is implied if fewer than 3 codewords remain */
    if (dataEnd - ptr < 3)
      return ptr;

    while (ptr < dataEnd) {

      /*
       * FIXME Also check that ptr+2 is safe to access -- shouldn't be a problem because I'm
       * guessing you can guarantee there will always be at least 3 error codewords
       */
      unpacked[0] = (msg.code[ptr] & 0xfc) >> 2;
      unpacked[1] = (msg.code[ptr] & 0x03) << 4 | (msg.code[ptr + 1] & 0xf0) >> 4;
      unpacked[2] = (msg.code[ptr + 1] & 0x0f) << 2 | (msg.code[ptr + 2] & 0xc0) >> 6;
      unpacked[3] = msg.code[ptr + 2] & 0x3f;

      for (i = 0; i < 4; i++) {

        /*
         * Advance input ptr (4th value comes from already-read 3rd byte)
         */
        if (i < 3)
          ptr++;

        /* Test for unlatch condition */
        if (unpacked[i] == DmtxValueEdifactUnlatch) {
          assert msg.output[msg.outputIdx] == 0; /* XXX dirty why? */
          return ptr;
        }

        PushOutputWord(unpacked[i] ^ (unpacked[i] & 0x20 ^ 0x20) << 1);
      }

      /* Unlatch is implied if fewer than 3 codewords remain */
      if (dataEnd - ptr < 3)
        return ptr;
    }

    return ptr;

    /*
     * XXX the following version should be safer, but requires testing before replacing the old
     * version int bits = 0; int bitCount = 0; int value;
     * 
     * while(ptr < dataEnd) {
     * 
     * if(bitCount < 6) { bits = (bits << 8) | *(ptr++); bitCount += 8; }
     * 
     * value = bits >> (bitCount - 6); bits -= (value << (bitCount - 6)); bitCount -= 6;
     * 
     * if(value == 0x1f) { assert(bits == 0); // should be padded with zero-value bits return ptr; }
     * PushOutputWord(msg, value ^ (((value & 0x20) ^ 0x20) << 1));
     * 
     * // Unlatch implied if just completed triplet and 1 or 2 words are left if(bitCount == 0 &&
     * dataEnd - ptr - 1 > 0 && dataEnd - ptr - 1 < 3) return ptr; }
     * 
     * assert(bits == 0); // should be padded with zero-value bits assert(bitCount == 0); // should
     * be padded with zero-value bits return ptr;
     */
  }

  /**
   * \brief Decode stream assuming Base 256 encodation \param msg \param ptr \param dataEnd \return
   * Pointer to next undecoded codeword
   */
  private int DecodeSchemeBase256(int ptr, int dataEnd) {
    int d0, d1;
    int idx;
    int ptrEnd;

    /* Find positional index used for unrandomizing */
    assert ptr + 1 >= 0;
    assert ptr + 1 <= Integer.MAX_VALUE;
    idx = ptr + 1;

    d0 = UnRandomize255State(msg.code[ptr++] & 0xff, idx++);
    if (d0 == 0)
      ptrEnd = dataEnd;
    else if (d0 <= 249)
      ptrEnd = ptr + d0;
    else {
      d1 = UnRandomize255State(msg.code[ptr++] & 0xff, idx++);
      ptrEnd = ptr + (d0 - 249) * 250 + d1;
    }

    if (ptrEnd > dataEnd)
      throw new IllegalStateException(); /*
                                          * XXX needs cleaner error handling
                                          */

    while (ptr < ptrEnd)
      PushOutputWord(UnRandomize255State(msg.code[ptr++] & 0xff, idx++));

    return ptr;
  }

  /****************** BASE255 */
  /**
   * \brief Unrandomize 255 state \param value \param idx \return Unrandomized value
   */
  private int UnRandomize255State(int value, int idx) {
    int pseudoRandom;
    int tmp;

    pseudoRandom = 149 * idx % 255 + 1;
    tmp = value - pseudoRandom;
    if (tmp < 0)
      tmp += 256;

    assert tmp >= 0 && tmp < 256;

    return tmp;
  }

}
