#!/usr/bin/env bash

echo "Building webapp..."

rm -rf ./webapp/build
rm -rf ./src/main/resources/cyndex-2

cd webapp

# Make sure it is up-to-date
echo 'pulling changes from master branch...'

git checkout master
git pull

npm install
npm run build
cd -

# Copy the file into resource
mkdir ./src/main/resources/cyndex-2
cp -R ./webapp/build/* ./src/main/resources/cyndex-2/

echo "-------------- Done! Frontend code is in src/main/resources/webapp -------------------"
