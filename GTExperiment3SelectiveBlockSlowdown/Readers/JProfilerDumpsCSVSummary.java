package GTExperiment3SelectiveBlockSlowdown.Readers;

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
        //    e.g. "GTExperiment2/JProfilerData"
        String directoryPath = "GTExperiment3SelectiveBlockSlowdown/JProfilerDataXMLNoSlowdown";  

        // 2) Output CSV file
        String outputCsv = "GTExperiment3SelectiveBlockSlowdown/Readers/Output/JProfiler_Summary_NoSlowdown.csv";

        // Initialize CSV file (write header)
        try (FileWriter writer = new FileWriter(outputCsv)) {
            writer.write("FileName,TargetBlock,Iteration,Benchmark,IsSlowdown,");
            writer.write("Method1,Percent1,Method2,Percent2,Method3,Percent3,Method4,Percent4,Method5,Percent5\n");
        }

        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("Directory does not exist or is not a directory: " + directoryPath);
            return;
        }

        // 3) List all JProfiler files. Adjust the extension if needed:
        //    If your files end with ".jps" or ".xml" or something else, set it here.
        File[] files = dir.listFiles((d, name) -> 
            name.toLowerCase().endsWith(".xml") || name.toLowerCase().endsWith(".jps")
        );
        if (files == null) {
            System.err.println("No JProfiler files found in: " + directoryPath);
            return;
        }

        // Sort files by name if you like
        Arrays.sort(files);

        // 4) Parse each file, get top 5 hotspots, write CSV row
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

            String[][] top5 = parseJProfilerFileTop5(file.getAbsolutePath());

            // Append row to CSV
            writeCsvRow(outputCsv, fileName, metadata, top5);
        }

        System.out.println("Done! CSV summary is written to " + outputCsv);
    }

    /**
     * Parse JProfiler data (XML or JPS) to extract the top 5 hotspots.
     * 
     * Adapted from your original code snippet:
     *
     * private static String[][] JProfilerHottestMethods(String Filename) { ... }
     */
    private static String[][] parseJProfilerFileTop5(String filename) {
        // We'll store top 5 [method, percent]
        String[][] top5 = new String[5][2];
        for (int i = 0; i < 5; i++) {
            top5[i][0] = "";
            top5[i][1] = "0";
        }

        try {
            File fXmlFile = new File(filename);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            // The original logic: NodeList nList = doc.getElementsByTagName("hotspot");
            NodeList nList = doc.getElementsByTagName("hotspot");

            // We'll only pick the first 5 hotspots
            for (int i = 0; i < nList.getLength() && i < 5; i++) {
                Node nNode = nList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    // E.g. "class" attribute + "." + "methodName", and "percent"
                    String cls = eElement.getAttribute("class");
                    String methodName = eElement.getAttribute("methodName");
                    String percent = eElement.getAttribute("percent");

                    top5[i][0] = cls + "." + methodName;
                    top5[i][1] = percent;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return top5;
    }

    /**
     * Parse the filename to extract slowdownSpeed, iteration, benchmark, isSlowdown.
     * Example: "100_3_Queens_Slowdown.xml"
     */
    private static FileMetadata parseFileName(String fileName) {
        // Strip extension if recognized
        if (fileName.toLowerCase().endsWith(".xml")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        } else if (fileName.toLowerCase().endsWith(".jps")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        // Now something like "100_3_Queens_Slowdown"
        String[] parts = fileName.split("_");
        if (parts.length < 4) {
            return null; // Not in the expected X_Y_Benchmark_Slowdown format
        }

        // e.g. "100_3_Queens_Slowdown"
        String slowdownSpeed = parts[0];
        String iteration = parts[1];
        String lastPart = parts[parts.length - 1];
        boolean isSlowdown = lastPart.equalsIgnoreCase("Slowdown");

        // The benchmark is everything between index 2 and the last part
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
     * Append a row to the CSV (FileName, SlowdownSpeed, Iteration, Benchmark, IsSlowdown,
     * five (method, percent) pairs).
     */
    private static void writeCsvRow(String outputCsv, String fileName, 
                                    FileMetadata metadata, String[][] top5) 
                                    throws IOException {
        try (FileWriter writer = new FileWriter(outputCsv, true)) {
            StringBuilder sb = new StringBuilder();

            sb.append(fileName).append(",");
            sb.append(metadata.slowdownSpeed).append(",");
            sb.append(metadata.iteration).append(",");
            sb.append(metadata.benchmark).append(",");
            sb.append(metadata.isSlowdown ? "Slowdown" : "NoSlowdown").append(",");

            // Now the 5 hotspots
            for (int i = 0; i < 5; i++) {
                // Quote the method name
                sb.append("\"").append(top5[i][0]).append("\",");
                sb.append(top5[i][1]);
                if (i < 4) {
                    sb.append(",");
                }
            }
            sb.append("\n");
            writer.write(sb.toString());
        }
    }

    // Simple data holder
    static class FileMetadata {
        String slowdownSpeed;
        String iteration;
        String benchmark;
        boolean isSlowdown;
    }
}
