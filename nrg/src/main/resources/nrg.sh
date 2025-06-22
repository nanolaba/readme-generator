#!/bin/bash
# export JAVA_HOME=/usr/lib/jvm/java-8-openjdk
# export PATH=$JAVA_HOME/bin:$PATH
java -Dfile.encoding=UTF8 -Xms128m -Xmx1024m -cp "$(dirname "$0")/*" com.nanolaba.nrg.NRG "$@"