package sde.project;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class WeightedPageRankDemo {

	private static int filesNeedTOProccessed;

	private static int totalNumberOfFiles;

	private static float[] aggregatedWeightedPageRank = null;
	
	private static float DAMPING_FACTOR	= 0.85F;
	
	private static float EQUAL_DISTRIBUTION_FACTOR = 0.15F;
	
	private static double ALLOWED_EDGE_WEIGHT_OR_TOLERANCE = 1.0E-7D;

	private static float getVal(String fileName) {
		/*
		 * We organized an online meeting via Skype,4 and the five experts discussed
		 * the relative importance of every pair of criteria (i.e., “INR”,
		 * “IMR”, “PAR”, “GVR”, “MCR”, “LVR”, and “RTR”). After
		 * consultation by the five experts, a final agreement was
		 * reached. Table 4 shows the final result of the pairwise comparisons. Then, W can be obtained via Formula (6) as
		 * W = (0.034, 0.290, 0.034; 0:034; 0:178; 0:394; 0:034Þ, which
		 * indicates the relative weights for the seven criteria MCR,
		 * INR, PAR, RTR, GVR, IMR, and LVR are 0.034, 0.290, 0.034,
		 * 0.034, 0.178, 0.394, and 0.034, respectively
		 * */
		fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
		float res = 0.0F;
		
		if(fileName.startsWith("1CALLS")) {
			res = 0.034F;
		}else if(fileName.startsWith("1EXTENDS")) {
			res = 0.29F;
		}else if(fileName.startsWith("1FUNC_PARAM")) {
			res = 0.034F;
		}else if(fileName.startsWith("1FUNC_RET")) {
			res = 0.034F;
		}else if(fileName.startsWith("1GLOBAL_VAR")) {
			res = 0.178F;
		}else if(fileName.startsWith("1IMPLEMENTS")) {
			res = 0.394F;
		}else if(fileName.startsWith("1LOCAL_VAR")) {
			res = 0.034F;
		}
		return res;
	}
	
	private static String getRelationType(String fileName) {
		fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
		String res = "";
		
		if(fileName.startsWith("1CALLS")) {
			res =  "MCR";
		}else if(fileName.startsWith("1EXTENDS")) {
			res = "INR";
		}else if(fileName.startsWith("1FUNC_PARAM")) {
			res = "PAR";
		}else if(fileName.startsWith("1FUNC_RET")) {
			res = "RTR";
		}else if(fileName.startsWith("1GLOBAL_VAR")) {
			res = "GVR";
		}else if(fileName.startsWith("1IMPLEMENTS")) {
			res = "IMR";
		}else if(fileName.startsWith("1LOCAL_VAR")) {
			res = "LVR";
		}
		return res;
	}

	private static void backupPreviousRanks(float[] edgePageRank, float[] backup, int totalNodeCount) {
		for (int i= 0; i < totalNodeCount; i++)
			backup[i] = edgePageRank[i];
	}
	
	private static void readNodes(BufferedReader bufferedReader, String[] nodes) throws IOException {
		boolean isWorkDone = false;
		String line;
		int numberOfNodes=0;
		System.out.println("Reading the nodes.");
		do {
			//Exit once reading of edges are completed
			if ((line = bufferedReader.readLine()).equals("*Arcs")) {
				isWorkDone = true;
			}else if(line.equals("*Edges")){
				System.out.println("Exiting.................");
				System.exit(0);
			}else {
				String fqdnClassName = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
				nodes[numberOfNodes++] = fqdnClassName;
			}
			//if(!isWorkDone)System.out.println("Reading the next "+numberOfNodes+" node.");
		}while (!isWorkDone);
		System.out.println("Reading the nodes completed.");
	}
	
	private static void readEdges(BufferedReader bufferedReader , float[][] adjacencyGraph) throws IOException {
		boolean isWorkDone=false;
		String line;
		System.out.println("Reading the edges.");
		int numberOfEdges=0;
		do {
			if ((line = bufferedReader.readLine()) == null) {
				isWorkDone=true;
			}else {
				String[] edgeProperties = line.split(" "); //Ex: 1175 2884 1
				if (edgeProperties.length<3){
					System.out.println("Edge weight is missing........");
					System.exit(0);
				}else if (3 == edgeProperties.length) {
					adjacencyGraph[Integer.parseInt(edgeProperties[0]) - 1][Integer.parseInt(edgeProperties[1]) - 1] = Float.parseFloat(edgeProperties[2]);
				}
			}
			//if(!isWorkDone)System.out.println("Reading the next "+numberOfEdges++ +" edge.");
		}while (!isWorkDone);
		System.out.println("Reading the edges completed.");
	}
	
	
	private static void computeInitialRanks(float[] edgePageRank, int totalNodeCount) {
		/* Computing the initial page rank 1/total node count.
		 * lines 2
		 * for j = 0 to N do
		 * 	  PRw(pj) = bakPRw(pj) = 1/N
		 * */
		for (int i = 0; i < totalNodeCount; i++)
			edgePageRank[i] = 1.0F / totalNodeCount;
	}
	
	
	private static void computeElementRank(String fileName, String outputFileName) throws IOException {
		BufferedWriter bufferedWriter = null;
		boolean bool = true;
		float f1 = 0.0F;
		int m = 1;
		String str2 = fileName;
		File file = new File(str2);
		FileReader fileReader = new FileReader(file);
		int totalNodeCount;
		String str1;
		
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		
		//reading the total node count from dataset file. reading the first line of data file.
		str1 = bufferedReader.readLine();
		totalNodeCount = Integer.parseInt(((str1).split(" "))[1]);
		
		/*
		 * 2884 1168 1
		 * 1169 2884 
		 * 2884 1169 8.0
		 * */
		float[][] adjacencyGraph = new float[totalNodeCount][totalNodeCount]; //graph containes weight of each edge.
		
		float[] nodeWeights1 = new float[totalNodeCount];
		float[] nodeWeights2 = new float[totalNodeCount];
		float aggregatedNodeWeightOrTotalSumWeightInLinks = 0.0F; //sumwInLinks
		float[] previousRanks = new float[totalNodeCount];
		float[] pageRank = new float[totalNodeCount];
		float[] edgePageRank = new float[totalNodeCount];
		if (totalNumberOfFiles == filesNeedTOProccessed)
			aggregatedWeightedPageRank = new float[totalNodeCount]; 
		float f3 = 0.0F;
		int n=0;
		boolean isWorkDone = false;
		String[] nodes = new String[totalNodeCount];
		
		//readig the graph
		System.out.println("Reading the graph for file "+str2);
		readNodes(bufferedReader,nodes);
		
		System.out.println("Reading the edges and weights.");
		readEdges(bufferedReader, adjacencyGraph);
		bufferedReader.close();
		fileReader.close();
		
		computeInitialRanks(edgePageRank, totalNodeCount);
		
		/* Computing sum of weight In links.
		 * lines 3
		 * for j = 0 to N do
		 * 	  sumwInLinks += wInLinks(pj)
		 * */
		for (int i = 0; i < totalNodeCount; i++) {
			System.out.println("computing sumInLinksWeight, iteration "+i);
			try {Thread.sleep(100);} catch (Exception e) {}
			float node1InLinksWeight = 0.0F;
			float node2InLinksWeight = 0.0F;
			for (int j = 0; j < totalNodeCount; j++) {
				if (Math.abs(adjacencyGraph[i][j]) >= ALLOWED_EDGE_WEIGHT_OR_TOLERANCE)
					node1InLinksWeight += adjacencyGraph[i][j]; 
				if (Math.abs(adjacencyGraph[j][i]) >= ALLOWED_EDGE_WEIGHT_OR_TOLERANCE)
					node2InLinksWeight += adjacencyGraph[j][i]; 
			} 
			nodeWeights1[i] = node1InLinksWeight;
			nodeWeights2[i] = node2InLinksWeight;
			aggregatedNodeWeightOrTotalSumWeightInLinks += nodeWeights2[i];
		} 
		System.out.println("");
		
		// computing the aggregated weighted element rank
		//Actual algorithm implementation
		//d = 0.85; 5; sumwInLinks = 0; sum = 0; SqDev =0; sigma = 10e-6
		isWorkDone=false;
		do {
			//System.out.println("computing the rank ");
			//taking the backup of previous ranks for each iteration
			backupPreviousRanks(edgePageRank,previousRanks, totalNodeCount);
			
			for (m = 0; m < totalNodeCount; m++) {
				for (int b = 0; b < totalNodeCount; b++) {
					if (Math.abs(adjacencyGraph[b][m]) >= ALLOWED_EDGE_WEIGHT_OR_TOLERANCE)
						f1 += previousRanks[b] * adjacencyGraph[b][m] / nodeWeights1[b]; 
				}
				f1 = f1 * DAMPING_FACTOR + EQUAL_DISTRIBUTION_FACTOR * nodeWeights2[m] / aggregatedNodeWeightOrTotalSumWeightInLinks;
				f3 += (f1 - pageRank[m]) * (f1 - pageRank[m]);
				pageRank[m] = f1;
				edgePageRank[m] = f1;
				f1 = 0.0F;
			}
			
			if (Math.sqrt(f3) - 1.0E-13D < 0 || n + 1 > 10000) {
				System.out.println("==========================================================");
				
				if (bool) {
					String outFile = String.valueOf(outputFileName.substring(0, outputFileName.lastIndexOf("."))) + "GlobalWeightedRank.txt";
					System.out.println("outFile :"+outFile);
					bufferedWriter = new BufferedWriter(new FileWriter(new File(outFile)));
					bool = false;
				}
				
				for (m = 0; m < totalNodeCount; m++) {
					float val = getVal(fileName);
					//aggregating the weighted page rank by AHP
					aggregatedWeightedPageRank[m] = aggregatedWeightedPageRank[m] + val * pageRank[m];
					//printing the computation
					System.out.println(getRelationType(fileName)+"= "+val + "*" + aggregatedWeightedPageRank[m]);
					
					bufferedWriter.write(String.valueOf(nodes[m]) + "\t" + aggregatedWeightedPageRank[m]);
					bufferedWriter.newLine();
				} 
				filesNeedTOProccessed--;
				isWorkDone=true;
			} else {
				f3 = 0.0F;
				n++;
			}
			
			if (isWorkDone && bufferedWriter != null) {
				bufferedWriter.flush();
				bufferedWriter.close();
			}
		}while(!isWorkDone);
	}

	private static List<String> prepareFilesList(String filePath, String fileExtension) throws IOException {
		ArrayList<String> arrayList = new ArrayList<>();

		/* validation */	   
		if(Objects.isNull(filePath)|| "".equalsIgnoreCase(filePath)) {
			System.out.println("file path null or empty is not allowed.");
			return arrayList;
		}

		if(Objects.isNull(fileExtension)|| "".equalsIgnoreCase(fileExtension)) {
			System.out.println("file extension null or empty is not allowed.");
			return arrayList;
		}

		if(!".net".equalsIgnoreCase(fileExtension)) {
			System.out.println("file "+ fileExtension +" is not allowed.");
			return arrayList;
		}

		Path path = Paths.get(filePath);

		if(!Files.exists(path, LinkOption.NOFOLLOW_LINKS)|| 
				!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
			System.out.println("directory not found.");
			return arrayList;
		}

		//Recursively add file paths into the list
		Files.walk(path)
		.filter(Files::isRegularFile)
		.filter(each->each.toAbsolutePath().toString().endsWith(fileExtension))
		.forEach(each -> {
			arrayList.add(each.toFile().getAbsolutePath());	
		});
		return arrayList;
	}

	public static void main(String[] args) {
		try {
			Instant now = Instant.now();
			String filePath = "C:\\Users\\bojjam\\Downloads\\Assignments\\Assignments\\lib\\data set\\jdk_MPN";
			List<String> list = prepareFilesList(filePath, ".net");
			filesNeedTOProccessed = list.size();
			totalNumberOfFiles = list.size();
			for (String fileName : list) {
				String outputFileName = String.valueOf(fileName.substring(0, fileName.lastIndexOf("."))) + "_OUTPUT.txt";
				try {
					computeElementRank(fileName, outputFileName);
				} catch (Exception exception) {
					exception.printStackTrace();
				} 
			}
			System.out.println("elapsed time in seconds: "+Duration.between(now, Instant.now()).toSeconds());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}