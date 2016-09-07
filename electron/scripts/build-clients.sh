#!/usr/bin/env bash

# Build and copy dependencies to the correct locations.

cd ./external-modules/CyFramework/
npm run build
cp build/CyFramework.js ../../webapp/ndex-login/CyFramework/
cp build/CyFramework.js ../../webapp/ndex/CyFramework/
cp build/CyFramework.js ../../webapp/ndex-save/CyFramework/
cd -

cd ./external-modules/NDExStore/
npm run build
cp build/NDExStore.js ../../webapp/ndex-login/NDExStore/
cp build/NDExStore.js ../../webapp/ndex/NDExStore/
cp build/NDExStore.js ../../webapp/ndex-save/NDExStore/
cd -

cd ./external-modules/NDExLogin/
npm run build
cp build/NDExLogin.js ../../webapp/ndex-login/NDExLogin/
cd -

cd ./external-modules/NDExPlugins/
npm run build
cp build/NDExPlugins.js ../../webapp/ndex/NDExPlugins/
cd -

cd ./external-modules/NDExValetFinder/
npm run build
cp build/NDExValetFinder.js ../../webapp/ndex/NDExValetFinder/
cd -

cd ./external-modules/NDExSave/
npm run build
cp build/NDExSave.js ../../webapp/ndex-save/NDExSave/
cd -
