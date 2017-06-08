#!/usr/bin/env bash

echo "Building webapp..."

rm -rf ./webapp/build
rm -rf ./src/main/resources/cyndex-2

cd webapp
npm install
npm run build
cd -

# Copy the file into resource
mkdir ./src/main/resources/cyndex-2
cp -R ./webapp/build/* ./src/main/resources/cyndex-2/

echo "-------------- Done! Frontend code is in src/main/resources/webapp -------------------"
