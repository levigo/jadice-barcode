package com.jadice.barcode.linear.c128;

import com.jadice.barcode.AbstractDecoder.CodeString;
import com.jadice.barcode.checksum.ChecksumVerifier;

public class Code128ChecksumVerifier implements ChecksumVerifier {

  @Override
  public boolean verifyChecksum(CodeString result) {
    int codes[] = result.getCodes();

    int position = 1;
    int checkSum = codes[0];
    while (position < codes.length - 1) {
      // weight * value
      checkSum += position * codes[position];
      position++;
    }

    final int csSymbol = codes[position - 1];
    checkSum -= csSymbol * (position - 1);

    if (checkSum % 103 == csSymbol) {
      result.setChecksumVerificationOK(true);
      return true;
    }

    return false;
  }

}
