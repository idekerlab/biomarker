package org.cytoscape.Biomarker.internal;

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

import org.cytoscape.Biomarker.ParameterPanel;


@SuppressWarnings("serial")
public class NetworkPropagationParameterPanel extends JPanel implements ParameterPanel{
	private JSpinner alphaSpinner;
	private JSpinner thresholdSpinner;
	
	public NetworkPropagationParameterPanel(){
		setName("ParameterPanel");
		setLayout(new GridBagLayout());
		this.setBorder(BorderFactory.createTitledBorder("parameter Panel"));
		
			alphaSpinner = newDoubleSpinner(0.80,0.01);
			thresholdSpinner = newDoubleSpinner(1e-6,1e-6);
			
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
	}

	public HashMap<String,Object> getParameters(){
		HashMap<String, Object> x = new HashMap<String, Object>();
		x.put("alpha", alphaSpinner.getValue());
		x.put("threshold", thresholdSpinner.getValue());
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
		((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(10);
		((JSpinner.NumberEditor) spinner.getEditor()).getFormat().setMaximumFractionDigits(10);
		return spinner;
	}

}
