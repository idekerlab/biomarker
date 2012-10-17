package org.cytoscape.Biomarker.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;

import org.cytoscape.Biomarker.BiomarkerFinderAlgorithm;
import org.cytoscape.Biomarker.ParameterPanel;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyEdge.Type;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.NetworkDestroyedEvent;
import org.cytoscape.model.events.NetworkDestroyedListener;
import org.cytoscape.service.util.internal.CyServiceRegistrarImpl;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.osgi.framework.BundleContext;

import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
public class ControlPanel extends JPanel implements CytoPanelComponent {

	private JTextField expressionMatrixFileField;
	private File expressionMatrixFile = null;
	private JComboBox<CyNetwork> networkComboBox;
	private JComboBox algorithmComboBox;
	private JButton searchButton;
	private CyNetwork network;
	private CyNetwork result;
	private JTextField weightDataField;
	private File weightDataFile =null;
	private CyNetworkManager netmgr;
	private JPanel parameterPanel;
	private CyServiceRegistrarImpl register;
	private BundleContext bc;
	private JSlider HideSlider;
	private JTextField HideSliderField;
	JPanel buttonsPanel;

	public ControlPanel(BundleContext bc){
		this.bc=bc;
		register = new CyServiceRegistrarImpl(bc);
		setLayout(new GridBagLayout());
		register.getService(CyNetworkViewManager.class);
		this.netmgr = register.getService(CyNetworkManager.class);

		addInputPanel();
		
		addButtonPanel();

		NetworkModifiedListener nml = new NetworkModifiedListener();
		register.registerService(nml, NetworkAddedListener.class,new Properties());
		register.registerService(nml, NetworkDestroyedListener.class,new Properties());
		
	}

	
	public class weightDataAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser filechooser =new JFileChooser();
			int selected = filechooser.showOpenDialog(new JPanel());
			if (selected == JFileChooser.APPROVE_OPTION){
			      weightDataFile = filechooser.getSelectedFile();
			      weightDataField.setText(weightDataFile.getName());
			    }else if (selected == JFileChooser.CANCEL_OPTION){
			    	weightDataField.setText("canceled");
			    }else if (selected == JFileChooser.ERROR_OPTION){
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
			if(weightDataFile==null){
				return;
			}
			new WeightDataReader(network, weightDataFile);	
			BiomarkerFinderAlgorithm algorithm;
			if(algorithmComboBox.getSelectedItem().equals("Network Propagation")){
				algorithm = new NetworkPropagationAlgorithm(bc);	
			}
			else{
				return;
			}
			algorithm.setParameter((ParameterPanel) parameterPanel);
			algorithm.run(network);
//			DisplayResult dr0 = new DisplayResult(bc, network);
			changeOriginalView();
			result = algorithm.getResult();
			DisplayResult dr = new DisplayResult(bc, result);
			
			addHideSlider();
			addResetButton();
		}


	}

	public class NetworkModifiedListener implements NetworkAddedListener,NetworkDestroyedListener{

		@Override
		public void handleEvent(NetworkDestroyedEvent arg0) {
			networkComboBox.removeAllItems();
			for(CyNetwork net:netmgr.getNetworkSet()){
				networkComboBox.addItem(net);
			}			
		}

		@Override
		public void handleEvent(NetworkAddedEvent arg0) {
			networkComboBox.removeAllItems();
			for(CyNetwork net:netmgr.getNetworkSet()){
				networkComboBox.addItem(net);
			}			
		}
		
	}
		
	public class ChooseExpressionMatrixFileAction implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser filechooser =new JFileChooser();
			int selected = filechooser.showOpenDialog(new JPanel());
			if (selected == JFileChooser.APPROVE_OPTION){
			      expressionMatrixFile = filechooser.getSelectedFile();
			      expressionMatrixFileField.setText(expressionMatrixFile.getName());
			      updateSearchButton();
			    }else if (selected == JFileChooser.CANCEL_OPTION){
			    	expressionMatrixFileField.setText("canceled");
			    }else if (selected == JFileChooser.ERROR_OPTION){
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

	public class CloseAction implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e) {
//			CySwingApplication desktop = register.getService(CySwingApplication.class);
//			CytoPanel westPanel = desktop.getCytoPanel(CytoPanelName.WEST);
//			westPanel.setState(CytoPanelState.HIDE);
//			JOptionPane.showMessageDialog(null, "Close Button is hitted");
			panelClose();
		}
	}
	
	public class ChooseAlgorithmAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			String algorithmName = (String)algorithmComboBox.getSelectedItem();
			if(algorithmName.equals("Network Propagation")){
				parameterPanel = new NetworkPropagationParameterPanel();
				addParameterPanel(parameterPanel);
//				addHideSlider();
				return;
			}
			else{
				removeParameterPanel();
			}
		}

	}

	public class HideSliderAction implements ChangeListener{

		public void stateChanged(ChangeEvent e) {
			extractBiomarker();
		}

	}
	
	private void extractBiomarker(){
		CyApplicationManager appManager = register.getService(CyApplicationManager.class);
		CyNetworkView currentView = appManager.getCurrentNetworkView();
		if (currentView == null) return;
		    
		    // View is always associated with its model.
		    final CyNetwork ntwk = currentView.getModel();
	    	double threshold = ( getMaximum(ntwk.getDefaultNodeTable().getColumn("score"))*HideSlider.getValue() )/100;
		    for (CyNode node : ntwk.getNodeList()) {
		        if (ntwk.getDefaultNodeTable().getRow(node.getSUID()).get("score",Double.class) < threshold) {
		        	currentView.getNodeView(node).setVisualProperty(BasicVisualLexicon.NODE_VISIBLE, false);

		        }
		        else if(ntwk.getDefaultNodeTable().getRow(node.getSUID()).get("score",Double.class) >= threshold){
		        	currentView.getNodeView(node).setVisualProperty(BasicVisualLexicon.NODE_VISIBLE, true);
		        	Iterator it = ntwk.getAdjacentEdgeIterable(node, CyEdge.Type.ANY).iterator();
		        	while(it.hasNext()){
		        		currentView.getEdgeView((CyEdge) it.next()).setVisualProperty(BasicVisualLexicon.EDGE_VISIBLE, true);
		        	}		        	
		        }
		    }
		    HideSliderField.setText(HideSlider.getValue()+"% of max score");
		    currentView.updateView();
	}
	private void resetStart(){
		this.removeAll();
		this.repaint();
	}
	private void addInputPanel(){
		JLabel expressionMatrixFileLabel = new JLabel("expression matrix file:");
		expressionMatrixFileField = new JTextField("None Choosen");
		expressionMatrixFileField.setEditable(false);
		JButton expressionMatrixFileButton = new JButton("Choose...");
		expressionMatrixFileButton.addActionListener(new ChooseExpressionMatrixFileAction());
		
		JLabel networkLabel = new JLabel("Network:");
		networkComboBox = new JComboBox<CyNetwork>();
		for(CyNetwork net:netmgr.getNetworkSet()){
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
		algorithmComboBox.addItem("");
		algorithmComboBox.addItem("Network Propagation");
		algorithmComboBox.addActionListener(new ChooseAlgorithmAction());
	
		
		//input part panel
		{
			JPanel filePanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.FIRST_LINE_START;
			c.anchor = GridBagConstraints.FIRST_LINE_START;
			c.insets = new Insets(5,5,5,5);
			
			c.gridx = 0;		c.gridy = 0;
			c.gridwidth = 1;	c.gridheight = 1;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0.0;	c.weighty = 0.0;
			filePanel.add(expressionMatrixFileLabel, c);
	
			c.gridx = 1;		c.gridy = 0;
			c.gridwidth = 1;	c.gridheight = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;	c.weighty = 0.0;
			filePanel.add(expressionMatrixFileField, c);
	
			c.gridx = 2;		c.gridy = 0;
			c.gridwidth = 1;	c.gridheight = 1;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0.0;	c.weighty = 0.0;
			filePanel.add(expressionMatrixFileButton, c);
			
			c.gridx = 0;		c.gridy = 1;
			c.gridwidth = 1;	c.gridheight = 1;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0.0;	c.weighty = 0.0;
			filePanel.add(networkLabel, c);
	
			c.gridx = 1;		c.gridy = 1;
			c.gridwidth = 2;	c.gridheight = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;	c.weighty = 0.0;
			filePanel.add(networkComboBox, c);
			
			c.gridx = 0;		c.gridy = 2;
			c.gridwidth = 1;	c.gridheight = 1;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0.0;	c.weighty = 0.0;
			filePanel.add(weightLabel, c);
	
			c.gridx = 1;		c.gridy = 2;
			c.gridwidth = 1;	c.gridheight = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;	c.weighty = 0.0;
			filePanel.add(weightDataField, c);
	
			c.gridx = 2;		c.gridy = 2;
			c.gridwidth = 1;	c.gridheight = 1;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0.0;	c.weighty = 0.0;
			filePanel.add(weightDataButton, c);
			
			c.gridx = 0;		c.gridy = 3;
			c.gridwidth = 1;	c.gridheight = 1;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0.0;	c.weighty = 0.0;
			filePanel.add(algorithmLabel, c);
	
			c.gridx = 1;		c.gridy = 3;
			c.gridwidth = 2;	c.gridheight = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;	c.weighty = 0.0;
			filePanel.add(algorithmComboBox, c);
	
			c.gridx = 0;		c.gridy = 0;
			c.gridwidth = 1;	c.gridheight = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;	c.weighty = 0.0;
			add(filePanel, c);
		}
		
		JSeparator separator0 = new JSeparator();
		{
			GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(5,5,5,5);
			c.gridx = 0;		c.gridy = 1;
			c.gridwidth = 1;	c.gridheight = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;	c.weighty = 0.0;
			add(separator0, c);
		}
	
	}

	private void addButtonPanel(){
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
			c.insets = new Insets(5,5,5,5);
			c.anchor = GridBagConstraints.LAST_LINE_END;
			c.gridwidth = 1;	c.gridheight = 1;
			c.gridx = 0;		c.gridy = 6;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 1.0;	c.weighty = 1.0;
			add(buttonsPanel, c);
		}
	}

	private void addResetButton() {
		JButton resetButton = new JButton("Reset");
		resetButton.addActionListener(new ResetAction());
		buttonsPanel.add(resetButton);
	}

	private void panelClose(){
		this.removeAll();
		this.repaint();
		register.getService(CySwingApplication.class).getCytoPanel(CytoPanelName.WEST).setSelectedIndex(0);
	}
	private void removeParameterPanel() {
//		this.parameterPanel = new JPanel();
		if(parameterPanel==null){
			return;
		}
		this.remove(parameterPanel);
		this.repaint();
	}

	private void addParameterPanel(JPanel panel){
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = new Insets(5,5,5,5);
		c.gridwidth = 1;	c.gridheight = 1;
		c.gridx = 0;		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;	c.weighty = 0.0;
		add(panel, c);
		this.repaint();
	}
	

	private void addHideSlider() {
		JPanel HideSliderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		HideSliderPanel.setLayout(new GridBagLayout());
		
		JLabel HideSliderLabel = new JLabel("Hide Score Threshold");
		HideSliderField = new JTextField(18);
		HideSliderField.setEditable(false);
		HideSlider = new JSlider(0,100,0);
		
		HideSlider.addChangeListener(new HideSliderAction());
		HideSliderField.setText(HideSlider.getValue()+"% of max score");
		{
			GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(5,5,5,5);
			c.gridwidth = 1;	c.gridheight = 1;
			c.gridx = 0;		c.gridy = 0;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;	c.weighty = 0.0;
			
			HideSliderPanel.add(HideSliderLabel,c);
			
			c.gridwidth = 1;	c.gridheight = 1;
			c.gridx = 0;		c.gridy = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;	c.weighty = 0.0;
			
			HideSliderPanel.add(HideSliderField,c);
			
			c.gridwidth = 1;	c.gridheight = 1;
			c.gridx = 1;		c.gridy = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;	c.weighty = 0.0;
			
			HideSliderPanel.add(HideSlider,c);
		}
		
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5,5,5,5);
		c.gridwidth = 1;	c.gridheight = 1;
		c.gridx = 0;		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;	c.weighty = 0.0;
		
		parameterPanel.add(HideSliderPanel,c);
		parameterPanel.repaint();
		this.repaint();
	}
	
	private Double getMinimum(CyColumn c){
		Double x=Double.MAX_VALUE;
		List<Double> values = c.getValues(Double.class);
		Iterator<Double> it = values.iterator();
		while(it.hasNext()){
			Double y = it.next();
			if(y < x){
				x=y;
			}
		}
		return x;
	}
	
	private Double getMaximum(CyColumn c){
		double x=Double.MIN_VALUE;
		List<Double> values = c.getValues(Double.class);
		Iterator<Double> it = values.iterator();
		while(it.hasNext()){
			Double y = it.next();
			if(y > x){
				x=y;
			}
		}		
		return x;
	}

	private void updateSearchButton()
	{
		searchButton.setEnabled(false);
		if (expressionMatrixFile == null)
			searchButton.setToolTipText("An expression matrix file must be selected before searching.");
		else if (networkComboBox.getItemCount() == 0)
			searchButton.setToolTipText("There must be at least one network available before searching.");
		else
		{
			searchButton.setEnabled(true);
			searchButton.setToolTipText(null);
		}
	}
	
	private void changeOriginalView(){
		
		CyNetworkViewManager viewManager = register.getService(CyNetworkViewManager.class);
		CyApplicationManager appManager = register.getService(CyApplicationManager.class);
		Collection<CyNetworkView> views = viewManager.getNetworkViews(network);
		CyNetworkView originalView = views.iterator().next();
		VisualMappingFunctionFactory passthroughFactory = register.getService(VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
		
		PassthroughMapping pMapping = (PassthroughMapping) passthroughFactory.createVisualMappingFunction("name", String.class,BasicVisualLexicon.NODE_LABEL);
		
		VisualMappingFunctionFactory continuousFactory = register.getService(VisualMappingFunctionFactory.class, "(mapping.type=continuous)");
		VisualMappingManager vmManager = register.getService(VisualMappingManager.class);
		VisualStyleFactory vsFactory = register.getService(VisualStyleFactory.class);
		VisualStyle vs = vsFactory.createVisualStyle(network.getDefaultNetworkTable().getRow(network.getSUID()).get("name", String.class));

		ContinuousMapping mapping = (ContinuousMapping) continuousFactory.createVisualMappingFunction("score", Double.class, BasicVisualLexicon.NODE_FILL_COLOR);
		
		Double val1 = getMinimum(network.getDefaultNodeTable().getColumn("score"));
		BoundaryRangeValues<Paint> brv1 = new BoundaryRangeValues<Paint>(Color.GREEN,Color.GREEN,Color.GREEN);
		
		Double val3 = getMaximum(network.getDefaultNodeTable().getColumn("score"));
		BoundaryRangeValues<Paint> brv3 = new BoundaryRangeValues<Paint>(Color.RED,Color.RED,Color.RED);
		
		Double val2 = (val1+val3+val1+val1)/4;
		BoundaryRangeValues<Paint> brv2 = new BoundaryRangeValues<Paint>(Color.YELLOW,Color.YELLOW,Color.YELLOW);
		
		mapping.addPoint(val1, brv1);
		mapping.addPoint(val2, brv2);
		mapping.addPoint(val3, brv3);
		
		vs.addVisualMappingFunction(pMapping);
		vs.addVisualMappingFunction(mapping);
		vs.apply(originalView);
		
		vmManager.addVisualStyle(vs);
		originalView.updateView();
		
		appManager.setCurrentNetwork(network);
		vmManager.setVisualStyle(vs, originalView);
		appManager.setCurrentNetworkView(originalView);
	}
	public Component getComponent() {
		return this;
	}
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.WEST;
	}
	public String getTitle() {
		return "Biomarker";
	}
	public Icon getIcon() {
		return null;
	}
	
}