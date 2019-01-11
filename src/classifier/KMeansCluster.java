package classifier;

import java.util.*;

public class KMeansCluster {
	final static int tMax = 1000;
	
	int K;
	int Nj;
	double threshold;
	int numOfClass;
	static double variation = 1;// 変化量
	static double strainScale;// 歪尺度
	static double previousStrainScale;// 歪尺度ストア
	
	static List<List<List<List<Double>>>> Cj = new ArrayList<List<List<List<Double>>>>();
	static List<List<List<Double>>> meanVectors = new ArrayList<List<List<Double>>>();
	List<List<Double>> data = new ArrayList<List<Double>>();//[400][3]
	static List<List<Double>> classifier = new ArrayList<List<Double>>();//[2*K][3]
	
	public KMeansCluster(List<List<Double>> data, int K, double threshold, int numOfClass) {
		this.data = data;
		this.K = K;
		this.threshold = threshold;
		this.Nj = data.size()/numOfClass;
		this.numOfClass = numOfClass;
	}
	
	public void run() {
		Cj.clear();
		meanVectors.clear();
		classifier.clear();
		/* 1. 訓練用標本の初期的分割 */
		for (int j = 0; j < numOfClass; j++) {
			List<List<List<Double>>> clusters = new ArrayList<List<List<Double>>>();
			for (int k = 0; k < K; k++) {// クラスタNo
				List<List<Double>> cluster = new ArrayList<List<Double>>();
				for (int i = 0; i < Nj / K; i++) {// クラスタ内index
					cluster.add(data.get(i + Nj / K * k + Nj*j));
				}
				clusters.add(cluster);
			}
			Cj.add(clusters);
		}

		/* 2. 初期クラスタ内の平均ベクトルの計算 */
		for(int j=0; j<numOfClass; j++) {
			List<List<Double>> MV = new ArrayList<List<Double>>();
			for (int k = 0; k < K; k++) {// クラスタNo
				MV.add(getMeanVec((Cj.get(j).get(k))));
			}
			meanVectors.add(MV);			
		}
		
		/*初期化に用いられなかった標本を復活させる*/		
		int remainder = Nj%K;
		int last = Nj - remainder;
		if(remainder != 0) {
			for (int j = 0; j < numOfClass; j++) {
				for (int i = 0; i < remainder; i++) {// 余った標本
					Cj.get(j).get(0).add(data.get(last+i+ Nj*j));
				}
			}
		}
		
		/* 3. 歪尺度の計算 */
		/* 4. 再分割 */
		for (int j = 0; j < numOfClass; j++) {
			strainScale = Integer.MAX_VALUE;
			for (int t = 0; t < tMax; t++) {
				/*3、4、6*/
				previousStrainScale = strainScale;
				strainScale = reClustering(K, meanVectors.get(j), Cj.get(j));
				
				/*5. 平均ベクトルの更新*/
				for (int k = 0; k < K; k++) {// クラスタNo
					if(Cj.get(j).get(k).size()!=0) {
						meanVectors.get(j).set(k,getMeanVec((Cj.get(j).get(k))));
					}
				}
				/*7. 歪の変化量の計算*/
				variation = calculateVariation(previousStrainScale, strainScale);
				/*8. 終了判定*/
				if (variation <= threshold) {
					break;
				}
			}
			
		}
		
		/*分類器をまとめる*/
		for(int j=0; j<numOfClass; j++) {
			for(int k=0; k<K; k++) {
				classifier.add(meanVectors.get(j).get(k));
			}
		}		
	}
	
	public List<List<Double>> getClassifier() {
		return classifier;
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
	public static double reClustering(int K, List<List<Double>> meanVector, List<List<List<Double>>> list2) {
		double strainScale = 0;
			
		for (int k1 = 0; k1 < list2.size(); k1++) {
			for (int n = 0; n < list2.get(k1).size(); n++) {//クラスタk1の中の各標本に対して
				double minEuclideanDistance = Double.MAX_VALUE;
				
				/*全クラスタの平均ベクトルとの距離を計算し，最も近い平均ベクトルに対応するクラスタに再配属する*/
				int nextCluster = 0;
				for (int k = 0; k < K; k++) {
					/*クラスタkの平均ベクトルとの二乗ユークリッド距離*/
					double euclideanDistance = 0;
					for (int vec = 0; vec < 2; vec++) {
						euclideanDistance += Math.pow((list2.get(k1).get(n).get(vec) - meanVector.get(k).get(vec)), 2);
					}
					/*最も近い平均ベクトルとの距離，それが属するクラスタのindexを保持*/
					if (minEuclideanDistance > euclideanDistance) {
						minEuclideanDistance = euclideanDistance;
						nextCluster = k;
					}
				}
				list2.get(nextCluster).add(list2.get(k1).get(n));// 新しいクラスタに追加
				list2.get(k1).remove(n);// 元のクラスタから削除
				strainScale += minEuclideanDistance;
			}
		}
		return strainScale;
	}

	public static double calculateVariation(double previous, double current) {
		double R = 0;
		R = Math.pow((previous - current), 2) / (previous * current);
		return R;
	}
}

