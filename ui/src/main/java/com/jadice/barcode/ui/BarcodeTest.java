/**
 * jadice barcode engine - a Java-based barcode decoding engine
 * 
 * Copyright (C) 1995-${year} levigo holding gmbh. All Rights Reserved.
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
 * Contact: solutions@levigo.de
 */
package com.jadice.barcode.ui;

/*
 * Copyright (c) 2003-2004 levigo holding gmbh. All Rights Reserved.
 * 
 * This software is the proprietary information of levigo holding gmbh Use is subject to license
 * terms.
 */
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.filechooser.FileFilter;

import com.jadice.barcode.BaseSettings;
import com.jadice.barcode.Detector;
import com.jadice.barcode.DiagnosticSettings;
import com.jadice.barcode.DiagnosticSettings.TransientMarkerListener;
import com.jadice.barcode.Options;
import com.jadice.barcode.Result;
import com.jadice.barcode.grid.BinaryGrid;
import com.jadice.barcode.j2d.BufferedImageLuminanceSource;
import com.jadice.barcode.j2d.Java2DUtils;

/**
 * A simple GUI that allows barcode testing. In contrast to <code>ViewerBarcodeDemo</code> this GUI
 * is independent of the jadice viewer.
 * 
 */
public class BarcodeTest extends JFrame {
  public static void main(String[] args) throws FileNotFoundException, IOException {
    System.setProperty("com.levigo.internal.omit.watermark", "true");
    new BarcodeTest("../engine/src/test/resources");
  }

  private static final long serialVersionUID = 1L;

  File baseDir = new File(".");
  private final JScrollPane imagePane;
  BufferedImage image;

  ResultPane resultPane;

  Options options = new Options();

  DebugAnnotationSelector debugAnnotationSelector;

  private DefaultComboBoxModel imageSelection;

  int zoomFactor = 100;

  private JCheckBox binarizedImageBox;

  private SwingWorker<List<Result>, Object> decodeWorker;

  private JTextField zoomField;

  private final JLabel coordinateLabel;

  /**
   * This layer is painted on top of the image and marks detected areas.
   */
  class ResultPane extends JComponent implements TransientMarkerListener {
    private static final long serialVersionUID = 1L;

    private final BufferedImage image;
    private BufferedImage binaryImage;
    private List<Result> results;

    private DiagnosticSettings diagSettings;

    ResultPane(BufferedImage image) {
      this.image = image;
      setOpaque(false);
      updateZoom();
    }

    /*
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent(Graphics g) {
      final Graphics2D g2 = (Graphics2D) g;

      g2.scale(zoomFactor / 100d, zoomFactor / 100d);

      g2.drawImage(image, null, 0, 0);

      if (null != binaryImage)
        g2.drawImage(binaryImage, null, 0, 0);

      try {
        if (null != diagSettings) {
          Java2DUtils.paintDebugMarkers(g2, diagSettings.getDebugMarkers(),
              debugAnnotationSelector.getEnabledDebugMarkers());
          Java2DUtils.paintDebugMarkers(g2, diagSettings.getTransientMarkers(),
              debugAnnotationSelector.getEnabledDebugMarkers());
        }
        if (null != results) {
          Java2DUtils.paintCodeMarkers(g2, results);
        }
      } catch (ConcurrentModificationException e) {
        // just re-try
        repaint();
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.levigo.barcode.DetectionResultAcceptor#setResults(java.awt.image. BufferedImage,
     * com.levigo.barcode.DetectionResult[])
     */
    public void setResults(List<Result> list) {
      this.results = list;
      repaint();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.levigo.barcode.DetectionResultAcceptor#setResults(java.awt.image. BufferedImage,
     * com.levigo.barcode.DetectionResult[])
     */
    public void setDiagSettings(DiagnosticSettings diagSettings) {
      this.diagSettings = diagSettings;
      diagSettings.addTransientMarkerListener(this);
      repaint();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.levigo.barcode.DetectionResultAcceptor#setImageResolution(int)
     */
    public void setImageResolution(int imageResolution) {
      // not used here.
    }

    /**
     * @param i
     */
    public void updateZoom() {
      setPreferredSize(new Dimension(image.getWidth() * zoomFactor / 100, image.getHeight() * zoomFactor / 100));
      revalidate();
      repaint();
    }

    public BufferedImage getBinaryImage() {
      return binaryImage;
    }

    public void setBinaryImage(BufferedImage binaryImage) {
      this.binaryImage = binaryImage;
    }

    @Override
    public void markersChanged() {
      repaint();
    }
  }

  private BarcodeTest(String name) {
    setDefaultCloseOperation(EXIT_ON_CLOSE);

    baseDir = new File(name);

    // where to paint the image
    imagePane = new JScrollPane();
    imagePane.setPreferredSize(new Dimension(500, 500));
    getContentPane().add(imagePane, BorderLayout.CENTER);

    // file selector
    getContentPane().add(createFileSelector(), BorderLayout.NORTH);

    getContentPane().add(new JScrollPane(createOptionPane(), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.EAST);

    coordinateLabel = new JLabel("-");
    getContentPane().add(coordinateLabel, BorderLayout.SOUTH);

    imagePane.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        Component view = imagePane.getViewport().getView();
        if (null == view)
          return;

        Point p = SwingUtilities.convertPoint(imagePane, e.getPoint(), view);

        int x = p.x * 100 / zoomFactor;
        int y = p.y * 100 / zoomFactor;

        coordinateLabel.setText(x + "/" + y);
      }
    });

    scanImageFiles(baseDir);

    pack();
    setLocationRelativeTo(null);
    setVisible(true);

    if (baseDir.isDirectory())
      scanImageFiles(baseDir);
    else if (baseDir.isFile())
      loadImageFile(baseDir);
  }

  private Component createFileSelector() {
    final Box panel = Box.createHorizontalBox();

    imageSelection = new DefaultComboBoxModel();
    final JComboBox imageComboBox = new JComboBox(imageSelection);
    imageComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final Object i = imageComboBox.getSelectedItem();
        if (i != null && i instanceof File && ((File) i).isFile())
          loadImageFile((File) i);
      }
    });

    panel.add(imageComboBox);

    final JButton selectDirButton = new JButton("...");
    selectDirButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        selectImageDirectory();
      }
    });
    panel.add(selectDirButton);

    return panel;
  }

  void selectImageDirectory() {
    final JFileChooser chooser = new JFileChooser();
    chooser.setCurrentDirectory(baseDir);
    chooser.setAcceptAllFileFilterUsed(false);
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.setFileFilter(new FileFilter() {
      @Override
      public String getDescription() {
        return "Directories";
      }

      @Override
      public boolean accept(File f) {
        return f.isDirectory();
      }
    });

    final int res = chooser.showOpenDialog(this);
    if (res == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
      baseDir = chooser.getSelectedFile();
      scanImageFiles(baseDir);
    }
  }

  private void scanImageFiles(File d) {
    imageSelection.removeAllElements();
    imageSelection.setSelectedItem(null);
    scan(d);
    if (imageSelection.getSize() > 0)
      imageSelection.setSelectedItem(imageSelection.getElementAt(0));
  }

  /**
   * @param d
   */
  private void scan(File f) {
    if (f.isFile()) {
      if (f.getAbsolutePath().indexOf(".svn") < 0)
        imageSelection.addElement(f);
    } else if (f.isDirectory())
      for (final File d : f.listFiles())
        scan(d);
  }

  void loadImageFile(File file) {
    try {
      setImage(ImageIO.read(file));
    } catch (final Exception e) {
      e.printStackTrace();
      JOptionPane.showConfirmDialog(this, e, "Can't load image", JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * @return
   */
  private JPanel createOptionPane() {
    final PropertyChangeListener runListener = new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        runDetection();
      }
    };

    final JPanel optionPane = new JPanel() {
      private static final long serialVersionUID = 1L;

      @Override
      public void setBounds(int x, int y, int width, int height) {
        final Dimension p = getMinimumSize();
        super.setBounds(x, y, width, Math.min(p.height, height));
      }
    };
    optionPane.setLayout(new RackLayout(optionPane, RackLayout.PAGE_AXIS));

    final ScanDirectionsSelector sds = new ScanDirectionsSelector(options);
    sds.addPropertyChangeListener("enabledDirection", runListener);
    optionPane.add(new CollapsibleSection(sds, "Directions to try", "Directions to try", false));

    final SymbologySelector ss = new SymbologySelector(options);
    ss.addPropertyChangeListener("enabledSymbology", runListener);
    optionPane.add(new CollapsibleSection(ss, "Symbologies to try", "Symbologies to try", false));

    debugAnnotationSelector = new DebugAnnotationSelector();
    debugAnnotationSelector.addPropertyChangeListener("enabledDebugMarkers", runListener);
    optionPane.add(new CollapsibleSection(debugAnnotationSelector, "Debug Annotations", "Debug Annotations", true));

    final ScanController scanController = new ScanController(options);
    scanController.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().startsWith("option"))
          runDetection();
      }
    });
    optionPane.add(new CollapsibleSection(scanController, "Scan parameters", "Scan parameters", false));

    binarizedImageBox = new JCheckBox("Show binarized image");
    binarizedImageBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        runDetection();
      }
    });
    optionPane.add(binarizedImageBox);

    final JPanel zoomPanel = new JPanel(new BorderLayout(5, 5));
    zoomPanel.add(new JLabel("Zoom:"));
    zoomField = new JTextField("100");

    zoomField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          zoomFactor = Integer.parseInt(zoomField.getText());
          if (null != resultPane)
            resultPane.updateZoom();
        } catch (final NumberFormatException e1) {
          zoomField.setText("100");
        }
        zoomField.getRootPane().repaint();
      }
    });
    zoomPanel.add(zoomField, BorderLayout.CENTER);
    zoomPanel.add(new JButton(new AbstractAction("fit") {
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(ActionEvent e) {
        zoomToFit();
      }
    }), BorderLayout.LINE_END);

    optionPane.add(zoomPanel);
    return optionPane;
  }

  protected void zoomToFit() {
    if (null == image)
      return;

    Insets i = imagePane.getInsets();
    zoomFactor = (int) Math.floor(Math.min(100.0 * (imagePane.getWidth() - i.left - i.right) / image.getWidth(),
        100.0 * (imagePane.getHeight() - i.top - i.bottom) / image.getHeight()));
    zoomField.setText(Integer.toString(zoomFactor));

    if (null != resultPane)
      resultPane.updateZoom();
  }

  private void setImage(Image image) {
    if (null == image) {
      // update context: remove any previously set image
      this.image = null;
      imagePane.setViewportView(new JLabel("no image loaded"));
      return;
    }

    if (image instanceof BufferedImage)
      this.image = (BufferedImage) image;
    else {
      final MediaTracker mediatracker = new MediaTracker(this);
      mediatracker.addImage(image, 0);
      try {
        mediatracker.waitForAll();
      } catch (final InterruptedException e) {
        // ignore
      }
      this.image = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
      final Graphics2D graphics2d = this.image.createGraphics();
      graphics2d.drawImage(image, 0, 0, this);
    }

    resultPane = new ResultPane(this.image);
    imagePane.setViewportView(resultPane);

    runDetection();
  }

  /**
   * 
   */
  void runDetection() {
    if (null == image)
      return;

    final BufferedImageLuminanceSource src = new BufferedImageLuminanceSource(image);
    List<Integer> thresholds = options.getSettings(BaseSettings.class).getThresholds();
    int threshold = thresholds.isEmpty() ? BaseSettings.AUTO_THRESHOLD : thresholds.get(0);
    BinaryGrid binaryGrid = Detector.prepareBinaryGrid(options, src, threshold);

    if (binarizedImageBox != null && binarizedImageBox.isSelected())
      resultPane.setBinaryImage(Java2DUtils.createBinaryGridImage(binaryGrid));
    else
      resultPane.setBinaryImage(null);

    DiagnosticSettings diagnostics = options.getSettings(DiagnosticSettings.class);
    diagnostics.getDebugMarkers().clear();
    diagnostics.getTransientMarkers().clear();

    diagnostics.setMarkupEnabled(debugAnnotationSelector.getEnabledDebugMarkers().size() > 0);

    resultPane.setDiagSettings(diagnostics);

    if (null != decodeWorker && decodeWorker.getState() != StateValue.DONE)
      decodeWorker.cancel(true);

    decodeWorker = new SwingWorker<List<Result>, Object>() {
      @Override
      protected List<Result> doInBackground() throws Exception {
        return Detector.decode(options, src);
      }

      @Override
      protected void done() {
        try {
          resultPane.setResults(get());
        } catch (CancellationException e) {
          // ignored
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (ExecutionException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    };
    decodeWorker.execute();
  }
}