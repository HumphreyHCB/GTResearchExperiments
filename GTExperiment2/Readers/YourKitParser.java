package Readers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class YourKitParser {

    public static void main(String[] args) {
        // For demonstration, specify a sample .txt path
        // e.g. "GTExperiment2/YourKitDataTXT/100_1_Bounce_NoSlowdown_Method-list-CPU.txt"
        String filePath = "GTExperiment2/YourKitDataTXT/50_1_Bounce_NoSlowdown-2025-02-08-shutdown_Method-list-CPU.txt";

        // 1) Parse the table rows into a list of Row objects
        List<Row> rows = parseYourKitCPUFile(filePath);

        if (rows.isEmpty()) {
            System.out.println("No data parsed from " + filePath);
            return;
        }

        // 2) Compute total own time across all methods
        double totalOwnTime = 0.0;
        for (Row r : rows) {
            totalOwnTime += r.ownTimeMs;
        }

        // 3) Calculate each row's % of total own time
        for (Row r : rows) {
            if (totalOwnTime > 0) {
                r.ownTimePercent = (r.ownTimeMs / totalOwnTime) * 100.0;
            } else {
                r.ownTimePercent = 0.0;
            }
        }

        // 4) Print out or store results
        // For example, let's just print them all
        // (sorted by largest ownTimePercent)
        rows.sort((a, b) -> Double.compare(b.ownTimePercent, a.ownTimePercent));
        System.out.printf("%-60s  %10s  %10s  %10s%n",
                          "Method", "OwnTime(ms)", "Samples", "% of Own");
        for (Row r : rows) {
            System.out.printf("%-60s  %10.1f  %10d  %8.2f%%%n",
                              r.method, r.ownTimeMs, r.samples, r.ownTimePercent);
        }
    }

    public static String[][] getTop5MethodsByOwnTimePercent(String filePath) {
        List<Row> rows = parseYourKitCPUFile(filePath);

                // 2) Compute total own time across all methods
                double totalOwnTime = 0.0;
                for (Row r : rows) {
                    totalOwnTime += r.ownTimeMs;
                }
        
                // 3) Calculate each row's % of total own time
                for (Row r : rows) {
                    if (totalOwnTime > 0) {
                        r.ownTimePercent = (r.ownTimeMs / totalOwnTime) * 100.0;
                    } else {
                        r.ownTimePercent = 0.0;
                    }
                }

        // Sort rows by ownTimePercent in descending order
        Collections.sort(rows, new Comparator<Row>() {
            @Override
            public int compare(Row r1, Row r2) {
                return Double.compare(r2.ownTimePercent, r1.ownTimePercent);
            }
        });

        // Prepare the result array
        int topN = Math.min(5, rows.size());
        String[][] result = new String[topN][2];

        for (int i = 0; i < topN; i++) {
            Row row = rows.get(i);
            result[i][0] = row.method;
            result[i][1] = String.format("%.2f", row.ownTimePercent);
        }

        return result;
    }



    /**
     * Parses the table lines from a YourKit "Method list (CPU)" text
     * and returns a list of Row objects with (method, totalTimeMs, ownTimeMs, samples).
     */
    private static List<Row> parseYourKitCPUFile(String filePath) {
        List<Row> rows = new ArrayList<>();

        try (Scanner scanner = new Scanner(new File(filePath))) {

            boolean inTable = false;

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                // Skip empty lines or lines with only dashes/plus
                if (line.trim().isEmpty() || line.contains("Method list (CPU)")) {
                    continue;
                }
                // If line looks like +------------+ ... it's a border. Skip.
                if (line.trim().startsWith("+") && line.trim().endsWith("+")) {
                    // This might indicate start/end of table
                    // We'll say if we see one, we toggle inTable
                    // But we actually want to skip these border lines
                    inTable = !inTable;
                    continue;
                }

                // If not in table yet, skip
                //if (!inTable) {
                //    continue;
                //}

                // Now we have a table row, e.g.:
                // |  Bounce$Ball.bounce()                                                                               |  54,530   62%  |         50,772  |    5,541  |

                // Let's strip the leading/trailing "|" and split by "|"
                // But watch out for multiple spaces. We might do a simpler approach:
                String[] splitByPipe = line.split("\\|");
                // Typically, we'll see something like:
                // [ "", "  Bounce$Ball.bounce()                                                               ", "  54,530   62%  ", "      50,772  ", "    5,541  ", "" ]
                if (splitByPipe.length < 5) {
                    continue; // Not a valid row
                }

                // We'll map them:
                // 0 => "" (empty)
                // 1 => method
                // 2 => time ms + percent
                // 3 => own time ms
                // 4 => samples
                // last => ""

                String methodRaw = splitByPipe[1].trim().split("\\(")[0];
                String timeRaw   = splitByPipe[2].trim(); // e.g. "87,932   99%"
                String ownRaw    = splitByPipe[3].trim(); // e.g. "50,772"
                String samplesRaw= splitByPipe[4].trim(); // e.g. "5,541"

                Row row = new Row();
                row.method = cleanLambdaMethodName(methodRaw);

                // parse time ms from something like "54,530   62%"
                // We can just take the substring up to the first space or so
                // or split by space:
                // "54,530" or "87,932" might have commas
                String[] timeParts = timeRaw.split("\\s+");
                if (timeParts.length > 0) {
                    row.totalTimeMs = parseDoubleRemovingCommas(timeParts[0]);
                }

                // parse ownTimeMs, e.g. "50,772" -> 50772
                row.ownTimeMs = parseDoubleRemovingCommas(ownRaw);

                // parse samples, e.g. "5,541" -> 5541
                row.samples = (int) parseDoubleRemovingCommas(samplesRaw);

                rows.add(row);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return rows;
    }

    /**
     * Helper to parse a double from a string that may contain commas (e.g. "50,772").
     * If parse fails, returns 0.
     */
    private static double parseDoubleRemovingCommas(String s) {
        try {
            s = s.replace(",", "");
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0.0;
        }
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
     * A small data container for each row in the table.
     */
    static class Row {
        String method;
        double totalTimeMs;   // from "Time (ms)" column
        double ownTimeMs;     // from "Own Time (ms)" column
        int samples;          // from "Samples" column
        double ownTimePercent;  // We'll calculate later
    }
}
