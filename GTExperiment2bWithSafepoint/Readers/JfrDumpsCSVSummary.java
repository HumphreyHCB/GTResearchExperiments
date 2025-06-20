package GTExperiment2bWithSafepoint.Readers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class JfrDumpsCSVSummary {

    public static void main(String[] args) throws IOException {

        // Directory with all the .jfr files, e.g. "GTExperiment2bWithSafepoint/JFRData"
        String directoryPath = "GTExperiment2bWithSafepoint/JFRData";

        // Output CSV file
        String outputCsv = "GTExperiment2bWithSafepoint/Readers/Output/JFR_Summary.csv";

        // Create/overwrite CSV with a header row
        try (FileWriter writer = new FileWriter(outputCsv)) {
            writer.write("FileName,SlowdownSpeed,Iteration,Benchmark,IsSlowdown,");
            writer.write("Method1,Count1,Method2,Count2,Method3,Count3,Method4,Count4,Method5,Count5\n");
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

        // For each .jfr file, parse the top 5 methods and write a CSV row
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            String fileName = file.getName();
            FileMetadata metadata = parseFileName(fileName);
            if (metadata == null) {
                // If parse fails, skip or handle differently
                System.out.println("Skipping file, name format unknown: " + fileName);
                continue;
            }

            // Get top 5 methods from the JFR file
            String[][] top5 = parseJfrFileTop5(file);

            // Append row to CSV
            writeCsvRow(outputCsv, fileName, metadata, top5);
        }

        System.out.println("Done! CSV summary written to " + outputCsv);
    }

    /**
     * Parse a .jfr filename like "100_3_Queens_Slowdown.jfr" to extract:
     *  - slowdownSpeed = "100"
     *  - iteration     = "3"
     *  - benchmark     = "Queens"
     *  - isSlowdown    = true if ends with "Slowdown"
     */
    private static FileMetadata parseFileName(String fileName) {
        // Remove .jfr extension
        if (fileName.endsWith(".jfr")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        // Split by underscore
        String[] parts = fileName.split("_");
        if (parts.length < 4) {
            return null; // Unexpected format
        }

        // For example: 100_3_Queens_Slowdown
        String slowdownSpeed = parts[0];
        String iteration = parts[1];
        String lastPart = parts[parts.length - 1];
        boolean isSlowdown = lastPart.equalsIgnoreCase("Slowdown");

        // The benchmark is everything between parts[2] and the last part
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
     * Reads the JFR file and returns the top 5 [method, count].
     *
     * Below are two possible approaches. Please adapt to the one you have:
     *
     * (A) If you have a Java class called JfrTop with a method getHottestMethods(fileName):
     *
     *        private static String[][] parseJfrFileTop5(File file) {
     *            // e.g. your original usage:
     *            // String[][] top10 = new JfrTop().getHottestMethods(file.getAbsolutePath());
     *            // but we only want top 5:
     *
     *            String[][] top5 = new String[5][2];
     *            for (int i = 0; i < 5; i++) {
     *                top5[i][0] = "";
     *                top5[i][1] = "0";
     *            }
     *
     *            try {
     *                String[][] topX = new JfrTop().getHottestMethods(file.getAbsolutePath());
     *                // copy only first 5
     *                for (int i = 0; i < 5 && i < topX.length; i++) {
     *                    top5[i][0] = topX[i][0];
     *                    top5[i][1] = topX[i][1];
     *                }
     *            } catch (Exception e) {
     *                e.printStackTrace();
     *            }
     *            return top5;
     *        }
     *
     * (B) If you have to call an external CLI:
     *     e.g. "java -jar JfrTop.jar <file> 5" or "jfr top <file> ...", parse the stdout, etc.
     *
     */
    private static String[][] parseJfrFileTop5(File file) {
        // For safety, default to empty placeholders
        // String[][] top5 = new String[5][2];
        // for (int i = 0; i < 5; i++) {
        //     top5[i][0] = "";
        //     top5[i][1] = "0";
        // }

        // =========== EXAMPLE: External CLI via ProcessBuilder =========== //
        // Adjust command line to your environment/tool usage:
        // For instance, if you have a "JfrTop" jar or script:
        //
        //    ProcessBuilder pb = new ProcessBuilder(
        //        "java", "-jar", "/path/to/JfrTop.jar",
        //        file.getAbsolutePath(),
        //        "5"  // or however many you want
        //    );
        //
        // If there's no such tool, you may rely on the "JfrTop" class
        // approach (A) shown above, or on official JMC APIs.
        //
        // For demonstration, I'll show a pseudo CLI parse:

        // try {
        //     ProcessBuilder pb = new ProcessBuilder(
        //         "java",  // obviously adapt or remove if you have a direct executable
        //         "JfrTop", 
        //         file.getAbsolutePath(),
        //         "5"     // meaning "top 5"
        //     );
        //     Process process = pb.start();
        //     BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        //     // Suppose the tool prints lines like: "methodName, sampleCount"
        //     // We'll just read 5 lines:
        //     for (int i = 0; i < 5; i++) {
        //         String line = reader.readLine();
        //         if (line == null) {
        //             break; // no more data
        //         }
        //         // parse: methodName, sampleCount
        //         // e.g. "com.foo.MyClass.myMethod, 123"
        //         String[] parts = line.split(",");
        //         if (parts.length >= 2) {
        //             top5[i][0] = parts[0].trim();
        //             top5[i][1] = parts[1].trim();
        //         }
        //     }
        //     process.waitFor();
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }

        // return top5;
        JfrTop jfrTop = new JfrTop();
        return jfrTop.getHottestMethods(file.getAbsolutePath());
    }

    /**
     * Appends one CSV row with the given data.
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
                // Wrap method in quotes to avoid CSV issues
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

    // Helper class for storing file name metadata
    static class FileMetadata {
        String slowdownSpeed;
        String iteration;
        String benchmark;
        boolean isSlowdown;
    }
}
