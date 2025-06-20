package GTExperiment2bWithSafepoint.Readers.Top20;

import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

public class YourKitDumpsCSVSummary {

    public static void main(String[] args) throws IOException {
        // Directory with .txt exports from YourKit
        // e.g. "GTExperiment2/YourKitDataTXT"
        String inputDir = "/home/hb478/repos/GTResearchExperiments/GTExperiment2/YourKitDataTXT";

        // Output CSV file
        String outputCsv = "/home/hb478/repos/GTResearchExperiments/GTExperiment2bWithSafepoint/Readers/Top20/Output/YourKit_Summary.csv";

        // Overwrite or create the CSV with a header row
        try (FileWriter writer = new FileWriter(outputCsv)) {
            writer.write("FileName,SlowdownSpeed,Iteration,Benchmark,IsSlowdown");
            // Append headers for top 20 methods
            for (int i = 1; i <= 20; i++) {
                writer.write(",Method" + i + ",Count" + i);
            }
            writer.write("\n");
        }

        File dir = new File(inputDir);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("Directory does not exist or is not a directory: " + inputDir);
            return;
        }

        // Filter for text files that end with "_Method-list-CPU.txt"
        File[] files = dir.listFiles((d, name) -> name.endsWith("_Method-list-CPU.txt"));
        if (files == null || files.length == 0) {
            System.err.println("No YourKit CPU text files found in: " + inputDir);
            return;
        }

        // Sort by filename (optional)
        Arrays.sort(files);

        // For each .txt file, parse the top 20 methods, then write to CSV
        for (File file : files) {
            String fileName = file.getName();

            // 1) Parse the filename to get metadata
            FileMetadata metadata = parseFileName(fileName);
            if (metadata == null) {
                System.out.println("Skipping file, unrecognized format: " + fileName);
                continue;
            }

            // 2) Parse top 20 from text file
            String[][] top20 = YourKitParser.getTop20MethodsByOwnTimePercent(file.getPath());

            // 3) Append row to CSV
            writeCsvRow(outputCsv, fileName, metadata, top20);
        }

        System.out.println("Done! Wrote summary to " + outputCsv);
    }

    /**
     * Parse a YourKit "Method-list-CPU.txt" to extract the top 20 [method, percentage/samples].
     */
    private static String[][] parseYourKitCPUFileTop20(File file) {
        // We'll store the top 20 lines as [20][2]: [methodName, percentage/samples]
        String[][] top20 = new String[20][2];
        for (int i = 0; i < 20; i++) {
            top20[i][0] = "";
            top20[i][1] = "0";
        }

        try (Scanner scanner = new Scanner(file)) {
            boolean tableStarted = false;
            int count = 0;

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();

                // Suppose the table starts after a line containing a boundary marker.
                if (line.contains("+--------------------------")) {
                    if (!tableStarted) {
                        tableStarted = true;
                        // Skip the header lines if needed.
                        continue;
                    } else {
                        // End when the table finishes.
                        break;
                    }
                }

                if (tableStarted && count < 20) {
                    String[] split = line.split("\\s+");
                    if (split.length < 2) {
                        continue; // skip malformed lines
                    }

                    // Assume the last token is the percentage/samples,
                    // and the rest is the method name.
                    String lastToken = split[split.length - 1];
                    StringBuilder method = new StringBuilder();
                    for (int i = 0; i < split.length - 1; i++) {
                        if (i > 0) {
                            method.append(" ");
                        }
                        method.append(split[i]);
                    }

                    top20[count][0] = method.toString();
                    top20[count][1] = lastToken;
                    count++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return top20;
    }

    /**
     * Parse the filename (e.g. "100_1_Bounce_NoSlowdown-2025-02-09-shutdown_Method-list-CPU.txt")
     * to extract slowdownSpeed, iteration, benchmark, isSlowdown.
     */
    private static FileMetadata parseFileName(String fileName) {
        if (fileName.endsWith("_Method-list-CPU.txt")) {
            fileName = fileName.substring(0, fileName.length() - "_Method-list-CPU.txt".length());
        }
        String[] parts = fileName.split("_");
        if (parts.length < 3) {
            return null;
        }

        String slowdownSpeed = parts[0];
        String iteration = parts[1];
        String benchmarkPart = parts[2];
        boolean isSlowdown = fileName.contains("_Slowdown");

        FileMetadata meta = new FileMetadata();
        meta.slowdownSpeed = slowdownSpeed;
        meta.iteration = iteration;
        meta.benchmark = benchmarkPart;
        meta.isSlowdown = isSlowdown;
        return meta;
    }

    /**
     * Append a row to the CSV with the file’s metadata and the top 20 methods.
     */
    private static void writeCsvRow(
        String outputCsv, String fileName, 
        FileMetadata metadata, String[][] top20
    ) throws IOException {
        try (FileWriter writer = new FileWriter(outputCsv, true)) {
            StringBuilder sb = new StringBuilder();
            sb.append(fileName).append(",");
            sb.append(metadata.slowdownSpeed).append(",");
            sb.append(metadata.iteration).append(",");
            sb.append(metadata.benchmark).append(",");
            sb.append(metadata.isSlowdown ? "Slowdown" : "NoSlowdown");
    
            // We only ever want 20 methods, or fewer if there aren't enough
            int maxMethods = Math.min(20, top20.length);
            for (int i = 0; i < 20; i++) {
                if (i < maxMethods) {
                    // Write real data from top20
                    sb.append(",\"").append(top20[i][0]).append("\",");
                    sb.append(top20[i][1]);
                } else {
                    // Fill in placeholders for any missing methods
                    sb.append(",\"").append("N/A").append("\",");
                    sb.append("0");
                }
            }
            sb.append("\n");
            writer.write(sb.toString());
        }
    }
    

    // Simple class to hold parsed metadata from the filename
    static class FileMetadata {
        String slowdownSpeed;
        String iteration;
        String benchmark;
        boolean isSlowdown;
    }
}
