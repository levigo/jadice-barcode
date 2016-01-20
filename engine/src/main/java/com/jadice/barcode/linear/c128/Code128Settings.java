package com.jadice.barcode.linear.c128;

import com.jadice.barcode.Settings;
import com.jadice.barcode.checksum.Checksum;
import com.jadice.barcode.checksum.ChecksumVerifier;

public class Code128Settings implements Settings, Checksum {

  /**
   * Wether checksum verification is active.
   */
  private boolean enableChecksumVerification = true;

  /**
   * The checksumverifier for Code128.
   */
  private ChecksumVerifier checksumVerifier = null;

  @Override
  public boolean isEnableChecksumVerification() {
    return enableChecksumVerification;
  }

  @Override
  public void setEnableChecksumVerification(boolean enableChecksumVerification) {
    this.enableChecksumVerification = enableChecksumVerification;
  }

  @Override
  public ChecksumVerifier getChecksumVerifier() {
    if (checksumVerifier == null) {
      checksumVerifier = new Code128ChecksumVerifier();
    }
    return checksumVerifier;
  }

  @Override
  public void setChecksumVerifier(ChecksumVerifier checksumVerifier) {
    this.checksumVerifier = checksumVerifier;
  }
}
