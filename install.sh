#!/data/data/com.termux/files/usr/bin/bash
# usage: ./install.sh [path/to/app-debug.apk]
TV=192.168.10.195:5555
APK=${1:-$HOME/storage/downloads/app-debug.apk}
adb connect $TV
adb -s $TV install -r "$APK" || exit 1
adb -s $TV shell cmd package set-home-activity com.evans.launcher/.MainActivity
adb -s $TV shell input keyevent KEYCODE_HOME
echo "Home is now:"
adb -s $TV shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1
# rollback: adb -s $TV uninstall com.evans.launcher   (Home falls back to Projectivy)
