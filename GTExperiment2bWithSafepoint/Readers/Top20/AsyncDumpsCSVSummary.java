package GTExperiment2bWithSafepoint.Readers.Top20;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class AsyncDumpsCSVSummary {

    public static void main(String[] args) throws IOException {

        // Where all the .txt files reside:
        String directoryPath = "/home/hb478/repos/GTResearchExperiments/GTExperiment2bWithSafepoint/AsyncData";
        // Where to write the CSV summary
        String outputCsv = "/home/hb478/repos/GTResearchExperiments/GTExperiment2bWithSafepoint/Readers/Top20/Output/Async_Summary.csv";

        // Initialize CSV with a header row
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

            String fileName = file.getName();
            FileMetadata metadata = parseFileName(fileName);
            if (metadata == null) {
                System.out.println("Skipping file, name format unknown: " + fileName);
                continue;
            }

            String[][] top20 = parseAsyncFileTop20(file);
            writeCsvRow(outputCsv, fileName, metadata, top20);
        }

        System.out.println("Done! Wrote CSV summary to " + outputCsv);
    }

    /**
     * Parse the filename (e.g. "100_3_Queens_Slowdown.txt")
     */
    private static FileMetadata parseFileName(String fileName) {
        if (fileName.endsWith(".txt")) {
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
     * Reads an "Async" style file, finds the marker line, then reads the next 20 lines.
     */
    private static String[][] parseAsyncFileTop20(File file) {
        String[][] top20 = new String[20][2];
        for (int i = 0; i < 20; i++) {
            top20[i][0] = "";
            top20[i][1] = "0";
        }
        try (Scanner scanner = new Scanner(file)) {
            boolean foundMarker = false;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains("----------  -------  -------  ---")) {
                    foundMarker = true;
                    // Next 20 lines contain the top 20 methods
                    for (int i = 0; i < 20; i++) {
                        if (!scanner.hasNextLine()) {
                            break; // file ended prematurely
                        }
                        String row = scanner.nextLine().trim();
                        String[] split = row.split("\\s+");
                        if (split.length < 4) {
                            top20[i][0] = "UnknownMethod";
                            top20[i][1] = "0";
                            continue;
                        }
                        if (split[0].equals("")) {
                            if (split.length >= 5) {
                                top20[i][0] = cleanLambdaMethodName(split[4]);
                                top20[i][1] = split[2].replace("%", "");
                            }
                        } else {
                            if (split.length >= 4) {
                                top20[i][0] = cleanLambdaMethodName(split[3]);
                                top20[i][1] = split[1].replace("%", "");
                            }
                        }
                    }
                    break;
                }
            }
            if (!foundMarker) {
                // Handle the case if the marker isn't found as needed.
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return top20;
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
     * Appends one CSV row with the extracted info.
     */
    private static void writeCsvRow(
            String outputCsv,
            String fileName,
            FileMetadata metadata,
            String[][] top20
    ) throws IOException {
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

    // A small holder for the parseFileName logic
    static class FileMetadata {
        String slowdownSpeed;
        String iteration;
        String benchmark;
        boolean isSlowdown;
    }
}
