package org.cytoscape.biomarkerfinder.internal.ui;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.cytoscape.biomarkerfinder.ParameterPanel;


@SuppressWarnings("serial")
public class NetworkPropagationParameterPanel extends JPanel implements ParameterPanel {
	
	// Algorithm name & ID
	static final String ALGORITHM_ID = "network-propagation";
	static final String ALGORITHM_NAME = "Network Propagation";
	
	private JSpinner alphaSpinner;
	private JSpinner thresholdSpinner;
	private JSpinner seedNumSpinner;
	private JSpinner moduleSizeSpinner;
	
	public NetworkPropagationParameterPanel(){
		setName("ParameterPanel");
		setLayout(new GridBagLayout());
		this.setBorder(BorderFactory.createTitledBorder("NetworkPropagation parameters"));
		
			alphaSpinner = newDoubleSpinner(0.80,0.01);
			thresholdSpinner = newDoubleSpinner(0.05,0.001);
			seedNumSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 10000, 1));
			moduleSizeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 500, 1));
			
			{
				JPanel alphaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
				alphaPanel.add(new JLabel("alpha:"));
				alphaPanel.add(alphaSpinner);
				
				GridBagConstraints c = new GridBagConstraints();
				c.insets = new Insets(5,5,5,5);
				c.gridwidth = 1;	c.gridheight = 1;
				c.gridx = 0;		c.gridy = 0;
				c.fill = GridBagConstraints.HORIZONTAL;
				c.weightx = 1.0;	c.weighty = 0.0;

				this.add(alphaPanel, c);
			}

			{
				JPanel thresholdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
				thresholdPanel.add(new JLabel("threshold:"));
				thresholdPanel.add(thresholdSpinner);
				
				GridBagConstraints c = new GridBagConstraints();
				c.insets = new Insets(5,5,5,5);
				c.gridwidth = 1;	c.gridheight = 1;
				c.gridx = 0;		c.gridy = 1;
				c.fill = GridBagConstraints.HORIZONTAL;
				c.weightx = 1.0;	c.weighty = 0.0;
				
				this.add(thresholdPanel, c);				
			}
			
			{
				JPanel seedNumPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
				seedNumPanel.add(new JLabel("The number of seeds:"));
				seedNumPanel.add(seedNumSpinner);
				
				GridBagConstraints c = new GridBagConstraints();
				c.insets = new Insets(5,5,5,5);
				c.gridwidth = 1;	c.gridheight = 1;
				c.gridx = 0;		c.gridy = 2;
				c.fill = GridBagConstraints.HORIZONTAL;
				c.weightx = 1.0;	c.weighty = 0.0;
				
				this.add(seedNumPanel, c);	
				
			}
			
			{
				JPanel moduleSizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
				moduleSizePanel.add(new JLabel("The size of modules:"));
				moduleSizePanel.add(moduleSizeSpinner);
				
				GridBagConstraints c = new GridBagConstraints();
				c.insets = new Insets(5,5,5,5);
				c.gridwidth = 1;	c.gridheight = 1;
				c.gridx = 0;		c.gridy = 3;
				c.fill = GridBagConstraints.HORIZONTAL;
				c.weightx = 1.0;	c.weighty = 0.0;
				
				this.add(moduleSizePanel, c);	
				
			}
			
	}

	public HashMap<String,Object> getParameters(){
		HashMap<String, Object> x = new HashMap<String, Object>();
		x.put("alpha", alphaSpinner.getValue());
		x.put("threshold", thresholdSpinner.getValue());
		x.put("seedNum", seedNumSpinner.getValue());
		x.put("moduleSize", moduleSizeSpinner.getValue());
		return x;
	}
	
	private JSpinner newDoubleSpinner(double defaultValue,double step)
	{
		return newDoubleSpinner(defaultValue, Double.MIN_VALUE,Double.MAX_VALUE,step);
	}

	private JSpinner newDoubleSpinner(double defaultValue, double minValue,double maxValue,double step)
	{
		return newNumberSpinner(new SpinnerNumberModel(defaultValue, minValue, maxValue, step));
	}

	private JSpinner newNumberSpinner(SpinnerNumberModel numberModel)
	{
		JSpinner spinner = new JSpinner();
		spinner.setModel(numberModel);
		new JSpinner.NumberEditor(spinner);
		((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(7);
		((JSpinner.NumberEditor) spinner.getEditor()).getFormat().setMaximumFractionDigits(10);
		((JSpinner.NumberEditor) spinner.getEditor()).getFormat().setMinimumFractionDigits(4);
		return spinner;
	}

	@Override
	public String getAlgorithmID() {
		return ALGORITHM_ID;
	}

}
