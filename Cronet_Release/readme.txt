
ubuntu18.04编译
最后使用/onet/malin/chromium/src/out/Cronet_Release/cronet路径下的*.jar. libs下的so.其他可以忽略

使用自带的脚本编译cronet
官方文档[hbuild_instructions.md](https://chromium.googlesource.com/chromium/src/+/master/components/cronet/android/build_instructions.md)
1.Building Cronet for releases
`./components/cronet/tools/cr_cronet.py gn --release --out_dir=out/Cronet_Release`
执行完后,会在`out/Cronet_Release`目录下生成args.gn文件,同时生成相应的配置参数.

```
use_errorprone_java_compiler = true
arm_use_neon = false
target_os = "android"
enable_websockets = false
disable_file_support = true
disable_ftp_support = true
disable_brotli_filter = false
use_platform_icu_alternatives = true
enable_reporting = false
is_component_build = false
ignore_elf32_limitations = true
use_partition_alloc = false
include_transport_security_state_preload_list = false
is_debug = false
is_official_build = true
```


2.可以修改,配置文件
`vim out/Cronet_Release/args.gn`
增加
```
target_cpu = "arm"
android_ndk_root = "/home/malin/ndk/android-ndk-r12b"
android_sdk_root = "/home/malin/sdk"
android_sdk_build_tools_version = "27.0.3"
```
说明默认的sdk,ndk工具位于`src/third_party/android_tools`

3.
`gn gen out/Cronet_Release`

4.Building Cronet for releases
`ninja -C out/Cronet_Release cronet_package`
