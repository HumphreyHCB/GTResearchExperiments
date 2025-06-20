#!/bin/bash

input_file="GTExperiment2bWithSafepoint/Readers/Output/Async_Summary.csv"

# Using awk to filter the required rows
awk -F, 'NR==1 || ($2 == 100 && ($4 == "Json" || $4 == "List" || $4 == "Havlak" || $4 == "Richards") && $5 == "NoSlowdown")' "$input_file"