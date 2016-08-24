#!/usr/bin/env bash

cd ./external-modules

for dir in $(ls -d */)
do
    cd $dir
    npm update && npm install
    cd -
done

