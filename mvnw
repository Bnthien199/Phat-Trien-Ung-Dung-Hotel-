#!/usr/bin/env sh

MAVEN_PROJECTBASEDIR="$(dirname "$0")"

exec java -cp "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar" -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" org.apache.maven.wrapper.MavenWrapperMain "$@"
