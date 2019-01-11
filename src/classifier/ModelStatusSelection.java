package classifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import utils.FileUtil;

public class ModelStatusSelection {
	int numOfPairs;
	int k; // Use for kNN to estimate posterior probabilities
	int R; //　the number of splits
	List<List<Double>> data = new ArrayList<List<Double>>(); // 0:x, 1:y, 2:true class, 3:estimated class
	List<List<Double>> classifier = new ArrayList<List<Double>>();
	static List<List<Double>> salientSamples = new ArrayList<List<Double>>();
	
	public ModelStatusSelection(List<List<Double>> data, List<List<Double>> classifier, int k, int numOfPairs, int R) {
		this.data = data;
		this.classifier = classifier;
		this.k = k;
		this.numOfPairs = numOfPairs;
		this.R = R;
	}
	
	public List<List<Double>> selectSalientSample (List<List<Double>> trainingData) {
		List<List<Double>> salientSamples = new ArrayList<List<Double>>();
		
		List<List<List<Double>>> samplesPairs = createSamplesPairs(trainingData);
		for(List<List<Double>>samplesPair: samplesPairs) {
			//Pick a pair of training samples
			List<Double> sampleX1 = samplesPair.get(0);
			List<Double> sampleX2 = samplesPair.get(1);
			// Find α ∈ [0,1] : g0(αx + (1 − α)x';Λ) =g1(αx + (1 − α)x'; Λ)
			double alpha = calculateByDichotomy(0.0, 1.0, 0.0001, sampleX1, sampleX2);
			List<Double> anchor = Arrays.asList(sampleX1.get(0)*alpha+sampleX2.get(0)*(1-alpha), 
												sampleX1.get(1)*alpha+sampleX2.get(1)*(1-alpha));
			
			// nearestSample = NN(αx + (1 − α)x',trainingData)
			kNearestNeighbor kNN = new kNearestNeighbor(/*k=*/ 1, anchor, trainingData);
			List<Double> nearestSample = kNN.findKNearestNeighbor().get(0);
			// If nearestSample ∉ S(Λ) then add to S(Λ)
			if(! salientSamples.contains(nearestSample)) {
				salientSamples.add(nearestSample);
			}
		}
		return salientSamples;
	}
	
	public List<List<List<Double>>> createSamplesPairs (List<List<Double>> trainingData){
		List<List<List<Double>>> pairs = new ArrayList<List<List<Double>>>();
		List<List<Double>> data = trainingData.stream().filter(i -> !(i.get(2).equals(i.get(3)))).collect(Collectors.toList());
		if(data.isEmpty())return pairs;
		List<List<Double>> class1 = trainingData.stream().filter(i -> i.get(2).equals(1.0)).collect(Collectors.toList());
		List<List<Double>> class2 = trainingData.stream().filter(i -> i.get(2).equals(2.0)).collect(Collectors.toList());
		List<Double> sampleX2 = new ArrayList<Double>();
		Random r = new Random();
		int cnt = 0;
		while(cnt < numOfPairs) {
			for(List<Double> sampleX1: data) {
				if(sampleX1.get(2).equals(1.0)) {
					sampleX2 = class2.get(r.nextInt(class2.size()));
				}else {
					sampleX2 = class1.get(r.nextInt(class1.size()));
				}
				pairs.add(Arrays.asList(sampleX1, sampleX2));
				cnt++;
			}	
		}
		return pairs;
	}
	public double evaluateUncertainlyMeasure(List<List<Double>> salientSamples, int minNumOfSamples, int maxNumOfSamples) {
		List<List<Double>> samples = salientSamples;
		List<Double> uncertainlyMeasures = new ArrayList<Double>();
		List<List<List<Double>>> clusters = new ArrayList<List<List<Double>>>();
		for(int r = 0; r < R; r++) {
			double sum = 0;
			Recursive2MeansCluster twoMeans = new Recursive2MeansCluster(maxNumOfSamples);
			clusters.clear();
			clusters.add(samples);
			clusters = twoMeans.run(clusters);
			//Throw away clusters that contain less than minNumOfSamples
			clusters = clusters.stream().filter(cluster -> cluster.size() > minNumOfSamples).collect(Collectors.toList());
			List<List<Double>> mergedClusters = new ArrayList<List<Double>>();
			clusters.stream().forEach(cluster -> mergedClusters.addAll(cluster));
			// Compute
			for(List<Double>sample: mergedClusters) {
				kNearestNeighbor kNN = new kNearestNeighbor(/*k=*/ k, sample, mergedClusters);
				List<List<Double>> nearestNeighbors = kNN.findKNearestNeighbor();
				int cnt = 0;
				/*If nearestNeighbor belongs to class j, count it. */
				for(List<Double> nearestNeighbor: nearestNeighbors) {
					if(nearestNeighbor.get(2).equals(sample.get(2))) {
						cnt++;
					}
				}
				if(cnt != 0) {
					double posteriorProbability = (double)cnt/nearestNeighbors.size();
					sum += (posteriorProbability * Math.log10(posteriorProbability));
				}				
			}
			if(!mergedClusters.isEmpty()) {
				uncertainlyMeasures.add(-sum/mergedClusters.size());
			}	
			// Write updated S(Λ)to text file 
			FileUtil fu = new FileUtil("");
			fu.writeSalientSamplesData(mergedClusters);
		}
		double uncertainlyMeasuresSum = 0;
		for (double x: uncertainlyMeasures) {
			uncertainlyMeasuresSum += x;
		}
		return uncertainlyMeasuresSum/R;
	}
	
	public double f(double alpha, List<Double> x1, List<Double> x2) {
		List<Double> middleSample = Arrays.asList(x1.get(0)*alpha+x2.get(0)*(1-alpha), x1.get(1)*alpha+x2.get(1)*(1-alpha));
        return discriminantFunction(0, classifier, middleSample)-discriminantFunction(1, classifier, middleSample);
    }

    double calculateByDichotomy(double a, double b, double eps, List<Double> x1, List<Double> x2) {
        int i=0;
        double alpha = 0;
 
        while(!(Math.abs(a-b)<eps)) {
            i++;
            alpha = (a+b)/2.0;
            if(f(alpha,x1,x2) * f(a,x1,x2)<0) {
            	b = alpha;
            }else {
            	a = alpha;
            }
            if(i==1000) break;
        }
        return alpha; 
    }
    
    public static double discriminantFunction(int classIndex, List<List<Double>>classifier, List<Double> sample) {
    	double confidence = 0.0;
    	for(int k=classIndex*(classifier.size()/2); k<(classIndex+1)*(classifier.size()/2); k++) {
    		for(int vec=0; vec<2; vec++){
    			confidence += Math.abs(Math.pow(sample.get(vec)-classifier.get(k).get(vec), 2));
    		}
		}
    	return confidence;
    }
}