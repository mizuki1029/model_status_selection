package classifier;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class kNearestNeighbor {
	int K;
	List<Double>target = new ArrayList<Double>();
	List<List<Double>> samples = new ArrayList<List<Double>>();
	
	public kNearestNeighbor(int k, List<Double>target, List<List<Double>> samples) {
		this.K = k;
		this.target = target;
		this.samples = samples;
	}
	
	public List<List<Double>> findKNearestNeighbor() {
		List<List<Double>> nearestNeighbors = new ArrayList<List<Double>>();
		TreeMap<Double,Integer>nnIndexes = new TreeMap<Double,Integer>();
		for(int i=0; i<samples.size(); i++) {
			double distance = Math.sqrt(Math.pow(samples.get(i).get(0)-target.get(0), 2) + Math.pow(samples.get(i).get(1)-target.get(1), 2));
			if(distance != 0) {
				nnIndexes.put(distance, i);
			}
		}
		if(nnIndexes.size()<K) {
			return samples;
		}else {
			for(int i=0; i<K; i++) {
				int n = nnIndexes.values().stream().findFirst().get();
				nearestNeighbors.add(samples.get(n));
				nnIndexes.remove(nnIndexes.keySet().stream().findFirst().get());
			}
			return nearestNeighbors;
		}	
	}
}