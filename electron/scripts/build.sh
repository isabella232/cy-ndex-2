#!/usr/bin/env bash

# Build Script for all 5 platforms

START=`pwd`
echo "Started from Dir: $START"

rm -rf ./build
mkdir ./build

APP_NAME="CyNDEx-2"

# For Mac (Universal)

electron-packager . --platform=darwin --arch=x64 --overwrite --prune --ignore=external-modules --ignore=build --ignore=scripts --ignore=.idea --icon=icon256.icns --out=./build
cd ./build/${APP_NAME}-darwin-x64

echo 'Creating zip archive for Mac...'
pwd

tar -zcvf ./${APP_NAME}-mac.tar.gz ./${APP_NAME}.app
mv ./${APP_NAME}-mac.tar.gz ../
cd ${START}

echo 'Done: Mac version\n\n'
pwd

# For Linux: 32bit
electron-packager . --platform=linux --arch=ia32 --prune --ignore=external-modules --ignore=build --ignore=.idea --out=./build
cd ./build
tar -zcvf ${APP_NAME}-linux32.tar.gz ./${APP_NAME}-linux-ia32

echo "Done: Linux 32bit version"
cd ${START}
pwd

# For Linux: 64bit
electron-packager . --platform=linux --arch=x64 --prune --ignore=external-modules --ignore=build --ignore=.idea --out=./build
cd ./build
tar -zcvf ${APP_NAME}-linux64.tar.gz ./${APP_NAME}-linux-x64
echo "Done: Linux 64bit version"
cd ${START}
pwd


# For Windows: 32bit
electron-packager . --platform=win32 --arch=ia32 --prune --ignore=external-modules --ignore=build --ignore=.idea --out=./build
cd ./build
mv ${APP_NAME}-win32-ia32 ${APP_NAME}-win32
zip -r ${APP_NAME}-win32.zip ./${APP_NAME}-win32

echo "Done: Windows 32bit version"
cd ${START}
pwd


# For Windows: 64bit
electron-packager . --platform=win32 --arch=x64 --prune --ignore=external-modules --ignore=build --ignore=.idea --out=./build
cd ./build

mv ${APP_NAME}-win32-x64 ${APP_NAME}-win64
zip -r ${APP_NAME}-win64.zip ./${APP_NAME}-win64

echo "Done: Windows 64bit version"
cd ${START}
pwd

echo "Done!"
