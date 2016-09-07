#!/usr/bin/env bash

cd ./external-modules

pwd

for dir in $(ls -d */)
do
    echo "Updating: $dir"
    cd $dir
    npm update && npm install
    cd -
done

