import re
import csv

# Input file containing the fixed-width table text.
input_file = "NewCD.out"
# Output CSV file.
output_file = "Data/JustSlowdownCD.csv"

data_rows = []

with open(input_file, "r") as f:
    lines = f.readlines()

for line in lines:
    line = line.strip()
    # Skip empty lines or lines that are just dashes.
    if not line or set(line) == {"-"}:
        continue
    # Skip the header line if it contains the word "Benchmark"
    if "Benchmark" in line:
        continue
    # Split the line on two or more whitespace characters.
    tokens = re.split(r'\s{2,}', line)
    # We expect 8 tokens from a valid data row:
    # [Benchmark, Executor, Suite, Extra, Core, Size, Var, Mean(#Samples,Mean)??]
    # For our purposes, we want:
    #   Benchmark = tokens[0]
    #   Suite     = tokens[2]
    #   Var       = tokens[5]
    #   Mean      = tokens[7]
    if len(tokens) >= 8:
        benchmark = tokens[0]
        suite = tokens[2]
        var = tokens[5]
        mean = tokens[7]
        data_rows.append([benchmark, suite, var, mean])

# Write the output CSV.
with open(output_file, "w", newline="") as csvfile:
    writer = csv.writer(csvfile)
    # Write header.
    writer.writerow(["Benchmark", "Suite", "Var", "Mean"])
    writer.writerows(data_rows)

print(f"CSV written to {output_file}")
