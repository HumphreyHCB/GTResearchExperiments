#!/usr/bin/env bash

# Top-level directory
CONFIGS_DIR="JProfilerConfigs"

# List of benchmarks
BENCHMARKS=(
  "Richards"
  "Json"
  "CD"
  "Bounce"
  "List"
  "Mandelbrot"
  "NBody"
  "Permute"
  "Queens"
  "Sieve"
  "Towers"
  "DeltaBlue"
  "Storage"
  "Havlak"
)

# Versions and modes
VERSIONS=(50 100 150)
MODES=("NoSlowdown" "Slowdown")

# Create the top-level directory if it doesn't exist
mkdir -p "${CONFIGS_DIR}"

# Loop over config directories (config1..config10)
for config_num in {1..10}; do
  # Create the config directory
  config_dir="${CONFIGS_DIR}/config${config_num}"
  mkdir -p "${config_dir}"

  # For each benchmark
  for benchmark in "${BENCHMARKS[@]}"; do
    # For each version
    for version in "${VERSIONS[@]}"; do
      # For each mode
      for mode in "${MODES[@]}"; do

        # Construct filename for the .xml file
        filename="${benchmark}_${version}_${mode}.xml"
        filepath="${config_dir}/${filename}"

        # Create the XML file
        cat <<EOF > "${filepath}"
<?xml version="1.0" encoding="UTF-8"?>
<config version="13.0.5">
  <sessions>
    <session id="120" name="New session" jvmConfigurationId="100" recordArrayAlloc="false" methodCallRecordingType="async" samplingFrequency="10" autoTuneInstrumentation="false">
      <filters>
        <group type="exclusive" name="Default excludes" template="defaultExcludes" />
      </filters>
      <triggers>
        <jvmStart>
          <actions>
            <startRecording>
              <cpu enabled="true" />
            </startRecording>
          </actions>
        </jvmStart>
        <jvmStop>
          <actions>
            <saveSnapshot file="/home/hb478/repos/GTResearchExperiments/GTExperiment2bADHOCWithSafepointANDMissingDebug/JProfilerData/${version}_${config_num}_${benchmark}_${mode}" />
          </actions>
        </jvmStop>
      </triggers>
    </session>
  </sessions>
</config>
EOF

        echo "Created: ${filepath}"
      done
    done
  done
done

echo "All config files have been created successfully."