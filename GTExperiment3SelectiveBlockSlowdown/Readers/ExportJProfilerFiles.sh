#!/usr/bin/env bash

# Directory containing your JProfiler .jps snapshots
INPUT_DIR="GTExperiment3SelectiveBlockSlowdown/JProfilerData"

# Directory where we want to store the exported XML files
OUTPUT_DIR="GTExperiment3SelectiveBlockSlowdown/JProfilerDataXMLNoSlowdown"

# Create the output directory if it does not exist
mkdir -p "$OUTPUT_DIR"

# Loop over all .jps files in INPUT_DIR
for jpsfile in "$INPUT_DIR"/*.jps; do
  # If there are no .jps files, skip
  [ -e "$jpsfile" ] || continue

  # Extract the base name (e.g. "100_3_Queens_Slowdown" from "100_3_Queens_Slowdown.jps")
  base="$(basename "$jpsfile" .jps)"

  # Construct path for the exported XML (same base name, .xml extension)
  xmlfile="$OUTPUT_DIR/${base}.xml"

 # If the .xml file already exists, skip
  if [ -f "$xmlfile" ]; then
    echo "Skipping $jpsfile because $xmlfile already exists."
    continue
  fi
  
  echo "Exporting $jpsfile -> $xmlfile"

  # Export the HotSpots view in XML format
  # You can add more options (e.g., -viewfilters=..., -aggregation=..., etc.) as needed
  /home/hb478/jprofiler14/bin/jpexport \
    "$jpsfile" \
    HotSpots -format=xml \
    "$xmlfile"
done

echo "Done exporting all JProfiler .jps snapshots to XML!"