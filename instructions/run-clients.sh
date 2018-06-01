#! /bin/bash
( cd $(dirname $0)
time java -server -Xmx1G -jar ./clients.jar)
