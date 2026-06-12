#!/bin/sh
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec "$JAVACMD" "$@" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$APP_ARGS"

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

warn () {
    echo "$*"
} >&2

# OS specific support
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MSYS* | MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

APP_HOME=`pwd -P`
APP_BASE_NAME=`basename "$0"`

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

JAVACMD="java"

# Split up the JVM_OPTS And GRADLE_OPTS values into an array
set -- \
  "-Dorg.gradle.appname=$APP_BASE_NAME" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"

exec "$JAVACMD" "$@"