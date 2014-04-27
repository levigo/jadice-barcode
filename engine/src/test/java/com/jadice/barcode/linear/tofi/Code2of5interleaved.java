package com.jadice.barcode.linear.tofi;

import java.util.Arrays;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.jadice.barcode.AbstractDecodeTest;
import com.jadice.barcode.Configurer;
import com.jadice.barcode.Options;
import com.jadice.barcode.Symbology;
import com.jadice.barcode.linear.LinearCodeSettings;
import com.jadice.barcode.linear.LinearCodeSettings.Direction;
import com.jadice.barcode.linear.tofi.TwoOfFiveInterleaved;

@RunWith(Parameterized.class)
public class Code2of5interleaved extends AbstractDecodeTest {
  //@formatter:off
  private static Object PARAMS[][] = { //
    { "118_1818.jpg", "200168", new Configurer() {
      @Override
      public void configure(Options o) {
        o.getOptions(LinearCodeSettings.class).setQuietZoneTolerance(30);
      }
    }}, //
    { "118_1819.jpg", "200168", new Configurer() {
      @Override
      public void configure(Options o) {
        o.getOptions(LinearCodeSettings.class).setQuietZoneTolerance(30);
      }
    }}, //
    { "2iof5.png", "1022049452", null }, //
    { "baglab.png", "1022049452", null }, //
    { "barcode-east.png", "04657467", null }, //
    { "barcode-north.png", "04657467", null }, //
    { "barcode-south.png", "04657467", null }, //
    { "barcode-west.png", "04657467", null }, //
    { "IMG_0577.jpg", "HI345678", null }, //
    { "IMG_0578.jpg", "HI345678", null }, //
    { "nur2of5.png", "1022049452", null }, //
  };
  // @formatter:on

  @Parameters
  public static List<Object[]> parameters() {
    return Arrays.asList(PARAMS);
  }

  public Code2of5interleaved(String imageName, String expectedResult, Configurer c) {
    super(imageName, expectedResult, c);
  }

  @Override
  protected void initOptions(Options options) {
    options.getOptions(LinearCodeSettings.class).setScanInterval(20);
    options.getOptions(LinearCodeSettings.class).setDirectionEnabled(Direction.NORTH, true);
    options.getOptions(LinearCodeSettings.class).setDirectionEnabled(Direction.SOUTH, true);
    options.getOptions(LinearCodeSettings.class).setDirectionEnabled(Direction.WEST, true);

    super.initOptions(options);
  }

  @Override
  protected Symbology createSymbology() {
    return new TwoOfFiveInterleaved();
  }
}
