package Readers;

import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

public class YourKitDumpsCSVSummary {

    public static void main(String[] args) throws IOException {
        // Directory with .txt exports from YourKit
        // e.g. "GTExperiment2/YourKitDataTXT"
        String inputDir = "GTExperiment2/YourKitDataTXT";

        // Output CSV file
        String outputCsv = "GTExperiment2/Readers/Output/YourKit_Summary.csv";

        // Overwrite or create the CSV with a header row
        try (FileWriter writer = new FileWriter(outputCsv)) {
            writer.write("FileName,SlowdownSpeed,Iteration,Benchmark,IsSlowdown,"
                    + "Method1,Count1,Method2,Count2,Method3,Count3,Method4,Count4,Method5,Count5\n");
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

        // For each .txt file, parse the top 5 methods, then write to CSV
        for (File file : files) {
            String fileName = file.getName();

            // 1) Parse the filename to get metadata
            FileMetadata metadata = parseFileName(fileName);
            if (metadata == null) {
                System.out.println("Skipping file, unrecognized format: " + fileName);
                continue;
            }

            // 2) Parse top 5 from text file
            String[][] top5 = YourKitParser.getTop5MethodsByOwnTimePercent(file.getAbsolutePath());

            // 3) Append row to CSV
            writeCsvRow(outputCsv, fileName, metadata, top5);
        }

        System.out.println("Done! Wrote summary to " + outputCsv);
    }

    /**
     * Parse a YourKit "Method-list-CPU.txt" to extract top 5 [method, percent or count].
     * 
     * The actual text format depends on your YourKit version and export options.
     * Below is an example based on your older "YourKitHottestMethods" approach, 
     * where you scanned lines for a table enclosed by:
     *    +---------------------------
     * Adapt the logic to match how your exported .txt files are structured.
     */
    private static String[][] parseYourKitCPUFileTop5(File file) {
        // We'll store the top 5 lines as [5][2]: [methodName, percentage/samples]
        String[][] top5 = new String[5][2];
        for (int i = 0; i < 5; i++) {
            top5[i][0] = "";
            top5[i][1] = "0";
        }

        try (Scanner scanner = new Scanner(file)) {
            boolean tableStarted = false;
            int count = 0;

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();

                // Suppose the table starts after a line containing
                // "+--------------------------"
                // and ends when we hit a similar boundary.
                // This is just an example marker from your older code.
                if (line.contains("+--------------------------")) {
                    if (!tableStarted) {
                        // We just found the start
                        tableStarted = true;
                        // Possibly skip some header lines. 
                        // E.g. if the very next lines are column headers,
                        // you can skip them:
                        continue;
                    } else {
                        // If we see a second boundary, we stop
                        break;
                    }
                }

                // If we are inside the table, parse the line
                if (tableStarted && count < 5) {
                    // For example, line might be:
                    // "com.example.MyClass.myMethod   <123>   12.34%"
                    // or some other structure. Adjust the split accordingly.
                    String[] split = line.split("\\s+");
                    if (split.length < 2) {
                        continue; // skip malformed lines
                    }

                    // We'll guess the last part might be the percentage or samples
                    String lastToken = split[split.length - 1];
                    // The method name might be everything up to that
                    StringBuilder method = new StringBuilder();
                    for (int i = 0; i < split.length - 1; i++) {
                        if (i > 0) {
                            method.append(" ");
                        }
                        method.append(split[i]);
                    }

                    top5[count][0] = method.toString();
                    top5[count][1] = lastToken;
                    count++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return top5;
    }

    /**
     * Parse the filename (e.g. "100_1_Bounce_NoSlowdown-2025-02-09-shutdown_Method-list-CPU.txt")
     * to extract slowdownSpeed, iteration, benchmark, isSlowdown.
     *
     * The logic is similar to your older code, except the snapshot name
     * has an extra date/time chunk. We'll do a partial parse:
     *
     * Example of your typical pattern:
     *  100_1_Bounce_NoSlowdown-2025-02-09-shutdown_Method-list-CPU.txt
     *
     * We might parse:
     *  slowdownSpeed = 100
     *  iteration     = 1
     *  benchmark     = Bounce
     *  isSlowdown    = NoSlowdown
     */
    private static FileMetadata parseFileName(String fileName) {
        // Remove the suffix "_Method-list-CPU.txt"
        if (fileName.endsWith("_Method-list-CPU.txt")) {
            fileName = fileName.substring(0, fileName.length() - "_Method-list-CPU.txt".length());
        }

        // Now we might have something like:
        // "100_1_Bounce_NoSlowdown-2025-02-09-shutdown"
        // We'll split around the first 3 underscores to get 
        //   [0] = "100"
        //   [1] = "1"
        //   [2] = "Bounce"
        //   [3..end] = "NoSlowdown-2025-02-09-shutdown"
        String[] parts = fileName.split("_");
        if (parts.length < 3) {
            return null; // Not in expected format
        }

        String slowdownSpeed = parts[0];
        String iteration = parts[1];
        // The third chunk is the benchmark's "base"
        String benchmarkPart = parts[2];

        // But you also have "NoSlowdown" or "Slowdown" plus date/time after that.
        // A quick hack is: check if parts[3] starts with "NoSlowdown" or "Slowdown"
        // or if it's merged with the date/time.
        // We'll do something simpler: check if the remainder of the string 
        // (beyond the first 2 underscores) contains "Slowdown" or "NoSlowdown".
        // Then we can label isSlowdown accordingly. 
        // This may require more robust logic if your naming varies.

        boolean isSlowdown = fileName.contains("_Slowdown");
        // or check e.g. "NoSlowdown" similarly if needed

        // We'll define the "benchmark" as everything from parts[2] if you want,
        // ignoring the date/time suffix. You can refine if you want a 
        // more precise approach to extracting the date/time from the name.

        FileMetadata meta = new FileMetadata();
        meta.slowdownSpeed = slowdownSpeed;
        meta.iteration = iteration;
        meta.benchmark = benchmarkPart; // minimal
        meta.isSlowdown = isSlowdown;
        return meta;
    }

    /**
     * Append a row to the CSV with the file’s metadata + the top 5 methods.
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

            // top5 is [5][2], each row: [method, count]
            for (int i = 0; i < 5; i++) {
                // Wrap method name in quotes to avoid CSV confusion
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

    // Simple class to hold parsed metadata from the filename
    static class FileMetadata {
        String slowdownSpeed;
        String iteration;
        String benchmark;
        boolean isSlowdown;
    }
}
