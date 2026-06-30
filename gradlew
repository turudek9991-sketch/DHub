#!/bin/sh
# Gradle wrapper script
GRADLE_VERSION=8.4

# Find JAVA_HOME
if [ -z "$JAVA_HOME" ]; then
    JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
fi

# Download gradle if not present
GRADLE_HOME="$HOME/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin"
GRADLE_ZIP="$GRADLE_HOME/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_BIN="$GRADLE_HOME/gradle-${GRADLE_VERSION}/bin/gradle"

if [ ! -f "$GRADLE_BIN" ]; then
    mkdir -p "$GRADLE_HOME"
    curl -L "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$GRADLE_ZIP"
    unzip -q "$GRADLE_ZIP" -d "$GRADLE_HOME"
fi

exec "$GRADLE_BIN" "$@"
