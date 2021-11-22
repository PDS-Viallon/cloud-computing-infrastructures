#!/bin/bash

DIR=$(dirname "${BASH_SOURCE[0]}")

if [ $# -ne 2 ]; then
    echo "usage: $(basename '$0') host:port #calls";
    exit 1;
fi

mkdir -p client || true

clientID="client#$(( ( RANDOM % 1000 )  + 1 ))"
rm -f ${clientID}.dat
host="$1"
ncalls="$2"
for i in `seq 1 ${ncalls}`; do
    /bin/env time -a -o client/${clientID}.dat -f '%e' -- "${DIR}"/request.sh ${host} 2>&1 >/dev/null
done
rm -f *.pdf
