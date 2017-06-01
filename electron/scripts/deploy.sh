#!/usr/bin/env bash

# Upload the archive to the release page

cd ./build

scp ./*.zip kono@grenache.ucsd.edu://cellar/users/kono/public_html/ci/app/cyndex2
scp ./*.gz kono@grenache.ucsd.edu://cellar/users/kono/public_html/ci/app/cyndex2
