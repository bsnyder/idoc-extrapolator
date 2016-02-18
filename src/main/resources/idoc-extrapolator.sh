#!/bin/bash

if [ $# -eq 0 ]; then
    # Args are empty so just print the help
    java -jar ./target/idoc-extrapolator-1.0-SNAPSHOT-jar-with-dependencies.jar
else
    # Pass args to the app
    java -jar ./target/idoc-extrapolator-1.0-SNAPSHOT-jar-with-dependencies.jar -t $1 -d $2 -f $3
fi
