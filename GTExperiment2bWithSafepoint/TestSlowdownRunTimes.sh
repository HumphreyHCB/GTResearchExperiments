#!/bin/bash
export PATH=/home/hburchell/Repos/graal-dev/mx:$PATH
exec /home/hb478/repos/graal-instrumentation/vm/latest_graalvm_home/bin/java \
  -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -Djdk.graal.IsolatedLoopHeaderAlignment=0 -Djdk.graal.LoopHeaderAlignment=0 -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary -XX:-TieredCompilation -XX:-BackgroundCompilation -Djdk.graal.DisableCodeEntryAlignment=true \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  "$@"