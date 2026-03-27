#!/bin/sh

# Minimal gradlew script

# Find java
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

if ! command -v "$JAVACMD" >/dev/bin/null 2>&1; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
    exit 1
fi

# Run gradle wrapper
exec "$JAVACMD" -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"
