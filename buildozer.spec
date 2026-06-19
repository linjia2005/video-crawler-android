[app]

# (str) Title of your application
title = Video Crawler Client

# (str) Package name
package.name = videocrawler

# (str) Package domain (needed for Android/ios packaging)
package.domain = org.example

# (str) Source code where the main.py lives
source.dir = .
source.include_exts = py,png,jpg,kv,atlas,ttf

# (list) Source files to exclude
source.exclude_exts = spec

# (list) List of custom git revision, even when tag is not set
version.regex = __version__ = ['"](.*)['"]
version.filename = %(source.dir)s/main.py

# (str) Application versioning (method 1)
version = 1.0.0

# (str) Application versioning (method 2)
# version.regex = __version__ = ['"](.*)['"]
# version.filename = %(source.dir)s/main.py

# (list) Application requirements
#   python3 kivy are required
requirements = python3,kivy>=2.1.0,requests

# (str) Custom source folders
# dotenv =

# (str) Presplash of the application
# presplash.filename = %(source.dir)s/data/presplash.png

# (str) Icon of the application
# icon.filename = %(source.dir)s/data/icon.png

# (str) Supported orientation (one of landscape, sensorLandscape, portrait or all)
orientation = portrait

# (list) List of service to declare
# services =

# OSX / iOS specific
osx.python_version = 3
osx.kivy_version = 2.1.0

# Android specific
android.api = 33
android.minapi = 21
android.sdk = 33
android.ndk = 25b
android.ndk_path =
android.sdk_path =
android.android_api = 33
android.google_apikey =
android.private_bundle_install = False
android.entrypoint = videocrawler_client.py
android.wakelock = False
android.verify_local_ssl = False
android.add_src =
android.add_src_filter =
android.extra_jars =
android.add_jars =
android.add_aars =
android.add_libs_armeabi =
android.add_libs_armeabi_v7a =
android.add_libs_arm64_v8a =
android.add_libs_x86 =
android.add_libs_x86_64 =
android.gradle_dependencies =
android.gradle_custom_command =
android.manifest_placeholders =
android.signed = False

# (list) Android architecture list
android.archs = arm64-v8a

# (str) Python-for-android (p4a) specific branch / commit
# p4a.branch = develop
# p4a.commit = HEAD

# (str) Output filename
android.filename = VideoCrawlerClient-%(version)s

# (str) Output dir for the APK
android.output_dir = ./apk

# (str) Permission list
android.permissions = INTERNET,ACCESS_NETWORK_STATE,ACCESS_WIFI_STATE

# (bool) Indicate if the application is a library or not
android.library_receivers = False

# (str) Meta-data to add to AndroidManifest.xml
# android.meta_data =

# (list) Libraries (libs) to add to the Android project
# android.add_libs =

# (bool) Indicate if the application should be fullscreen
android.fullscreen = 0

# (bool) If True, then the screen will stay on
# android.keep_screen_on = False

# (bool) If True, the project will be compiled and packaged with the presplash tool
# android.presplash_size_limit = 64

# (str) The Android logcat filters to use
# android.logcat_filters =

# (int) Android logging level (0-5)
# android.log_level = 0

# (bool) Copy library instead of making a libs directory
# android.copy_libs = True

# (str) The Android arch to build for (choices: armeabi-v7a, arm64-v8a, x86, x86_64)
# android.arch = arm64-v8a

# Windows specific
# win.title = My application
# win.icon = data/icon.ico

# iOS specific
# ios.codesign.allowed = False
# ios.dep_licenses =
# ios.use_arc = True
# ios.use_frameworks = False
# ios.utf8 = True

# macOS specific
# macos.title = My application
# macos.icon = data/icon.ico

# Linux specific
# linux.title = My application
# linux.icon = data/icon.ico

# (str) Prefix path for external dependencies
# (str) Where to store the build results
# log_dir = ./logs

# (bool) Show full Python output (useful for debugging)
log_level = 2

# (int) Number of parallel build jobs
# jobs = 4

# (str) Where to store the downloaded Android SDK
# android.sdk_path = %(buildozer_dir)s/android/platform/android-sdk

# (str) Where to store the downloaded Android NDK
# android.ndk_path = %(buildozer_dir)s/android/platform/android-ndk

# (str) Android NDK version (default: 19c)
# android.ndk = 19c

# (bool) Accept the SDK license automatically
android.accept_sdk_license = True
