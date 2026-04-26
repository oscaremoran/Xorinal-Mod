#!/bin/bash
# Build the Xorinal Java mod classes (no Gradle).
# Outputs compiled .class files to build/classes/, ready to be packaged into the mod zip.
set -e

MINDUSTRY_JAR="/Users/seanmoran/Library/Application Support/Steam/steamapps/common/Mindustry/Mindustry.app/Contents/Resources/desktop.jar"

if [ ! -f "$MINDUSTRY_JAR" ]; then
    echo "ERROR: Mindustry desktop.jar not found at: $MINDUSTRY_JAR"
    exit 1
fi

rm -rf build/classes
mkdir -p build/classes

find java-src -name "*.java" > build/sources.txt
javac --release 17 -cp "$MINDUSTRY_JAR" -d build/classes @build/sources.txt

echo "Compiled $(find build/classes -name '*.class' | wc -l | tr -d ' ') classes."
