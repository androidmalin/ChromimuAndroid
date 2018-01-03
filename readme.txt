

使用
https://chromium.googlesource.com/chromium/src.git/+/65.0.3289.0/components/cronet/android/build_instructions.md

1.
cd /onet/malin/chromium/src/

2.clean
gn clean out/Default/
rm out/Default/build.ninja
rm out/Default/build.ninja.d

3.delete
rm -rf out/Release/

4.cronet release
./components/cronet/tools/cr_cronet.py gn --release

5.add some config to out/Release/args.gn
echo "android_ndk_root = \"/home/malin/ndk/android-ndk-r12b\"" >> out/Release/args.gn
echo "android_ndk_version = \"r12b\"" >> out/Release/args.gn
echo "android_sdk_root = \"/home/malin/sdk\"" >> out/Release/args.gn
echo "android_sdk_platform_version = \"27\"" >> out/Release/args.gn
echo "android_sdk_build_tools_version = \"27.0.3\"" >> out/Release/args.gn

6.build
ninja -C out/Release cronet_package
