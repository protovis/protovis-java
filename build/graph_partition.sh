#!/bin/bash
LIBPATH=$(dirname $0)/lib
CLASSPATH=$LIBPATH/gluegen-rt.jar:$LIBPATH/jogl.jar:$LIBPATH/google-collect-1.0-rc4.jar:$LIBPATH/protovis.jar:$LIBPATH/protovis-examples.jar

java -classpath $CLASSPATH -Djava.library.path=$LIBPATH \
     edu.stanford.vis.examples.GraphPartition