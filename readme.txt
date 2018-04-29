

module
cronet_api,
cronet_impl_common_java,
cronet_impl_native_java,
cronet_impl_platform_java
分别是从对应的src.jar中解压复制出来的.
Cronet_Release/cronet/cronet_api-src.jar
Cronet_Release/cronet/cronet_impl_common_java-src.jar
Cronet_Release/cronet/cronet_impl_native_java-src.jar
Cronet_Release/cronet/cronet_impl_platform_java-src.jar


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



basepath = "/onet/malin/chromium/src"

gopath ="/home/malin/github_demo/ChromimuAndroid"

cp -R $basepath/base/android/java/src/org/ $gopath/lib_base_java/src/main/java

cp -R $basepath/components/cronet/android/api/src/org $gopath/lib_cronet_api/src/main/java
cp -R $basepath/src/components/cronet/android/api/res/raw $gopath/lib_cronet_api/src/main/res

cp -R /onet/malin/chromium/src/components/cronet/android/java/src/org /home/malin/github_demo/ChromimuAndroid/lib_cronet_impl_all/src/main/java

cp -R /onet/malin/chromium/src/net/android/java/src/org /home/malin/github_demo/ChromimuAndroid/lib_net_java/src/main/java

cp -R /onet/malin/chromium/src/url/android/java/src/org /home/malin/github_demo/ChromimuAndroid/lib_url_java/src/main/java


cp -R /onet/malin/chromium/src/base/android/java/templates /home/malin/github_demo/ChromimuAndroid/lib_base_java/src/main/


===================
https://stackoverflow.com/a/42130853/3326683
https://www.jianshu.com/p/08f0701e86de

 2010  cp LibraryProcessType.java /home/malin/github_demo/ChromimuAndroid/lib_base_java/src/main/java/org/chromium/base/library_loader/
 2012  cp LibraryLoadFromApkStatusCodes.java /home/malin/github_demo/ChromimuAndroid/lib_base_java/src/main/java/org/chromium/base/library_loader/
 2046  cp CertVerifyStatusAndroid.java /home/malin/github_demo/ChromimuAndroid/lib_net_java/src/main/java/org/chromium/net
 2047  cp CellularSignalStrengthError.java /home/malin/github_demo/ChromimuAndroid/lib_net_java/src/main/java/org/chromium/net
 2049  cp TrafficStatsUid.java /home/malin/github_demo/ChromimuAndroid/lib_net_java/src/main/java/org/chromium/net
 2051  cp TrafficStatsError.java /home/malin/github_demo/ChromimuAndroid/lib_net_java/src/main/java/org/chromium/net
 2054  cp NetError.java /home/malin/github_demo/ChromimuAndroid/lib_net_java/src/main/java/org/chromium/net
 2056  cp ConnectionType.java /home/malin/github_demo/ChromimuAndroid/lib_net_java/src/main/java/org/chromium/net
 2057  cp ConnectionSubtype.java /home/malin/github_demo/ChromimuAndroid/lib_net_java/src/main/java/org/chromium/net
 2058  cp NetId.java /home/malin/github_demo/ChromimuAndroid/lib_net_java/src/main/java/org/chromium/net
 2076  cp RequestPriority.java /home/malin/github_demo/ChromimuAndroid/lib_cronet_impl_all/src/main/java/org/chromium/net/impl/
 2082  cp HttpCacheType.java /home/malin/github_demo/ChromimuAndroid/lib_cronet_impl_all/src/main/java/org/chromium/net/impl
 2084  cp EffectiveConnectionType.java /home/malin/github_demo/ChromimuAndroid/lib_net_java/src/main/java/org/chromium/net
 2085  cp RttThroughputValues.java /home/malin/github_demo/ChromimuAndroid/lib_net_java/src/main/java/org/chromium/net
 2088  cp ImplVersion.java /home/malin/github_demo/ChromimuAndroid/lib_cronet_impl_all/src/main/java/org/chromium/net/impl
 2089  cp LoadState.java /home/malin/github_demo/ChromimuAndroid/lib_cronet_impl_all/src/main/java/org/chromium/net/impl
 2090  cp UrlRequestError.java /home/malin/github_demo/ChromimuAndroid/lib_cronet_impl_all/src/main/java/org/chromium/net/impl

