#!/usr/bin/env bash

# Directory containing all YourKit .snapshot files
INPUT_DIR="GTExperiment3SelectiveBlockSlowdown/YourKitData"

# Directory to store the exported text files
OUTPUT_DIR="GTExperiment3SelectiveBlockSlowdown/YourKitDataTXT"

# Path to your specific Java 17 binary
JAVA17="/home/hb478/Downloads/labsjdk-ce-17.0.9+6-jvmci-23.0-b19-linux-amd64/labsjdk-ce-17.0.9-jvmci-23.0-b19/bin/java"

# Path to your yourkit.jar (adjust as needed)
YKP_JAR="/home/hb478/ProgramFiles/YourKit-JavaProfiler-2024.9/lib/yourkit.jar"

# Create output directory if not present
mkdir -p "$OUTPUT_DIR"

# Loop over all .snapshot files in INPUT_DIR
for snapshot in "$INPUT_DIR"/*.snapshot; do
  [ -e "$snapshot" ] || continue  # no .snapshot files found => skip

  # Extract base name (e.g. "100_1_Bounce_NoSlowdown-2025-02-09-shutdown")
  base="$(basename "$snapshot" .snapshot)"

  # We'll rename "Method-list-CPU.txt" to "$base_Method-list-CPU.txt"
  # That means final path will be e.g. "GTExperiment2/YourKitDataTXT/100_1_Bounce_NoSlowdown-2025-02-09-shutdown_Method-list-CPU.txt"
  renamed_cpu_txt="$OUTPUT_DIR/${base}_Method-list-CPU.txt"

  # If it's already exported/renamed, skip
  if [ -f "$renamed_cpu_txt" ]; then
    echo "Skipping $snapshot: already have $renamed_cpu_txt"
    continue
  fi

  echo "Exporting $snapshot -> $OUTPUT_DIR"

  # Run the export with your specific Java path
  #   -Dexport.method.list.cpu => export CPU hotspots
  #   -Dexport.txt => text format
  #   -export => yourkit export mode
  # This typically generates "Method-list-CPU.txt" in the output directory
  "$JAVA17" \
    -Dexport.method.list.cpu \
    -Dexport.txt \
    -jar "$YKP_JAR" \
    -export "$snapshot" "$OUTPUT_DIR"

  # After that, we expect "Method-list-CPU.txt" in OUTPUT_DIR
  # Let's rename it if it exists
  generated="$OUTPUT_DIR/Method-list-CPU.txt"

  if [ -f "$generated" ]; then
    mv "$generated" "$renamed_cpu_txt"
    echo "Renamed $generated -> $renamed_cpu_txt"
  else
    echo "Warning: $generated not found! Check if YourKit used a different filename."
  fi

done

echo "Done exporting all YourKit snapshots!"
