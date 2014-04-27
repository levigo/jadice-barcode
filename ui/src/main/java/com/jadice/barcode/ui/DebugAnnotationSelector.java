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

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

import com.jadice.barcode.Marker;
import com.jadice.barcode.Marker.Feature;

public class DebugAnnotationSelector extends JComponent {
	private static final long serialVersionUID = 1L;

	class FeatureCheckBox extends JCheckBox {
		private static final long serialVersionUID = 1L;
		private final Feature feature;

		public FeatureCheckBox(final Marker.Feature feature, String title,
				boolean enabled) {
			super();

			setAction(new AbstractAction(title) {
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					syncModelFromPresentation();
					((JComponent) e.getSource()).getRootPane().repaint();
				}
			});

			this.feature = feature;
			setSelected(enabled);
			syncModelFromPresentation();
		}

		public FeatureCheckBox(Feature feature, String title) {
			this(feature, title, false);
		}

		/**
		 * @param feature
		 * @param e
		 */
		void syncModelFromPresentation() {
			if (isSelected())
				enabledDebugMarkers.add(feature);
			else
				enabledDebugMarkers.remove(feature);

			DebugAnnotationSelector.this.firePropertyChange("enabledDebugMarkers",
					!isSelected(), isSelected());
		}

		void syncPresentationFromModel() {
			setSelected(enabledDebugMarkers.contains(feature));
		}
	}

	final Set<Feature> enabledDebugMarkers = new HashSet<Feature>();

	public DebugAnnotationSelector() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(new FeatureCheckBox(Feature.INITIAL_SCAN, "Initial scans", true));
		add(new FeatureCheckBox(Feature.SCAN, "Secondary (fine) scans"));
		add(new FeatureCheckBox(Feature.DETECTION_SCAN, "Detection scans"));
		add(new FeatureCheckBox(Feature.START, "Start detections", true));
		add(new FeatureCheckBox(Feature.START_RESCAN, "Start rescans"));
		add(new FeatureCheckBox(Feature.START_EDGE, "Start edges"));
		add(new FeatureCheckBox(Feature.STOP, "Stop detections", true));
		add(new FeatureCheckBox(Feature.STOP_RESCAN, "Stop rescans"));
		add(new FeatureCheckBox(Feature.STOP_EDGE, "Stop edges"));
		add(new FeatureCheckBox(Feature.HISTOGRAM, "Histogram"));
		add(new FeatureCheckBox(Feature.HISTOGRAM_THRESHOLD, "Histogram threshold"));
		add(new FeatureCheckBox(Feature.EDGE, "Edge areas"));
		add(new FeatureCheckBox(Feature.INVALID_EDGE, "Edge areas deemed invalid"));
		add(new FeatureCheckBox(Feature.SINGLETON_EDGE,
				"Edge areas without a partner"));
		add(new FeatureCheckBox(Feature.EDGE_ASSOCIATION, "Edge assicoations", true));
		add(new FeatureCheckBox(Feature.METHOD2, "Aggregate scan"));
	}

	public void setMarkerEnabled(Feature f, boolean enabled) {
		if (enabled)
			enabledDebugMarkers.add(f);
		else
			enabledDebugMarkers.remove(f);

		for (int i = 0; i < getComponentCount(); i++)
			if (getComponent(i) instanceof FeatureCheckBox)
				((FeatureCheckBox) getComponent(i)).syncPresentationFromModel();
	}

	/**
	 * @return the enabledDebugMarkers
	 */
	public Set<Feature> getEnabledDebugMarkers() {
		return enabledDebugMarkers;
	}
}
