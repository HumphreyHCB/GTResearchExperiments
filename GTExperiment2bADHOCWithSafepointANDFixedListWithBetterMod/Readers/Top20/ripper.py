#!/usr/bin/env python3

import pandas as pd

# Path to the CSV file (adjust as needed)
toolName = "JProfiler"
CSV_FILE = "/home/hb478/repos/GTResearchExperiments/GTExperiment2bWithSafepoint/Readers/Top20/Output/"+toolName+"_Summary.csv"

def main():
    # 1. Read CSV
    df = pd.read_csv(CSV_FILE)
    
    # 2. Filter for rows where:
    #    SlowdownSpeed == "100"
    #    IsSlowdown == "NoSlowdown"
    #    Benchmark in ["Json", "List", "Richards", "Havlak"]
    desired_benchmarks = ["Json", "List", "Richards", "Havlak"]
    
    filtered = df[
        (df["SlowdownSpeed"].astype(str) == "100") &
        (df["IsSlowdown"] == "NoSlowdown") &
        (df["Benchmark"].isin(desired_benchmarks))
    ]
    
    # 3. Print or Save
    if filtered.empty:
        print("No matching rows found.")
    else:
        # Print matching rows to console
        print(filtered.to_string(index=False))
        
        # Optionally, save to a new CSV
        filtered.to_csv(toolName+"_NoSlowdownOutput.csv", index=False)
        print("Filtered rows saved to FilteredOutput.csv")


if __name__ == "__main__":
    main()
