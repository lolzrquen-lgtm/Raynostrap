#!/bin/sh
#
# Gradle start up script for UN*X
#
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
GRADLE_OPTS=""

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
  CYGWIN* )    cygwin=true  ;;
  Darwin* )    darwin=true  ;;
  MSYS* | MINGW* ) msys=true ;;
  NONSTOP* )   nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine JAVA_HOME
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

if [ ! -x "$JAVACMD" ] ; then
    die "ERROR: JAVA_HOME is not set and no 'java' command could be found."
fi

# Find the application home
APP_HOME="`pwd -P`"
WRAPPER_LAUNCHER=org.gradle.wrapper.GradleWrapperMain

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
  "-Dorg.gradle.appname=$APP_BASE_NAME" \
  -classpath "$CLASSPATH" \
  $WRAPPER_LAUNCHER \
  "$@"
