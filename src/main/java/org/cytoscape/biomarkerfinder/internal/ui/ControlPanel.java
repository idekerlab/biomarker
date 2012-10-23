package org.cytoscape.biomarkerfinder.internal.ui;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.biomarkerfinder.BiomarkerFinderAlgorithm;
import org.cytoscape.biomarkerfinder.BiomarkerFinderAlgorithmFactory;
import org.cytoscape.biomarkerfinder.BiomarkerFinderAlgorithmFactoryManager;
import org.cytoscape.biomarkerfinder.ParameterPanel;
import org.cytoscape.biomarkerfinder.ParameterPanelManager;
import org.cytoscape.biomarkerfinder.event.BiomarkerFinderAlgorithmFactoryAddedEvent;
import org.cytoscape.biomarkerfinder.event.BiomarkerFinderAlgorithmFactoryAddedListener;
import org.cytoscape.biomarkerfinder.internal.DisplayResultTask;
import org.cytoscape.biomarkerfinder.internal.ParameterPanelManagerImpl;
import org.cytoscape.biomarkerfinder.internal.ScoreDataReader;
import org.cytoscape.biomarkerfinder.internal.WeightDataReader;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.NetworkDestroyedEvent;
import org.cytoscape.model.events.NetworkDestroyedListener;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;

public class ControlPanel extends JPanel implements CytoPanelComponent, NetworkAddedListener, NetworkDestroyedListener, BiomarkerFinderAlgorithmFactoryAddedListener{

	private static final long serialVersionUID = 2458177217592334211L;

	private static final String PANEL_TITLE = "Biomarker";

	// GUI Components
	private JTextField expressionMatrixFileField;
	private File expressionMatrixFile = null;
	private JComboBox networkComboBox;
	private JComboBox algorithmComboBox;
	private JButton searchButton;
	private JTextField weightDataField;
	private File weightDataFile = null;
	private JPanel parameterPanel;
	private JSlider HideSlider;
	private JTextField HideSliderField;
	private JPanel buttonsPanel;

	// Services injected via constructor
	private final TaskManager<?, ?> taskManager;
	private final CyNetworkManager netmgr;
	private final BiomarkerFinderAlgorithmFactoryManager algorithmManager;
	private final CyApplicationManager appManager;
	private final ParameterPanelManager parameterManager;

	private CyNetwork network;

	public ControlPanel(final TaskManager<?, ?> taskManager, final CyNetworkManager netmgr,
			final BiomarkerFinderAlgorithmFactoryManager algorithmManager, final CyApplicationManager appManager, final ParameterPanelManager parameterManager) {

		// Inject services
		this.taskManager = taskManager;
		this.netmgr = netmgr;
		this.algorithmManager = algorithmManager;
		this.appManager = appManager;
		this.parameterManager = parameterManager;

		// Init GUI components
		this.setLayout(new GridBagLayout());
		addInputPanel();
		updateParameterPanel();
		addButtonPanel();
	}

	@Override
	public void handleEvent(NetworkDestroyedEvent arg0) {
		networkComboBox.removeAllItems();
		for (CyNetwork net : netmgr.getNetworkSet()) {
			networkComboBox.addItem(net);
		}
	}

	@Override
	public void handleEvent(NetworkAddedEvent arg0) {
		networkComboBox.removeAllItems();
		for (CyNetwork net : netmgr.getNetworkSet()) {
			networkComboBox.addItem(net);
		}
	}

	public class weightDataAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser filechooser = new JFileChooser();
			int selected = filechooser.showOpenDialog(new JPanel());
			if (selected == JFileChooser.APPROVE_OPTION) {
				weightDataFile = filechooser.getSelectedFile();
				weightDataField.setText(weightDataFile.getName());
			} else if (selected == JFileChooser.CANCEL_OPTION) {
				weightDataField.setText("canceled");
			} else if (selected == JFileChooser.ERROR_OPTION) {
				weightDataField.setText("error or canceled");
			}
		}

	}

	public class ResetAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			resetStart();
			System.gc();
			addInputPanel();
			addButtonPanel();
		}

	}

	public class SearchAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			network = (CyNetwork) networkComboBox.getSelectedItem();
			new ScoreDataReader(network, expressionMatrixFile);
			if (weightDataFile == null) {
				return;
			}
			new WeightDataReader(network, weightDataFile);
			
			final Object selectedAlgorithmName = algorithmComboBox.getSelectedItem();
			if(selectedAlgorithmName == null)
				return;
			
			final BiomarkerFinderAlgorithmFactory factory = algorithmManager.getAlgorithm(selectedAlgorithmName.toString());
			if(factory == null)
				return;
			
			final NetworkTaskFactory networkTaskFactory = (NetworkTaskFactory) factory;
			final TaskIterator itr = networkTaskFactory.createTaskIterator(network);
			final Map<String, Object> parameters = ((ParameterPanel) parameterPanel).getParameters();
			
			final BiomarkerFinderAlgorithm algorithm = (BiomarkerFinderAlgorithm) itr.next();
			algorithm.setParameter(parameters);
			final TaskIterator finalItr = new TaskIterator(algorithm, new DisplayResultTask(algorithm));
			
			taskManager.execute(finalItr);
			
			
			// TODO: make all of the following into tasks!
			changeOriginalView();
			
			addHideSlider();
			addResetButton();
		}

	}

	public class ChooseExpressionMatrixFileAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser filechooser = new JFileChooser();
			int selected = filechooser.showOpenDialog(new JPanel());
			if (selected == JFileChooser.APPROVE_OPTION) {
				expressionMatrixFile = filechooser.getSelectedFile();
				expressionMatrixFileField.setText(expressionMatrixFile.getName());
				updateSearchButton();
			} else if (selected == JFileChooser.CANCEL_OPTION) {
				expressionMatrixFileField.setText("canceled");
			} else if (selected == JFileChooser.ERROR_OPTION) {
				expressionMatrixFileField.setText("error or canceled");
			}
		}
	}

	public class networkComboBoxAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			updateSearchButton();
		}

	}

	public class CloseAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			// CySwingApplication desktop =
			// register.getService(CySwingApplication.class);
			// CytoPanel westPanel = desktop.getCytoPanel(CytoPanelName.WEST);
			// westPanel.setState(CytoPanelState.HIDE);
			// JOptionPane.showMessageDialog(null, "Close Button is hitted");
			panelClose();
		}
	}

	public class ChooseAlgorithmAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			// FIXME: DO NOT USE IF
			// Create panel manager and create panel dynamically
			updateParameterPanel();

		}

	}

	public class HideSliderAction implements ChangeListener {

		public void stateChanged(ChangeEvent e) {
			extractBiomarker();
		}
	}

	private void extractBiomarker() {
		CyNetworkView currentView = appManager.getCurrentNetworkView();
		if (currentView == null)
			return;

		// View is always associated with its model.
		final CyNetwork ntwk = currentView.getModel();
		double threshold = (getMaximum(ntwk.getDefaultNodeTable().getColumn("score")) * HideSlider.getValue()) / 100;
		for (CyNode node : ntwk.getNodeList()) {
			if (ntwk.getDefaultNodeTable().getRow(node.getSUID()).get("score", Double.class) < threshold) {
				currentView.getNodeView(node).setVisualProperty(BasicVisualLexicon.NODE_VISIBLE, false);

			} else if (ntwk.getDefaultNodeTable().getRow(node.getSUID()).get("score", Double.class) >= threshold) {
				currentView.getNodeView(node).setVisualProperty(BasicVisualLexicon.NODE_VISIBLE, true);
				Iterator it = ntwk.getAdjacentEdgeIterable(node, CyEdge.Type.ANY).iterator();
				while (it.hasNext()) {
					currentView.getEdgeView((CyEdge) it.next())
							.setVisualProperty(BasicVisualLexicon.EDGE_VISIBLE, true);
				}
			}
		}
		HideSliderField.setText(HideSlider.getValue() + "% of max score");
		currentView.updateView();
	}

	private void updateParameterPanel(){
		BiomarkerFinderAlgorithmFactory factory = (BiomarkerFinderAlgorithmFactory) algorithmComboBox.getSelectedItem();
		if(factory==null){
			removeParameterPanel();
			return;
		}
		if(((ParameterPanelManagerImpl)parameterManager).getPanelCount()!=0){
			parameterPanel = (JPanel) parameterManager.getParameterPanel(factory.getAlgorithmID());
			addParameterPanel(parameterPanel);
		}
	}
	
	private void resetStart() {
		this.removeAll();
		this.repaint();
	}

	private void addInputPanel() {
		JLabel expressionMatrixFileLabel = new JLabel("expression matrix file:");
		expressionMatrixFileField = new JTextField("None Choosen");
		expressionMatrixFileField.setEditable(false);
		JButton expressionMatrixFileButton = new JButton("Choose...");
		expressionMatrixFileButton.addActionListener(new ChooseExpressionMatrixFileAction());

		JLabel networkLabel = new JLabel("Network:");
		networkComboBox = new JComboBox();
		for (CyNetwork net : netmgr.getNetworkSet()) {
			networkComboBox.addItem(net);
		}
		networkComboBox.addActionListener(new networkComboBoxAction());

		JLabel weightLabel = new JLabel("weight file:");
		weightDataField = new JTextField("None Choosen");
		weightDataField.setEditable(false);
		JButton weightDataButton = new JButton("Choose...");
		weightDataButton.addActionListener(new weightDataAction());

		JLabel algorithmLabel = new JLabel("algorithm");
		algorithmComboBox = new JComboBox();
		for(BiomarkerFinderAlgorithmFactory factory: algorithmManager.getAllFactories()) {
			algorithmComboBox.addItem(factory);
		}
		algorithmComboBox.addActionListener(new ChooseAlgorithmAction());

		// input part panel
		{
			JPanel filePanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.FIRST_LINE_START;
			c.anchor = GridBagConstraints.FIRST_LINE_START;
			c.insets = new Insets(5, 5, 5, 5);

			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0.0;
			c.weighty = 0.0;
			filePanel.add(expressionMatrixFileLabel, c);

			c.gridx = 1;
			c.gridy = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.weighty = 0.0;
			filePanel.add(expressionMatrixFileField, c);

			c.gridx = 2;
			c.gridy = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0.0;
			c.weighty = 0.0;
			filePanel.add(expressionMatrixFileButton, c);

			c.gridx = 0;
			c.gridy = 1;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0.0;
			c.weighty = 0.0;
			filePanel.add(networkLabel, c);

			c.gridx = 1;
			c.gridy = 1;
			c.gridwidth = 2;
			c.gridheight = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.weighty = 0.0;
			filePanel.add(networkComboBox, c);

			c.gridx = 0;
			c.gridy = 2;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0.0;
			c.weighty = 0.0;
			filePanel.add(weightLabel, c);

			c.gridx = 1;
			c.gridy = 2;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.weighty = 0.0;
			filePanel.add(weightDataField, c);

			c.gridx = 2;
			c.gridy = 2;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0.0;
			c.weighty = 0.0;
			filePanel.add(weightDataButton, c);

			c.gridx = 0;
			c.gridy = 3;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0.0;
			c.weighty = 0.0;
			filePanel.add(algorithmLabel, c);

			c.gridx = 1;
			c.gridy = 3;
			c.gridwidth = 2;
			c.gridheight = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.weighty = 0.0;
			filePanel.add(algorithmComboBox, c);

			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.weighty = 0.0;
			add(filePanel, c);
		}

		JSeparator separator0 = new JSeparator();
		{
			GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(5, 5, 5, 5);
			c.gridx = 0;
			c.gridy = 1;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.weighty = 0.0;
			add(separator0, c);
		}

	}

	private void addButtonPanel() {
		searchButton = new JButton("Search");
		searchButton.addActionListener(new SearchAction());
		updateSearchButton();
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new CloseAction());
		{
			buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			buttonsPanel.add(closeButton);
			buttonsPanel.add(searchButton);

			GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(5, 5, 5, 5);
			c.anchor = GridBagConstraints.LAST_LINE_END;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.gridx = 0;
			c.gridy = 6;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 1.0;
			c.weighty = 1.0;
			add(buttonsPanel, c);
		}
	}

	private void addResetButton() {
		JButton resetButton = new JButton("Reset");
		resetButton.addActionListener(new ResetAction());
		buttonsPanel.add(resetButton);
	}

	private void panelClose() {
		this.removeAll();
		this.repaint();
	}

	private void removeParameterPanel() {
		// this.parameterPanel = new JPanel();
		if (parameterPanel == null) {
			return;
		}
		this.remove(parameterPanel);
		this.repaint();
	}

	private void addParameterPanel(JPanel panel) {
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = new Insets(5, 5, 5, 5);
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.weighty = 0.0;
		add(panel, c);
		this.repaint();
	}

	private void addHideSlider() {
		JPanel HideSliderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		HideSliderPanel.setLayout(new GridBagLayout());

		JLabel HideSliderLabel = new JLabel("Hide Score Threshold");
		HideSliderField = new JTextField(18);
		HideSliderField.setEditable(false);
		HideSlider = new JSlider(0, 100, 0);

		HideSlider.addChangeListener(new HideSliderAction());
		HideSliderField.setText(HideSlider.getValue() + "% of max score");
		{
			GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(5, 5, 5, 5);
			c.gridwidth = 1;
			c.gridheight = 1;
			c.gridx = 0;
			c.gridy = 0;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.weighty = 0.0;

			HideSliderPanel.add(HideSliderLabel, c);

			c.gridwidth = 1;
			c.gridheight = 1;
			c.gridx = 0;
			c.gridy = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.weighty = 0.0;

			HideSliderPanel.add(HideSliderField, c);

			c.gridwidth = 1;
			c.gridheight = 1;
			c.gridx = 1;
			c.gridy = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.weighty = 0.0;

			HideSliderPanel.add(HideSlider, c);
		}

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5, 5, 5, 5);
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.weighty = 0.0;

		parameterPanel.add(HideSliderPanel, c);
		parameterPanel.repaint();
		this.repaint();
	}

	private Double getMinimum(CyColumn c) {
		Double x = Double.MAX_VALUE;
		List<Double> values = c.getValues(Double.class);
		Iterator<Double> it = values.iterator();
		while (it.hasNext()) {
			Double y = it.next();
			if (y < x) {
				x = y;
			}
		}
		return x;
	}

	private Double getMaximum(CyColumn c) {
		double x = Double.MIN_VALUE;
		List<Double> values = c.getValues(Double.class);
		Iterator<Double> it = values.iterator();
		while (it.hasNext()) {
			Double y = it.next();
			if (y > x) {
				x = y;
			}
		}
		return x;
	}

	private void updateSearchButton() {
		searchButton.setEnabled(false);
		if (expressionMatrixFile == null)
			searchButton.setToolTipText("An expression matrix file must be selected before searching.");
		else if (networkComboBox.getItemCount() == 0)
			searchButton.setToolTipText("There must be at least one network available before searching.");
		else {
			searchButton.setEnabled(true);
			searchButton.setToolTipText(null);
		}
	}

	private void changeOriginalView() {

		//FIXME: Create external class to handle this.  Also, this is mostly same as DisplayResult!
		
//		Collection<CyNetworkView> views = viewManager.getNetworkViews(network);
//		CyNetworkView originalView = views.iterator().next();
//		VisualMappingFunctionFactory passthroughFactory = register.getService(VisualMappingFunctionFactory.class,
//				"(mapping.type=passthrough)");
//
//		PassthroughMapping pMapping = (PassthroughMapping) passthroughFactory.createVisualMappingFunction("name",
//				String.class, BasicVisualLexicon.NODE_LABEL);
//
//		VisualMappingFunctionFactory continuousFactory = register.getService(VisualMappingFunctionFactory.class,
//				"(mapping.type=continuous)");
//		VisualMappingManager vmManager = register.getService(VisualMappingManager.class);
//		VisualStyleFactory vsFactory = register.getService(VisualStyleFactory.class);
//		VisualStyle vs = vsFactory.createVisualStyle(network.getDefaultNetworkTable().getRow(network.getSUID())
//				.get("name", String.class));
//
//		ContinuousMapping mapping = (ContinuousMapping) continuousFactory.createVisualMappingFunction("score",
//				Double.class, BasicVisualLexicon.NODE_FILL_COLOR);
//
//		Double val1 = getMinimum(network.getDefaultNodeTable().getColumn("score"));
//		BoundaryRangeValues<Paint> brv1 = new BoundaryRangeValues<Paint>(Color.GREEN, Color.GREEN, Color.GREEN);
//
//		Double val3 = getMaximum(network.getDefaultNodeTable().getColumn("score"));
//		BoundaryRangeValues<Paint> brv3 = new BoundaryRangeValues<Paint>(Color.RED, Color.RED, Color.RED);
//
//		Double val2 = (val1 + val3 + val1 + val1) / 4;
//		BoundaryRangeValues<Paint> brv2 = new BoundaryRangeValues<Paint>(Color.YELLOW, Color.YELLOW, Color.YELLOW);
//
//		mapping.addPoint(val1, brv1);
//		mapping.addPoint(val2, brv2);
//		mapping.addPoint(val3, brv3);
//
//		vs.addVisualMappingFunction(pMapping);
//		vs.addVisualMappingFunction(mapping);
//		vs.apply(originalView);
//
//		vmManager.addVisualStyle(vs);
//		originalView.updateView();
//
//		appManager.setCurrentNetwork(network);
//		vmManager.setVisualStyle(vs, originalView);
//		appManager.setCurrentNetworkView(originalView);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.WEST;
	}

	@Override
	public String getTitle() {
		return PANEL_TITLE;
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public void handleEvent(BiomarkerFinderAlgorithmFactoryAddedEvent e) {
		algorithmComboBox.addItem(e.getFacotry());
		updateParameterPanel();
	}

}