package org.cytoscape.biomarker.internal;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;

/**
 * Sample class to add menu item to desktop
 *
 */
public class MenuAction extends AbstractCyAction {

	private static final long serialVersionUID = 6651394680524283663L;

	public MenuAction(CyApplicationManager cyApplicationManager, final String menuTitle) {

		super(menuTitle, cyApplicationManager, null, null);
		setPreferredMenu("Tools.Biomarker");

	}

	public void actionPerformed(ActionEvent e) {
		// Write your own function here.
		JOptionPane.showMessageDialog(null, "Sample menu for biomarker plugin.");
	}
}
