package GTExperiment2bWithSafepoint.Readers.Top20;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class JfrDumpsCSVSummary {

    public static void main(String[] args) throws IOException {

        // Directory with all the .jfr files, e.g. "GTExperiment2/JFRData"
        String directoryPath = "/home/hb478/repos/GTResearchExperiments/GTExperiment2bWithSafepoint/JFRData";

        // Output CSV file
        String outputCsv = "/home/hb478/repos/GTResearchExperiments/GTExperiment2bWithSafepoint/Readers/Top20/Output/JFR_Summary.csv";

        // Create/overwrite CSV with a header row
        try (FileWriter writer = new FileWriter(outputCsv)) {
            writer.write("FileName,SlowdownSpeed,Iteration,Benchmark,IsSlowdown");
            for (int i = 1; i <= 20; i++) {
                writer.write(",Method" + i + ",Count" + i);
            }
            writer.write("\n");
        }

        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("Directory does not exist or is not a directory: " + directoryPath);
            return;
        }

        // List all .jfr files
        File[] files = dir.listFiles((d, name) -> name.endsWith(".jfr"));
        if (files == null) {
            System.err.println("No JFR files found in: " + directoryPath);
            return;
        }

        // For each .jfr file, parse the top 20 methods and write a CSV row
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            String fileName = file.getName();
            FileMetadata metadata = parseFileName(fileName);
            if (metadata == null) {
                System.out.println("Skipping file, name format unknown: " + fileName);
                continue;
            }

            // Get top 20 methods from the JFR file
            String[][] top20 = parseJfrFileTop20(file);
            writeCsvRow(outputCsv, fileName, metadata, top20);
        }

        System.out.println("Done! CSV summary written to " + outputCsv);
    }

    /**
     * Reads the JFR file and returns the top 20 [method, count].
     * This example uses a hypothetical JfrTop class.
     */
    private static String[][] parseJfrFileTop20(File file) {
        String[][] top20 = new String[20][2];
        for (int i = 0; i < 20; i++) {
            top20[i][0] = "";
            top20[i][1] = "0";
        }

        // Example using JfrTop. Adjust this to your actual logic.
        JfrTop jfrTop = new JfrTop();
        try {
            String[][] topX = jfrTop.getHottestMethods(file.getAbsolutePath());
            for (int i = 0; i < 20 && i < topX.length; i++) {
                top20[i][0] = topX[i][0];
                top20[i][1] = topX[i][1];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return top20;
    }

    /**
     * Parse a .jfr filename like "100_3_Queens_Slowdown.jfr" to extract metadata.
     */
    private static FileMetadata parseFileName(String fileName) {
        if (fileName.endsWith(".jfr")) {
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
     * Appends one CSV row with the given data.
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

    // Helper class for storing file name metadata
    static class FileMetadata {
        String slowdownSpeed;
        String iteration;
        String benchmark;
        boolean isSlowdown;
    }
}
