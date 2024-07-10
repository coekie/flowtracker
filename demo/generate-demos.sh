#!/bin/bash
set -euo pipefail

# This script generates the demos website at https://flowtracker-demo.coekie.com/
# This should be ran from the directory where the output should be written; e.g. an empty dir.

error() {
  printf '\e[1;31mERROR\e[0m %s\n' "$*"
  exit 1
}

build_flowtracker() {
  pushd "$(dirname $0)/.."
  mvn -DskipTests clean package
  popd
}

set_env() {
  FT_JAR=$(realpath "$(dirname $0)/../flowtracker/target/flowtracker-0.0.1-SNAPSHOT.jar")
  if [ ! -e $FT_JAR ]; then
    error Cannot find $FT_JAR
  fi
  echo Using $FT_JAR
  FT_JVMOPTS="$(java -jar $FT_JAR jvmopts)"
}

run_petclinic() {
  echo Demo: spring-petclinic
  if [ ! -e spring-petclinic ]; then
    git clone https://github.com/spring-projects/spring-petclinic.git
  fi
  cd spring-petclinic
  # stick to a specific sha to keep the demos reproducible
  git checkout a35189a9c56eb1d813890fe33be2e67c9ff43636
  ./mvnw dependency:sources

  # demo with in-memory database
  ./mvnw integration-test -Dtest=PetClinicIntegrationTests#testOwnerDetails -DargLine="-javaagent:$FT_JAR=webserver=false;trackCreation;snapshotOnExit=../petclinic-snapshot.zip $FT_JVMOPTS"
  # sanity check that the link to the template worked
  unzip -p ../petclinic-snapshot.zip | grep -q '"target","classes","templates","fragments","layout.html"' || error petclinic tracking of template failed

  # demo with mysql
  ./mvnw integration-test -Dspring-boot.run.profiles=mysql -Dtest=MySqlIntegrationTests#testOwnerDetails -DargLine="-javaagent:$FT_JAR=webserver=false;trackCreation;snapshotOnExit=../petclinic-mysql-snapshot.zip $FT_JVMOPTS"
  # sanity check that the link to the template worked
  unzip -p ../petclinic-mysql-snapshot.zip | grep -q '"target","classes","templates","fragments","layout.html"' || error petclinic-mysql tracking of template failed

  cd ..
}

run_javac() {
  echo Demo: javac
  echo 'class HelloWorld {
    public static void main(String[] args) {
      System.out.println("Hello, World!");
    }
  }' > HelloWorld.java
  # eagerly instrumenting com.sun.tools.javac.jvm.* improves results.
  # to pass JVM arguments to javac, they are prefixed with -J
  FT_JAVAC_OPTS="-J-javaagent:$FT_JAR=webserver=false;snapshotOnExit=javac-snapshot.zip;eager=+com.sun.tools.javac.jvm.* "$(for s in $FT_JVMOPTS; do echo -n "-J$s "; done)
  javac $FT_JAVAC_OPTS HelloWorld.java
}

run_simple() {
  OUT=$(pwd)
  cd "$(dirname $0)"
  CP=$(mvn dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=/dev/stdout -q):target/classes/
  for demo in AsmDemo GsonDemo HelloWorld JdkHttpDemo ProtobufDemo SerializationDemo SnakeYamlDemo; do
    echo Demo: $demo
    java -cp $CP "-javaagent:$FT_JAR=webserver=false;trackCreation;snapshotOnExit=$OUT/$demo-snapshot.zip" $FT_JVMOPTS demo.$demo > /dev/null
  done
  cd -
}

prepare_git() {
  if [ ! -e pages ]; then
    git clone --branch pages git@github.com:coekie/flowtracker-demo.git pages
  fi
  cd pages
  git fetch
  git checkout pages
  git reset --hard origin/main
  for f in ../*-snapshot.zip; do
    demo=$(basename -s -snapshot.zip $f)
    rm -rf $demo
    unzip -q $f
    mv snapshot $demo
  done
  if [ ! -e GsonDemo ] || [ ! -e petclinic ]; then
    # don't publish if not all demos ran
    error Sanity check failed, missing demo
  fi
  git add .
  git commit -m 'Generated'
  cd ..
}

publish_live() {
  cd pages
  git push -f
  cd ..
}

build_flowtracker
set_env
run_petclinic
run_javac
run_simple
prepare_git

if [ "${1:-}" == "live" ]; then
  publish_live
fi