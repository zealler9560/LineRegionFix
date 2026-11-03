#!/bin/bash
set -e
cd "$(dirname "$0")"
export JAVA_HOME=~/jdk
export PATH=~/jdk/bin:$PATH
BT=~/android-sdk/build-tools/35.0.0
AJAR=~/android-sdk/platforms/android-35/android.jar

rm -rf build
mkdir -p build/classes build/dex

echo "== aapt2 compile res =="
"$BT/aapt2" compile --dir res -o build/res.zip

echo "== javac =="
javac -source 8 -target 8 -encoding UTF-8 \
  -cp "$AJAR:libs/api-82.jar" \
  -d build/classes $(find src -name '*.java')

echo "== d8 =="
"$BT/d8" --lib "$AJAR" --min-api 29 --output build/dex \
  build/classes/io/muse/lineregionfix/*.class

echo "== aapt2 link =="
"$BT/aapt2" link -I "$AJAR" \
  --manifest AndroidManifest.xml \
  -A assets \
  build/res.zip \
  -o build/app-unsigned.apk

echo "== add classes.dex =="
(cd build/dex && "$JAVA_HOME/bin/jar" uf ../app-unsigned.apk classes.dex)

echo "== zipalign =="
"$BT/zipalign" -f -p 4 build/app-unsigned.apk build/app-aligned.apk || cp build/app-unsigned.apk build/app-aligned.apk

echo "== keystore =="
if [ ! -f build/debug.keystore ]; then
  keytool -genkeypair -keystore build/debug.keystore -alias androiddebugkey \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass android -keypass android -dname "CN=LineRegionFix,O=Muse,C=CN"
fi

echo "== apksigner =="
"$BT/apksigner" sign --ks build/debug.keystore \
  --ks-pass pass:android --key-pass pass:android \
  --out LineRegionFix.apk build/app-aligned.apk

echo "== verify =="
"$BT/apksigner" verify LineRegionFix.apk && echo SIGN_OK
"$BT/aapt2" dump badging LineRegionFix.apk | head -4
unzip -l LineRegionFix.apk
ls -la LineRegionFix.apk
