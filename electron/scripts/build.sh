#!/usr/bin/env bash

# Build Script for all 3 platforms

START=`pwd`
echo "Started from Dir: $START"

rm -rf ./build
mkdir ./build

# For Mac (Universal)
electron-packager . --platform=darwin --arch=x64 --overwrite --prune --ignore=external-modules --ignore=build --ignore=.idea --icon=icon256.icns --out=./build
cd ./build/NDEx-Valet-darwin-x64

cd ./NDEx-Valet.app/Contents/Resources/app
npm install xmlbuilder
cd -

echo 'Creating zip archive for Mac...'
pwd

tar -zcvf ./NDEx-Valet-mac.tar.gz ./NDEx-Valet.app
rm -f ../../../src/main/resources/ndex/NDEx-Valet-mac.tar.gz
cp ./NDEx-Valet-mac.tar.gz ../../../src/main/resources/ndex/
cd ${START}

echo 'Done: Mac version\n\n'
pwd

# For Linux: 32bit
electron-packager . --platform=linux --arch=ia32 --prune --ignore=external-modules --ignore=build --ignore=.idea --out=./build
cd ./build

cd ./NDEx-Valet-linux-ia32/resources/app
npm install xmlbuilder
cd -

tar -zcvf NDEx-Valet-linux32.tar.gz ./NDEx-Valet-linux-ia32
rm -f ../../src/main/resources/ndex/NDEx-Valet-linux32.tar.gz
cp NDEx-Valet-linux32.tar.gz ../../src/main/resources/ndex/

echo "Done: Linux 32bit version"
cd ${START}
pwd

# For Linux: 64bit
electron-packager . --platform=linux --arch=x64 --prune --ignore=external-modules --ignore=build --ignore=.idea --out=./build
cd ./build

cd ./NDEx-Valet-linux-x64/resources/app
npm install xmlbuilder
cd -

tar -zcvf NDEx-Valet-linux64.tar.gz ./NDEx-Valet-linux-x64
rm -f ../../src/main/resources/ndex/NDEx-Valet-linux64.tar.gz
cp NDEx-Valet-linux64.tar.gz ../../src/main/resources/ndex/
echo "Done: Linux 64bit version"
cd ${START}
pwd

# For Windows: 32bit
electron-packager . --platform=win32 --arch=ia32 --prune --ignore=external-modules --ignore=build --ignore=.idea --out=./build
cd ./build

cd ./NDEx-Valet-win32-ia32/resources/app
npm install xmlbuilder
cd -

mv NDEx-Valet-win32-ia32 NDEx-Valet-win32
zip -r NDEx-Valet-win32.zip ./NDEx-Valet-win32
rm -f ../../src/main/resources/ndex/NDEx-Valet-win32.zip
cp NDEx-Valet-win32.zip ../../src/main/resources/ndex/

echo "Done: Windows 32bit version"
cd ${START}
pwd


# For Windows: 64bit
electron-packager . --platform=win32 --arch=x64 --prune --ignore=external-modules --ignore=build --ignore=.idea --out=./build
cd ./build

cd ./NDEx-Valet-win32-x64/resources/app
npm install xmlbuilder
cd -

mv NDEx-Valet-win32-x64 NDEx-Valet-win64
zip -r NDEx-Valet-win64.zip ./NDEx-Valet-win64
rm -f ../../src/main/resources/ndex/NDEx-Valet-win64.zip
cp NDEx-Valet-win64.zip ../../src/main/resources/ndex/

echo "Done: Windows 64bit version"
cd ${START}
pwd
echo "Done!"