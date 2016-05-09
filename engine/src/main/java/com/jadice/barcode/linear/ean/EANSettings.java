package com.jadice.barcode.linear.ean;

import com.jadice.barcode.Settings;
import com.jadice.barcode.checksum.Checksum;
import com.jadice.barcode.checksum.ChecksumVerifier;

public class EANSettings implements Settings, Checksum {

  /**
   * Wether checksum verification is active.
   */
  private boolean enableChecksumVerification = true;

  /**
   * The checksumverifier for EAN
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
      checksumVerifier = new EANChecksumVerifier();
    }
    return checksumVerifier;
  }

  @Override
  public void setChecksumVerifier(ChecksumVerifier checksumVerifier) {
    this.checksumVerifier = checksumVerifier;
  }
}
