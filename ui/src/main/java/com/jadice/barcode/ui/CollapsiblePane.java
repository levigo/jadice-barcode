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
/**
 * L2FProd.com Common Components 7.3 License.
 * 
 * Copyright 2005-2007 L2FProd.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * <code>JCollapsiblePane</code> provides a component which can collapse or
 * expand its content area with animation and fade in/fade out effects. It also
 * acts as a standard container for other Swing components.
 * 
 * <p>
 * In this example, the <code>JCollapsiblePane</code> is used to build a Search
 * pane which can be shown and hidden on demand.
 * 
 * <pre>
 * &lt;code&gt;
 * JCollapsiblePane cp = new JCollapsiblePane();
 * 
 * // JCollapsiblePane can be used like any other container
 * cp.setLayout(new BorderLayout());
 * 
 * // the Controls panel with a textfield to filter the tree
 * JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
 * controls.add(new JLabel(&quot;Search:&quot;));
 * controls.add(new JTextField(10));
 * controls.add(new JButton(&quot;Refresh&quot;));
 * controls.setBorder(new TitledBorder(&quot;Filters&quot;));
 * cp.add(&quot;Center&quot;, controls);
 * 
 * JFrame frame = new JFrame();
 * frame.setLayout(new BorderLayout());
 * 
 * // Put the &quot;Controls&quot; first
 * frame.add(&quot;North&quot;, cp);
 * 
 * // Then the tree - we assume the Controls would somehow filter the tree
 * JScrollPane scroll = new JScrollPane(new JTree());
 * frame.add(&quot;Center&quot;, scroll);
 * 
 * // Show/hide the &quot;Controls&quot;
 * JButton toggle = new JButton(cp.getActionMap().get(
 * 		JCollapsiblePane.TOGGLE_ACTION));
 * toggle.setText(&quot;Show/Hide Search Panel&quot;);
 * frame.add(&quot;South&quot;, toggle);
 * 
 * frame.pack();
 * frame.setVisible(true);
 * &lt;/code&gt;
 * </pre>
 * 
 * <p>
 * Note: <code>JCollapsiblePane</code> requires its parent container to have a
 * {@link java.awt.LayoutManager} using {@link #getPreferredSize()} when
 * calculating its layout {@link java.awt.BorderLayout} ).
 * 
 * @javabean.attribute name="isContainer" value="Boolean.TRUE" rtexpr="true"
 * 
 * @javabean.attribute name="containerDelegate" value="getContentPane"
 * 
 * @javabean.class name="JCollapsiblePane" shortDescription="A pane which hides
 *                 its content with an animation."
 *                 stopClass="java.awt.Component"
 * 
 */
public class CollapsiblePane extends JPanel {
	/**
	 * 
	 */
	private static final int MAX_ANIMATION_STEP = 4;

	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	private static final int ANIMATION_STEPS = 10;

	/**
	 * 25 FPS
	 */
	private static final int ANIMATION_DELAY = 1000 / 25;

	/**
	 * Used when generating PropertyChangeEvents for the "animationState" property
	 */
	public final static String ANIMATION_STATE_KEY = "animationState";

	/**
	 * Indicates whether the component is collapsed or expanded
	 */
	private boolean collapsed = false;

	/**
	 * Timer used for doing the transparency animation (fade-in)
	 */
	Timer animateTimer;

	/**
	 * This is either a width or a height
	 */
	private boolean useAnimation = true;
	private int orientation = SwingConstants.VERTICAL;

	float alpha = 1.0f;

	private Container contentPanel;

	private boolean usePrescribedSize = false;

	/**
	 * The icon used by the "toggle" action when the JCollapsiblePane is expanded,
	 * i.e the icon which indicates the pane can be collapsed.
	 */
	public final static String COLLAPSED_ICON = "collapseIcon";

	/**
	 * The name used by the "toggle" action when the JCollapsiblePane is expanded,
	 * i.e the name which indicates the pane can be collapsed.
	 */
	public final static String COLLAPSED_NAME = "collapseName";

	/**
	 * The icon used by the "toggle" action when the JCollapsiblePane is
	 * collapsed, i.e the icon which indicates the pane can be expanded.
	 */
	public final static String EXPANDED_ICON = "expandIcon";

	/**
	 * The name used by the "toggle" action when the JCollapsiblePane is
	 * collapsed, i.e the name which indicates the pane can be expanded.
	 */
	public final static String EXPANDED_NAME = "expandName";

	/**
	 * JCollapsible has a built-in toggle action which can be bound to buttons.
	 * Accesses the action through
	 * <code>collapsiblePane.getActionMap().get(JCollapsiblePane.TOGGLE_ACTION)</code>
	 * .
	 */
	public final static String TOGGLE_ACTION = "toggle";

	/**
	 * Constructs a new JCollapsiblePane with a {@link JPanel} as content pane and
	 * a vertical with a gap of 2 pixels as layout manager.
	 */
	public CollapsiblePane(Container contentPane) {
		super.setLayout(new BorderLayout(0, 0));

		setContentPane(contentPane);

		getActionMap().put(TOGGLE_ACTION, new ToggleAction());
	}

	/**
	 * Constructs a new JCollapsiblePane with a {@link JPanel} as content pane and
	 * a vertical with a gap of 2 pixels as layout manager.
	 */
	public CollapsiblePane() {
		this(new JPanel(new BorderLayout()));
	}

	/**
	 * Sets the content pane of this JCollapsiblePane. Components must be added to
	 * this content pane, not to the JCollapsiblePane.
	 * 
	 * @param contentPanel
	 * @throws IllegalArgumentException if contentPanel is null
	 */
	public void setContentPane(Container contentPanel) {
		if (contentPanel == null)
			throw new IllegalArgumentException("Content pane can't be null");

		this.contentPanel = contentPanel;
		super.addImpl(contentPanel, BorderLayout.CENTER, -1);
	}

	/**
	 * @return the content pane
	 */
	public Container getContentPanel() {
		return contentPanel;
	}

	// /**
	// * Overriden to redirect call to the content pane.
	// */
	// protected void addImpl(Component comp, Object constraints, int index) {
	// getContentPane().add(comp, constraints, index);
	// }
	//
	// /**
	// * Overriden to redirect call to the content pane
	// */
	// public void remove(Component comp) {
	// getContentPane().remove(comp);
	// }
	//
	// /**
	// * Overriden to redirect call to the content pane.
	// */
	// public void remove(int index) {
	// getContentPane().remove(index);
	// }
	//
	// /**
	// * Overriden to redirect call to the content pane.
	// */
	// public void removeAll() {
	// getContentPane().removeAll();
	// }

	/**
	 * If true, enables the animation when pane is collapsed/expanded. If false,
	 * animation is turned off.
	 * 
	 * <p>
	 * When animated, the <code>JCollapsiblePane</code> will progressively reduce
	 * (when collapsing) or enlarge (when expanding) the height of its content
	 * area until it becomes 0 or until it reaches the preferred height of the
	 * components it contains. The transparency of the content area will also
	 * change during the animation.
	 * 
	 * <p>
	 * If not animated, the <code>JCollapsiblePane</code> will simply hide
	 * (collapsing) or show (expanding) its content area.
	 * 
	 * @param animated
	 * @javabean.property bound="true" preferred="true"
	 */
	public void setAnimated(boolean animated) {
		if (animated != useAnimation) {
			useAnimation = animated;
			firePropertyChange("animated", !useAnimation, useAnimation);
		}
	}

	/**
	 * @return true if the pane is animated, false otherwise
	 * @see #setAnimated(boolean)
	 */
	public boolean isAnimated() {
		return useAnimation;
	}

	/**
	 * @return true if the pane is collapsed, false if expanded
	 */
	public boolean isCollapsed() {
		return collapsed;
	}

	/**
	 * Expands or collapses this <code>JCollapsiblePane</code>.
	 * 
	 * <p>
	 * If the component is collapsed and <code>val</code> is false, then this call
	 * expands the JCollapsiblePane, such that the entire JCollapsiblePane will be
	 * visible. If {@link #isAnimated()} returns true, the expansion will be
	 * accompanied by an animation.
	 * 
	 * <p>
	 * However, if the component is expanded and <code>val</code> is true, then
	 * this call collapses the JCollapsiblePane, such that the entire
	 * JCollapsiblePane will be invisible. If {@link #isAnimated()} returns true,
	 * the collapse will be accompanied by an animation.
	 * 
	 * @see #isAnimated()
	 * @see #setAnimated(boolean)
	 * @javabean.property bound="true" preferred="true"
	 */
	public void setCollapsed(boolean val) {
		if (collapsed != val) {
			collapsed = val;
			if (isAnimated()) {
				if (collapsed) {
					int size = orientation == SwingConstants.VERTICAL
							? getHeight()
							: getWidth();

					animate(ANIMATION_DELAY, Math.max(MAX_ANIMATION_STEP, size
							/ ANIMATION_STEPS), 1.0f, 0.01f, size, 0);
				} else {
					int currentSize = orientation == SwingConstants.VERTICAL
							? getContentPanel().getSize().height
							: getContentPanel().getSize().width;

					if (!getContentPanel().isVisible())
						currentSize = 0;

					int finalSize = orientation == SwingConstants.VERTICAL
							? getContentPanel().getPreferredSize().height
							: getContentPanel().getPreferredSize().width;

					// System.out.println("Expand from " + currentSize + " to " +
					// finalSize);
					animate(ANIMATION_DELAY,
							Math.max(MAX_ANIMATION_STEP, finalSize / ANIMATION_STEPS), //
							(currentSize + 1.0f) / (finalSize + 1.0f), 1.0f, currentSize,
							finalSize);
				}
			} else {
				if (collapsed)
					setStateCollapsed();
				else
					setStateExpanded();
				invalidate();
				doLayout();
			}
			repaint();
			firePropertyChange("collapsed", !collapsed, collapsed);
		}
	}

	/**
	 * @param animationDelay
	 * @param max
	 * @param f
	 * @param g
	 * @param size
	 * @param i
	 */
	private void animate(int animationDelay, int increment, float alphaStart,
			float alphaEnd, int startSize, int finalSize) {
		// TODO Auto-generated method stub

		if (animateTimer != null)
			animateTimer.stop();

		setStateAnimated();

		animateTimer = new Timer(animationDelay, new AnimationListener(increment,
				alphaStart, alphaEnd, startSize, finalSize));
		animateTimer.setInitialDelay(animationDelay);
		animateTimer.start();

		CollapsiblePane.this
				.firePropertyChange(ANIMATION_STATE_KEY, null, "reinit");
	}

	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	/**
	 * The critical part of the animation of this <code>JCollapsiblePane</code>
	 * relies on the calculation of its preferred size. During the animation, its
	 * preferred size (specially its height) will change, when expanding, from 0
	 * to the preferred size of the content pane, and the reverse when collapsing.
	 * 
	 * @return this component preferred size
	 */
	@Override
	public Dimension getPreferredSize() {
		if (usePrescribedSize)
			return getSize();

		return super.getPreferredSize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.Component#setBounds(java.awt.Rectangle)
	 */
	@Override
	public void setBounds(int x, int y, int w, int h) {
		if (usePrescribedSize)
			if (orientation == SwingConstants.VERTICAL)
				h = getHeight();
			else
				w = getWidth();

		super.setBounds(x, y, w, h);
	}

	/**
	 * Tagging interface for containers in a JCollapsiblePane hierarchy who needs
	 * to be revalidated (invalidate/validate/repaint) when the pane is expanding
	 * or collapsing. Usually validating only the parent of the JCollapsiblePane
	 * is enough but there might be cases where the parent parent must be
	 * validated.
	 */
	public static interface JCollapsiblePaneContainer {
		Container getValidatingContainer();
	}

	/**
	 * This class actual provides the animation support for scrolling up/down this
	 * component. This listener is called whenever the animateTimer fires off. It
	 * fires off in response to scroll up/down requests. This listener is
	 * responsible for modifying the size of the content container and causing it
	 * to be repainted.
	 * 
		 */
	private final class AnimationListener implements ActionListener {
		/**
		 * This is the starting width/height when animating. If > finalHeight, then
		 * the animation is going to be to scroll up the component. If it is < then
		 * finalHeight, then the animation will scroll down the component.
		 */
		private final int startSize;

		/**
		 * This is the final width/height that the content container is going to be
		 * when scrolling is finished.
		 */
		private final int finalSize;

		/**
		 * The current alpha setting used during "animation" (fade-in/fade-out)
		 */
		private final float alphaEnd;
		private final float alphaStart;
		private final int delta;

		/**
		 * @param alphaStart2
		 * @param alphaEnd2
		 * @param startSize2
		 * @param finalSize2
		 */
		public AnimationListener(int delta, float alphaStart, float alphaEnd,
				int startSize, int finalSize) {
			this.delta = delta;
			this.alphaStart = alphaStart;
			this.alphaEnd = alphaEnd;
			this.startSize = startSize;
			this.finalSize = finalSize;
		}

		public void actionPerformed(ActionEvent e) {

			int size = (orientation == SwingConstants.VERTICAL
					? getHeight()
					: getWidth());

			// System.out.println(startSize + " -> " + size + " -> " + finalSize);

			/*
			 * Pre-1) If startHeight == finalHeight, then we're done so stop the timer
			 * 1) Calculate whether we're contracting or expanding. 2) Calculate the
			 * delta (which is either positive or negative, depending on the results
			 * of (1)) 3) Calculate the alpha value 4) Resize the ContentContainer 5)
			 * Revalidate/Repaint the content container
			 */
			if (size == finalSize) {
				animateTimer.stop();
				alpha = alphaEnd;

				// keep the content pane hidden when it is collapsed, other
				// it may still receive focus.
				if (finalSize > 0) {
					setStateExpanded();
					validate();
					CollapsiblePane.this.firePropertyChange(ANIMATION_STATE_KEY, null,
							"expanded");
				} else {
					setStateCollapsed();
					validate();
					CollapsiblePane.this.firePropertyChange(ANIMATION_STATE_KEY, null,
							"collapsed");
				}
				return;
			}

			final boolean contracting = startSize > finalSize;
			size += contracting ? -1 * delta : delta;

			if (contracting)
				size = Math.max(size, finalSize);
			else
				size = Math.min(size, finalSize);

			// resize
			if (orientation == SwingConstants.VERTICAL)
				CollapsiblePane.super.setBounds(getX(), getY(), getWidth(), size);
			else
				CollapsiblePane.super.setBounds(getX(), getY(), size, getHeight());

			// calculate and clamp alpha
			alpha = (float) (size - finalSize) / (startSize - finalSize)
					* (alphaStart - alphaEnd) + alphaEnd;
			alpha = Math.min(alpha, Math.max(alphaStart, alphaEnd));
			alpha = Math.max(alpha, Math.min(alphaStart, alphaEnd));

			validate();
		}

		void validate() {
			Container parent = SwingUtilities.getAncestorOfClass(
					JCollapsiblePaneContainer.class, CollapsiblePane.this);
			if (parent != null)
				parent = ((JCollapsiblePaneContainer) parent).getValidatingContainer();
			else
				parent = getParent();

			if (parent != null) {
				/*
				 * we always invalidate, since some layout managers (yes, I'm looking at
				 * you, FormLayout) cache stuff that doesn't get properly invalidated in
				 * certain rather weird cases.
				 */
				parent.invalidate();

				if (parent instanceof JComponent)
					((JComponent) parent).revalidate();

				parent.doLayout();
				parent.repaint();
			}
		}
	}

	/**
	 * Toggles the JCollapsiblePane state and updates its icon based on the
	 * JCollapsiblePane "collapsed" status.
	 */
	private class ToggleAction extends AbstractAction
			implements
				PropertyChangeListener {
		private static final long serialVersionUID = 1L;

		public ToggleAction() {
			update();

			// the action must track the collapsed status of the pane to update
			// its icon
			CollapsiblePane.this.addPropertyChangeListener("collapsed", this);
		}

		@Override
		public void putValue(String key, Object newValue) {
			super.putValue(key, newValue);
			if (EXPANDED_ICON.equals(key) || COLLAPSED_ICON.equals(key)
					|| EXPANDED_NAME.equals(key) || COLLAPSED_NAME.equals(key))
				update();

			if (NAME.equals(key)) {
				super.putValue(EXPANDED_NAME, newValue);
				super.putValue(COLLAPSED_NAME, newValue);
				update();
			}

			if (SMALL_ICON.equals(key)) {
				super.putValue(EXPANDED_ICON, newValue);
				super.putValue(COLLAPSED_ICON, newValue);
				update();
			}
		}

		public void actionPerformed(ActionEvent e) {
			setCollapsed(!isCollapsed());
		}

		public void propertyChange(PropertyChangeEvent evt) {
			update();
		}

		void update() {
			if (!isCollapsed()) {
				super.putValue(SMALL_ICON, getValue(EXPANDED_ICON));
				super.putValue(NAME, getValue(EXPANDED_NAME));
			} else {
				super.putValue(SMALL_ICON, getValue(COLLAPSED_ICON));
				super.putValue(NAME, getValue(COLLAPSED_NAME));
			}
		}
	}

	/**
	 * @return the orientation
	 * @see SwingConstants#HORIZONTAL
	 * @see SwingConstants#VERTICAL
	 */
	public int getOrientation() {
		return orientation;
	}

	/**
	 * Set the orientation
	 * 
	 * @param orientation the orientation to set
	 * @see SwingConstants#HORIZONTAL
	 * @see SwingConstants#VERTICAL
	 */
	public void setOrientation(int orientation) {
		this.orientation = orientation;
	}

	void setStateAnimated() {
		contentPanel.setVisible(true);
		usePrescribedSize = true;
	}

	void setStateCollapsed() {
		contentPanel.setVisible(false);
		usePrescribedSize = false;
	}

	void setStateExpanded() {
		contentPanel.setVisible(true);
		usePrescribedSize = false;
	}
}
