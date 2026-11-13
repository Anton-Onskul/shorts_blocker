#!/bin/bash
# Сборка APK без Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner.
# Нужны: JDK 17+, build-tools 34 (aapt2, d8, zipalign, apksigner) и platforms/android-33/android.jar.
# Пути задаются переменными BT и ANDROID_JAR.
set -euo pipefail
cd "$(dirname "$0")"

BT="${BT:?укажите BT=путь/к/build-tools/34.0.4}"
ANDROID_JAR="${ANDROID_JAR:?укажите ANDROID_JAR=путь/к/platforms/android-33/android.jar}"
SRC=app/src/main
OUT=app/build/apk
KS=signing/shortsblocker.jks
KS_PASS="${KS_PASS:-shortsblocker}"
VERSION_CODE="${VERSION_CODE:-2}"
VERSION_NAME="${VERSION_NAME:-1.1}"

rm -rf "$OUT" && mkdir -p "$OUT"/{res,gen,classes,dex}

echo "== 1. Тесты логики"
./run_tests.sh

echo "== 2. Ресурсы (aapt2)"
"$BT/aapt2" compile --dir "$SRC/res" -o "$OUT/res/res.zip"
# В исходном манифесте нет package (его задаёт namespace в Gradle) — подставляем для aapt2.
sed 's|<manifest xmlns:android="http://schemas.android.com/apk/res/android"|& package="com.shortsblock.app"|' \
  "$SRC/AndroidManifest.xml" > "$OUT/AndroidManifest.xml"
"$BT/aapt2" link -o "$OUT/base.apk" -I "$ANDROID_JAR" \
  --manifest "$OUT/AndroidManifest.xml" --java "$OUT/gen" \
  --min-sdk-version 29 --target-sdk-version 33 \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" \
  "$OUT/res/res.zip"

echo "== 3. Компиляция Java (ошибки и предупреждения = провал)"
find "$SRC/java" "$OUT/gen" -name '*.java' > "$OUT/sources.txt"
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$ANDROID_JAR:$BT/core-lambda-stubs.jar" \
  -Xlint:all -Xlint:-options -Werror -d "$OUT/classes" @"$OUT/sources.txt"

echo "== 4. Dex (d8)"
java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 29 \
  --lib "$ANDROID_JAR" --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')

echo "== 5. Упаковка, выравнивание, подпись"
cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && zip -q -X ../unsigned.apk classes.dex)
"$BT/zipalign" -p -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
if [ ! -f "$KS" ]; then
  mkdir -p signing
  keytool -genkeypair -keystore "$KS" -storepass "$KS_PASS" -keypass "$KS_PASS" \
    -alias shortsblocker -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Shorts Blocker" >/dev/null
fi
java -jar "$BT/lib/apksigner.jar" sign --ks "$KS" --ks-pass "pass:$KS_PASS" \
  --out "$OUT/ShortsBlocker.apk" "$OUT/aligned.apk"

echo "== 6. Проверка подписи"
java -jar "$BT/lib/apksigner.jar" verify --verbose "$OUT/ShortsBlocker.apk" | head -6
echo "Готово: $OUT/ShortsBlocker.apk"
