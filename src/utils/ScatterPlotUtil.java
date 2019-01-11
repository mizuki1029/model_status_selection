package utils;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.JFrame;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

public class ScatterPlotUtil extends JFrame{
	private static final long serialVersionUID = 1L;
	
	List<List<Double>> salientSamples = new ArrayList<List<Double>>();
	List<List<Double>> samples = new ArrayList<List<Double>>();
	
	public ScatterPlotUtil(List<List<Double>> salientSamples, List<List<Double>> samples){
		this.salientSamples = salientSamples;
		this.samples = samples;
		
		JFreeChart chart = ChartFactory.createScatterPlot("GMM_2class_400", "", "", createData(),
				PlotOrientation.VERTICAL, true, false, false);

		ChartPanel cpanel = new ChartPanel(chart);
		getContentPane().add(cpanel, BorderLayout.CENTER);
	}

	private XYSeriesCollection createData(){
		XYSeriesCollection data = new XYSeriesCollection();
		
		XYSeries series0 = new XYSeries("class0");
		XYSeries series1 = new XYSeries("class1");
		XYSeries series2 = new XYSeries("S(Λ)");
		XYSeries series3 = new XYSeries("updated S(Λ)");
		
		FileUtil fu = new FileUtil("salientSamples.txt");
		List<List<Double>> updatedSalientSamples = fu.readFile();
		samples.stream().filter(i -> i.get(2)==1.0).forEach(i -> series0.add(i.get(0), i.get(1)));
		samples.stream().filter(i -> i.get(2)==2.0).forEach(i -> series1.add(i.get(0), i.get(1)));
		salientSamples.stream().forEach(i -> series2.add(i.get(0), i.get(1)));
		updatedSalientSamples.stream().forEach(i -> series3.add(i.get(0), i.get(1)));

		//data.addSeries(series3); //間引いた後の重要な標本S(Λ)を表示できる
		data.addSeries(series2);
		data.addSeries(series0);
		data.addSeries(series1);
		
		return data;
	}
}