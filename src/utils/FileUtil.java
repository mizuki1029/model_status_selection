package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 
 * データファイルを読み込み，2次元リストに変換して返すためのクラス
 *
 */
public class FileUtil {
	private static String fileName;
	public FileUtil(String fileName) {
		FileUtil.fileName = fileName;
	}
	public List<List<Double>> readFile() {
		List<List<Double>> data = new ArrayList<List<Double>>();
		try {
			File file = new File(fileName);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String str;
			while ((str = bufferedReader.readLine()) != null) {
				List<Double> elements = Arrays.stream(str.split(" ")).map(Double::valueOf).collect(Collectors.toList());
                data.add(elements);
			}
			fileReader.close();
			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}
	
	public void writeErrorRate(List<Double> errorRate) {
		try {
			File file = new File("ErrorRate.txt");
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			for(double rate: errorRate) {
				bw.write(rate+" ");
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeUncertainlyMeasure(List<Double> uncertainlyMeasure) {
		try {
			File file = new File("uncertainlyMeasure.txt");
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			for(double measure: uncertainlyMeasure) {
				bw.write(measure+" ");
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeSalientSamplesData(List<List<Double>> salientSamples) {
		try {
			File file = new File("salientSamples.txt");
			/* exception when the file not exit */
			if (!file.exists()) {
				System.out.print("File Not Exit");
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			for(List<Double>sample: salientSamples) {
				sample.stream().forEach(i -> {
					try {
						bw.write(i.toString()+" ");
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}