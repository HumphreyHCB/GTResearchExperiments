package GTExperiment2bWithSafepoint.Readers.Top20;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import java.util.Arrays;

public class JProfilerDumpsCSVSummary {

    public static void main(String[] args) throws IOException {

        // 1) Directory path containing JProfiler XML/JPS files
        String directoryPath = "/home/hb478/repos/GTResearchExperiments/GTExperiment2bWithSafepoint/JProfilerDataXML";  
        // 2) Output CSV file
        String outputCsv = "/home/hb478/repos/GTResearchExperiments/GTExperiment2bWithSafepoint/Readers/Top20/Output/JProfiler_Summary.csv";

        // Initialize CSV file (write header)
        try (FileWriter writer = new FileWriter(outputCsv)) {
            writer.write("FileName,SlowdownSpeed,Iteration,Benchmark,IsSlowdown");
            for (int i = 1; i <= 20; i++) {
                writer.write(",Method" + i + ",Percent" + i);
            }
            writer.write("\n");
        }

        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("Directory does not exist or is not a directory: " + directoryPath);
            return;
        }

        // List all JProfiler files (.xml or .jps)
        File[] files = dir.listFiles((d, name) -> 
            name.toLowerCase().endsWith(".xml") || name.toLowerCase().endsWith(".jps")
        );
        if (files == null) {
            System.err.println("No JProfiler files found in: " + directoryPath);
            return;
        }

        Arrays.sort(files);

        // For each file, parse the metadata and the top 20 hotspots
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            String fileName = file.getName();
            FileMetadata metadata = parseFileName(fileName);
            if (metadata == null) {
                System.out.println("Skipping file, unrecognized name format: " + fileName);
                continue;
            }

            String[][] top20 = parseJProfilerFileTop20(file.getAbsolutePath());

            // Append row to CSV
            writeCsvRow(outputCsv, fileName, metadata, top20);
        }

        System.out.println("Done! CSV summary is written to " + outputCsv);
    }

    /**
     * Parse JProfiler data (XML or JPS) to extract the top 20 hotspots.
     */
    private static String[][] parseJProfilerFileTop20(String filename) {
        String[][] top20 = new String[20][2];
        for (int i = 0; i < 20; i++) {
            top20[i][0] = "";
            top20[i][1] = "0";
        }

        try {
            File fXmlFile = new File(filename);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("hotspot");

            // We'll only pick the first 20 hotspots
            for (int i = 0; i < nList.getLength() && i < 20; i++) {
                Node nNode = nList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    // E.g. "class" attribute + "." + "methodName", and "percent"
                    String cls = eElement.getAttribute("class");
                    String methodName = eElement.getAttribute("methodName");
                    String percent = eElement.getAttribute("percent");

                    top20[i][0] = cls + "." + methodName;
                    top20[i][1] = percent;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return top20;
    }

    /**
     * Parse the filename (e.g. "100_3_Queens_Slowdown.xml") to extract metadata.
     */
    private static FileMetadata parseFileName(String fileName) {
        if (fileName.toLowerCase().endsWith(".xml")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        } else if (fileName.toLowerCase().endsWith(".jps")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        String[] parts = fileName.split("_");
        if (parts.length < 4) {
            return null;
        }

        String slowdownSpeed = parts[0];
        String iteration = parts[1];
        String lastPart = parts[parts.length - 1];
        boolean isSlowdown = lastPart.equalsIgnoreCase("Slowdown");

        StringBuilder bench = new StringBuilder();
        for (int i = 2; i < parts.length - 1; i++) {
            if (i > 2) {
                bench.append("_");
            }
            bench.append(parts[i]);
        }
        String benchmark = bench.toString();

        FileMetadata data = new FileMetadata();
        data.slowdownSpeed = slowdownSpeed;
        data.iteration = iteration;
        data.benchmark = benchmark;
        data.isSlowdown = isSlowdown;
        return data;
    }

    /**
     * Append a row to the CSV with the metadata and twenty (method, percent) pairs.
     */
    private static void writeCsvRow(String outputCsv, String fileName, 
                                    FileMetadata metadata, String[][] top20) 
                                    throws IOException {
        try (FileWriter writer = new FileWriter(outputCsv, true)) {
            StringBuilder sb = new StringBuilder();

            sb.append(fileName).append(",");
            sb.append(metadata.slowdownSpeed).append(",");
            sb.append(metadata.iteration).append(",");
            sb.append(metadata.benchmark).append(",");
            sb.append(metadata.isSlowdown ? "Slowdown" : "NoSlowdown");

            for (int i = 0; i < 20; i++) {
                sb.append(",\"").append(top20[i][0]).append("\",");
                sb.append(top20[i][1]);
            }
            sb.append("\n");
            writer.write(sb.toString());
        }
    }

    // Simple data holder for metadata
    static class FileMetadata {
        String slowdownSpeed;
        String iteration;
        String benchmark;
        boolean isSlowdown;
    }
}
