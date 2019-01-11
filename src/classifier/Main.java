package classifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import utils.FileUtil;
import utils.ScatterPlotUtil;

public class Main {
	private final static String FILE_NAME = "GMM_2class_400.dat"; //file name
	private final static int MIN_NUM_OF_SAMPLES = 2; // Nm
	private final static int MAX_NUM_OF_SAMPLES = 4; // NM
	private final static int UNCERTAINLY_MEASURE_DEFAULT = 0;
	private final static int NUM_OF_PAIRS = 1000;
	private final static int K = 6; // Use for kNN to estimate posterior probabilities
	private final static int R = 10; // the number of splits
	
    public static void main(String args[]) {
    	
    	/*ファイルを読み込み，リストに変換*/
    	FileUtil fileUtil = new FileUtil(FILE_NAME);
    	List<List<Double>> data = fileUtil.readFile();
    	
    	/*パラメータ候補を作成する*/
    	Map<Integer, List<Double>> candidateStatuses = new LinkedHashMap<>();
    	candidateStatuses.put(1, Arrays.asList(0.0001));
    	for(int k=1; k<=150; k++) {
    		if(k%10==0) {
    			candidateStatuses.put(k, Arrays.asList(0.0001));
    		}
    	}
    	int bestK = 1;
    	double bestThreshold = 0.0001;
    	double bestUncertainlyMeasure = 0;	
    	double uncertainlyMeasure = 0;
    	/*すべてのパラメータ候補に対して*/
    	for(int k : candidateStatuses.keySet()){
    		for(double threshold: candidateStatuses.get(k)) {
    			/*K-平均法を用いた分類*/
    			List<List<Double>> newData = data.subList(0, 320);
    			KMeansCluster kmeans = new KMeansCluster(newData, /*k=*/k, /*threshold=*/threshold, /*num of class=*/2);
    			kmeans.run();	
    			List<List<Double>> classifier = kmeans.getClassifier();
    			List<List<Double>> classifiedData = classifier(data, classifier);
    			
    			/*モデル状態選択*/
    			ModelStatusSelection modelStatusSelection = new ModelStatusSelection(classifiedData, classifier, K, NUM_OF_PAIRS, R);
    			List<List<Double>> salientSamples = modelStatusSelection.selectSalientSample(classifiedData); // 重要な標本S(Λ)

    			/*U(Λ)の計算*/
    			if(!salientSamples.isEmpty()) {
    				uncertainlyMeasure = modelStatusSelection.evaluateUncertainlyMeasure(salientSamples, MIN_NUM_OF_SAMPLES, MAX_NUM_OF_SAMPLES);
    			}else {
    				uncertainlyMeasure = UNCERTAINLY_MEASURE_DEFAULT;
    			}
    			
    			/*U(Λ)の値により，最適なパラメータを更新する*/
    			if(bestUncertainlyMeasure < uncertainlyMeasure) {
    				bestK = k;
    				bestUncertainlyMeasure = uncertainlyMeasure;
    				bestThreshold = threshold;
    			}
    			System.out.println("k="+k+", threshold="+threshold+", bestK="+bestK+", U(Λ)="+uncertainlyMeasure);
    		}
    	};  		
    	
    	System.out.println("best parameter: K = " + bestK + ", threshold = "+bestThreshold);
    	System.out.println("max U(Λ) = " + bestUncertainlyMeasure);
    	
   	/*一番良かったパラメータを使ってK-平均法で分類を行い，重要な標本S(Λ)を作成する*/
    	KMeansCluster kmeans = new KMeansCluster(data, /*k=*/320, bestThreshold, /*num of class=*/2);
    	kmeans.run();	
    	List<List<Double>> classifier = kmeans.getClassifier();
    	List<List<Double>> classifiedData = classifier(data, classifier);  
		ModelStatusSelection modelStatusSelection = new ModelStatusSelection(classifiedData, classifier, K, NUM_OF_PAIRS, R);
		List<List<Double>> salientSamples = modelStatusSelection.selectSalientSample(classifiedData); // 重要な標本S(Λ)
		/*U(Λ)の計算*/
		if(!salientSamples.isEmpty()) {
			uncertainlyMeasure = modelStatusSelection.evaluateUncertainlyMeasure(salientSamples, MIN_NUM_OF_SAMPLES, MAX_NUM_OF_SAMPLES);
		}else {
			uncertainlyMeasure = UNCERTAINLY_MEASURE_DEFAULT;
		}
		/*データの描画*/
		ScatterPlotUtil frame = new ScatterPlotUtil(salientSamples, classifiedData);
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    frame.setBounds(10, 10, 1500, 750);
	    frame.setTitle("特別演習実習");
	    frame.setVisible(true);
    }
    
    /*与えられたデータに，推定されたクラスの情報を追加して返す*/
    public static List<List<Double>> classifier(List<List<Double>> data, List<List<Double>> classifier) {
    	List<List<Double>> classifiedData = new ArrayList<List<Double>>();
    	for(int i=0; i<data.size(); i++) {
    		List<Double> classifiedSample = data.get(i).subList(0, 3);// 0:x, 1:y, 2:true class, 3:estimated class
    		classifiedSample.add(estimateClass(classifier, data.get(i)).get(2));
    		classifiedData.add(classifiedSample);
    	}
    	return classifiedData;
    }
    
    /*識別関数でクラスを推定する*/
	public static List<Double> estimateClass(List<List<Double>> classifier, List<Double> list){
		double minGjk = 100000;
		int minIndex = 0;
		for(int t=0; t<classifier.size(); t++){
			double Gjk = 0;
			for(int vec=0; vec<2; vec++){
				Gjk += Math.abs(Math.pow(list.get(vec)-classifier.get(t).get(vec), 2));
			}
			if(Gjk<minGjk){
				minGjk=Gjk;
				minIndex = t;
			}	
		}
		return classifier.get(minIndex);
	}
}
