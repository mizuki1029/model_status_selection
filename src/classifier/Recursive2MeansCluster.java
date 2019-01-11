package classifier;

import java.util.*;

public class Recursive2MeansCluster {
	final static int tMax = 1000;
	final static double threshold = 0.0001;// 閾値
	final static int K = 2;
	int maxNumOfSamples;
	static double variation = 1;// 変化量
	static double strainScale;// 歪尺度
	static double previousStrainScale;// 歪尺度ストア
	
	static List<List<Double>> meanVectors = new ArrayList<List<Double>>();
	
	public Recursive2MeansCluster(int maxNumOfSamples) {
		this.maxNumOfSamples = maxNumOfSamples;
	}
	
	public List<List<List<Double>>> run(List<List<List<Double>>> oldClusters) {
		List<List<List<Double>>> clusters = new ArrayList<List<List<Double>>>();
		clusters.clear();
		/* 1. 訓練用標本の初期的分割 */
		for(List<List<Double>> oldCluster: oldClusters) {
			int middle = oldCluster.size()/2;
			clusters.add(oldCluster.subList(0, middle));
			clusters.add(oldCluster.subList(middle, oldCluster.size()));
		}
		/* 2. 初期クラスタ内の平均ベクトルの計算 */
		for (int k = 0; k < clusters.size(); k++) {// クラスタNo
			meanVectors.add(getMeanVec(clusters.get(k)));
		}
		/* 3. 歪尺度の計算 */
		/* 4. 再分割 */
		strainScale = Integer.MAX_VALUE;
		for (int t = 0; t < tMax; t++) {
			/*3、4、6*/
			previousStrainScale = strainScale;
			strainScale = reClustering(meanVectors, clusters);
				
			/*5. 平均ベクトルの更新*/
			for (int k = 0; k < clusters.size(); k++) {// クラスタNo
				if(clusters.get(k).size()!=0) {
					meanVectors.set(k,getMeanVec((clusters.get(k))));
				}
				/*7. 歪の変化量の計算*/
				variation = calculateVariation(previousStrainScale, strainScale);
				/*8. 終了判定*/
				if (variation <= threshold) {
					break;
				}
			}
		}
		int flag = 0;
		for(List<List<Double>> cluster: clusters) {
			if(cluster.size() < maxNumOfSamples) {
				flag = 1;
				break;
			}
		}
		if(flag == 0) {
			return run(clusters);
		}
		return clusters;
	}

	public static List<Double> getMeanVec(List<List<Double>> cluster) {
		List<Double> sumVectors = new ArrayList<Double>();
		for (int vec = 0; vec < 3; vec++) {
			Double sum = 0.0;
			for (List<Double> data: cluster) {
				sum += data.get(vec);
			}
			sumVectors.add(sum / cluster.size());
		}
		return sumVectors;
	}

	/*標本群の再分割をし，歪尺度を返す*/
	public static double reClustering(List<List<Double>> meanVector, List<List<List<Double>>> clusters) {
		double strainScale = 0;
		List<List<List<Double>>> newClusters = new ArrayList<List<List<Double>>>(clusters.size());
		
		List<Double> list1 = Arrays.asList(0.0, 0.0);
		List<List<Double>> list2 = Arrays.asList(list1);
		for(int i=0; i<clusters.size(); i++) {
			newClusters.add(list2);
		}
		for (int k1 = 0; k1 < clusters.size(); k1++) {
			for (int n = 0; n<clusters.get(k1).size(); n++) {//クラスタk1の中の各標本に対して
				double minEuclideanDistance = Double.MAX_VALUE;
				/*全クラスタの平均ベクトルとの距離を計算し，最も近い平均ベクトルに対応するクラスタに再配属する*/
				int nextCluster = 0;
				for (int k = 0; k < clusters.size(); k++) {
					/*クラスタkの平均ベクトルとの二乗ユークリッド距離*/
					double euclideanDistance = 0;
					for (int vec = 0; vec < 2; vec++) {
						euclideanDistance += Math.pow((clusters.get(k1).get(n).get(vec) - meanVector.get(k).get(vec)), 2);
					}
					/*最も近い平均ベクトルとの距離，それが属するクラスタのindexを保持*/
					if (minEuclideanDistance > euclideanDistance) {
						minEuclideanDistance = euclideanDistance;
						nextCluster = k;
					}
				}
				List<Double> movedSample = new ArrayList<Double>();
				movedSample = clusters.get(k1).get(n);
				if(k1 != nextCluster) {//違うクラスに追加
					if(newClusters.get(nextCluster).get(0).get(0).equals(0.0)) {
						newClusters.set(nextCluster, Arrays.asList(movedSample));
					}else {
						List<List<Double>> newSamples = new ArrayList<List<Double>>();
						newSamples.addAll(newClusters.get(nextCluster));
						newSamples.add(movedSample);
						newClusters.set(nextCluster, newSamples);
					}
					
				}else { //同じクラスに追加
					if(newClusters.get(k1).get(0).get(0).equals(0.0)) {
						newClusters.set(k1, Arrays.asList(movedSample));
					}else {
						List<List<Double>> newSamples = new ArrayList<List<Double>>();
						newSamples.addAll(newClusters.get(k1));
						newSamples.add(movedSample);
						newClusters.set(k1, newSamples);
					}
				}
				strainScale += minEuclideanDistance;
			}
		}
		clusters = newClusters;
		return strainScale;
	}

	public static double calculateVariation(double previous, double current) {
		double R = 0;
		R = Math.pow((previous - current), 2) / (previous * current);
		return R;
	}
}

