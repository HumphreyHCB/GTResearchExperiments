package GTExperiment2bADHOCWithSafepointANDFixedListWithBetterMod.Readers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class AsyncDumpsCSVSummary {

    public static void main(String[] args) throws IOException {

        // Where all the .txt files reside:
        // E.g., "GTExperiment2/AsyncData" (relative or absolute path)
        String directoryPath = "/home/hb478/repos/GTResearchExperiments/GTExperiment2bADHOCWithSafepointANDFixedListWithBetterMod/AsyncData";

        // Where to write the CSV summary
        String outputCsv = "GTExperiment2bADHOCWithSafepointANDFixedListWithBetterMod/Readers/Output/Async_Summary.csv";

        // Initialize CSV with a header row
        try (FileWriter writer = new FileWriter(outputCsv)) {
            writer.write("FileName,SlowdownSpeed,Iteration,Benchmark,IsSlowdown,");
            writer.write("Method1,Count1,Method2,Count2,Method3,Count3,Method4,Count4,Method5,Count5\n");
        }

        // Process each file in that directory
        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("Directory does not exist or is not a directory: " + directoryPath);
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null) {
            System.err.println("No files found in: " + directoryPath);
            return;
        }

        // For each .txt file, parse and write CSV lines
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            // 1. Parse the filename to extract the data
            //    Example: 100_3_Queens_Slowdown.txt
            String fileName = file.getName();
            FileMetadata metadata = parseFileName(fileName);
            if (metadata == null) {
                // If parse fails for any reason, skip
                System.out.println("Skipping file, name format unknown: " + fileName);
                continue;
            }

            // 2. Parse the top 5 methods from the file content
            String[][] top5 = parseAsyncFileTop5(file);

            // 3. Write a row to the CSV
            writeCsvRow(outputCsv, fileName, metadata, top5);
        }

        System.out.println("Done! Wrote CSV summary to " + outputCsv);
    }

    /**
     * Parse the filename (e.g. "100_3_Queens_Slowdown.txt")
     * to extract slowdown speed, iteration, benchmark name, isSlowdown.
     */
    private static FileMetadata parseFileName(String fileName) {
        // Remove extension
        if (fileName.endsWith(".txt")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }

        // Expect 4 underscores if strictly matching "speed_iter_bench_Slowdown"
        // But handle variations if needed
        // Typical: "100_3_Queens_Slowdown"
        String[] parts = fileName.split("_");
        if (parts.length < 4) {
            return null; // Not in expected format
        }

        // Example mapping:
        //   parts[0] = 100 (the slowdown speed)
        //   parts[1] = 3    (the iteration)
        //   parts[2..(end-1)] = benchmark name if it has underscores. 
        //        But typically it's just one chunk, e.g. "Queens"
        //   last part: "Slowdown" or "NoSlowdown"

        String slowdownSpeed = parts[0];
        String iteration = parts[1];

        // The last part is whether it's "Slowdown" or "NoSlowdown"
        String lastPart = parts[parts.length - 1];
        boolean isSlowdown = lastPart.equalsIgnoreCase("Slowdown");

        // The benchmark might be everything in between, but typically it’s just parts[2].
        // If you have multi-word benchmarks with underscores (rare in your example),
        // you’d join parts[2..(length-2)].
        StringBuilder bench = new StringBuilder();
        for (int i = 2; i < parts.length - 1; i++) {
            if (i > 2) {
                bench.append("_"); // put underscore back if needed
            }
            bench.append(parts[i]);
        }
        String benchmark = bench.toString();

        // Build the metadata
        FileMetadata data = new FileMetadata();
        data.slowdownSpeed = slowdownSpeed;
        data.iteration = iteration;
        data.benchmark = benchmark;
        data.isSlowdown = isSlowdown;
        return data;
    }

    /**
     * Reads an "Async" style file, looks for the line containing
     * "----------  -------  -------  ---" and then reads the next 5 lines,
     * parsing them to get (methodName, count).
     */
    private static String[][] parseAsyncFileTop5(File file) {
        String[][] top5 = new String[5][2]; // [i][0] -> methodName, [i][1] -> count
        for (int i = 0; i < 5; i++) {
            top5[i][0] = "";
            top5[i][1] = "0";
        }
        try (Scanner scanner = new Scanner(file)) {
            boolean foundMarker = false;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains("----------  -------  -------  ---")) {
                    foundMarker = true;
                    // Next 5 lines contain the top 5
                    for (int i = 0; i < 5; i++) {
                        if (!scanner.hasNextLine()) {
                            break; // file ended prematurely
                        }
                        String row = scanner.nextLine().trim();
                        // e.g. after splitting by whitespace:
                        // The original example code was:
                        //    if split[0].equals("") then top10[i][0] = split[4], top10[i][1] = split[2]
                        //    else top10[i][0] = split[3], top10[i][1] = split[1]
                        // We'll replicate that logic carefully.

                        String[] split = row.split("\\s+");
                        if (split.length < 4) {
                            // Malformed line, skip
                            top5[i][0] = "UnknownMethod";
                            top5[i][1] = "0";
                            continue;
                        }
                        if (split[0].equals("")) {
                            // Then method name is split[4], count is split[2]
                            // (check length, though, to avoid out of bounds)
                            if (split.length >= 5) {
                                top5[i][0] = cleanLambdaMethodName(split[4]);
                                top5[i][1] = split[2].replace("%", ""); // remove % if present
                            }
                        } else {
                            // method name is split[3], count is split[1]
                            if (split.length >= 4) {
                                top5[i][0] = cleanLambdaMethodName(split[3]);
                                top5[i][1] = split[1].replace("%", ""); // remove % if present
                            }
                        }
                    }
                    break; // We got our top 5
                }
            }
            if (!foundMarker) {
                // The file never had the "----------  -------  -------  ---" line
                // We'll just keep the default top5 placeholders or you can handle differently
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return top5;
    }

    public static String cleanLambdaMethodName(String methodName) {
        if (methodName.contains("$$Lambda.")) {
            int lambdaIndex = methodName.indexOf("$$Lambda.");
            int dotIndex = methodName.indexOf('.', lambdaIndex + 9);
            if (dotIndex != -1) {
                return methodName.substring(0, lambdaIndex + 8) + methodName.substring(dotIndex);
            }
        }
        return methodName;
    }

    /**
     * Appends one CSV row to the given CSV file with the extracted info.
     */
    private static void writeCsvRow(
            String outputCsv,
            String fileName,
            FileMetadata metadata,
            String[][] top5
    ) throws IOException {

        // top5 is a 5x2 array: top5[i][0] => methodName, top5[i][1] => count

        try (FileWriter writer = new FileWriter(outputCsv, true)) {
            StringBuilder sb = new StringBuilder();
            // CSV columns:
            //  FileName,SlowdownSpeed,Iteration,Benchmark,IsSlowdown,
            //  Method1,Count1,Method2,Count2,Method3,Count3,Method4,Count4,Method5,Count5

            sb.append(fileName).append(",");
            sb.append(metadata.slowdownSpeed).append(",");
            sb.append(metadata.iteration).append(",");
            sb.append(metadata.benchmark).append(",");
            sb.append(metadata.isSlowdown ? "Slowdown" : "NoSlowdown").append(",");

            // For each of the top 5
            for (int i = 0; i < 5; i++) {
                // In practice you should probably quote method names for safety
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

    // A small holder object for the parseFileName logic
    static class FileMetadata {
        String slowdownSpeed;
        String iteration;
        String benchmark;
        boolean isSlowdown;
    }
}
