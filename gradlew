#!/bin/sh
# Gradle wrapper script

# Determine the Java command to use
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/bin/java" ] ; then
        JAVACMD="$JAVA_HOME/bin/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set"
fi

# Gradle wrapper jar
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Run Gradle
exec "$JAVACMD" $JAVA_OPTS $GRADLE_OPTS "-Dorg.gradle.appname=$APP_BASE_NAME" -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
