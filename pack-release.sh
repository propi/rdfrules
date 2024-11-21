#!/bin/bash

VERSION=$(cat ./.version)

mkdir build-release
mkdir build-release/lib
mkdir build-release/bin
mkdir build-release/webapp

cp ./http/target/pack/lib/* build-release/lib
cp ./http/target/pack/bin/* build-release/bin
cp -R ./gui/webapp/* build-release/webapp
cp ./gui/target/scala-2.13/gui-opt.js build-release/webapp/js

sed -i -e "s/DEV/v$VERSION/g" ./build-release/webapp/index.html
sed -i -e "s/\.\.\/target\/scala-2.13\/gui-fastopt\/main.js/js\/gui-opt.js/g" ./build-release/webapp/index.html
sed -i -e "s/(\.js|\.css)/\0?v=$VERSION/g" ./build-release/webapp/index.html

cd build-release || exit
zip -r "../rdfrules-$VERSION.zip" *