---
title: "Chromium Android源码编译"
date: 2017-12-11 10:50
tags: chromium
---

编译需要大约3小时.安装官网给出的方案,即可.

[linux_build_instructions](https://chromium.googlesource.com/chromium/src/+/master/docs/linux_build_instructions.md)


解决办法:

1.切换到src目录
`cd xx/xx/chromium/src/`

2.删除历史构建缓存目录
`rm -rf out/Default`

3.设置构建缓存目录为/out/Default
`gn gen out/Default`

4.生成配置文件
`vim out/Default/args.gn`

内容如下
```
target_os = "android"
target_cpu = "arm"  # (default)
is_debug = false  # (default)
# Other args you may want to set:
is_component_build = false
is_clang = true
symbol_level = 1  # Faster build with fewer symbols. -g1 rather than -g2
enable_incremental_javac = false  # Much faster; experimental
android_ndk_root = "/home/malin/ndk/android-ndk-r12b"
android_sdk_root = "/home/malin/sdk"
android_sdk_build_tools_version = "27.0.1"
disable_file_support = true
disable_ftp_support = true
enable_websockets = false
use_platform_icu_alternatives = true
```

字段含义请看这篇文章(懒人chromium net android移植指南)[http://hanpfei.github.io/2016/11/11/lazy-chromium-net-android-porting-guide/]


5.执行如下命令生成cronet so文件：
`ninja -C out/Default/ cronet`


```
You can get a list of all of the other build targets from GN by running gn ls out/Default from the command line. To compile one, pass the GN label to Ninja with no preceding “//” (so, for //chrome/test:unit_tests use ninja -C out/Default chrome/test:unit_tests).
```

6.执行如下命令生成cronet Java层代码的jar包：


错误1.
```
../../third_party/icu/source/common/unicode/unistr.h:32:10: fatal error: 'unicode/utypes.h' file not found
#include "unicode/utypes.h"
         ^~~~~~~~~~~~~~~~~~
1 error generated.

```

完整日志

```
malin@malin:/onet/malin/chromium/src$ ninja -C out/Default/ cronet
ninja: Entering directory `out/Default/'
[57/85] CXX obj/net/net/ftp_util.o
FAILED: obj/net/net/ftp_util.o
../../third_party/llvm-build/Release+Asserts/bin/clang++ -MMD -MF obj/net/net/ftp_util.o.d -DV8_DEPRECATION_WARNINGS -DNO_TCMALLOC -DSAFE_BROWSING_DB_REMOTE -DCHROMIUM_BUILD -DFIELDTRIAL_TESTING_ENABLED -D_FILE_OFFSET_BITS=64 -DANDROID -DHAVE_SYS_UIO_H -DANDROID_NDK_VERSION_ROLL=r12b_1 -DCR_CLANG_REVISION=\"318667-1\" -D__STDC_CONSTANT_MACROS -D__STDC_FORMAT_MACROS -D_FORTIFY_SOURCE=2 -D__GNU_SOURCE=1 -DCHROMIUM_CXX_TWEAK_INLINES -D__compiler_offsetof=__builtin_offsetof -Dnan=__builtin_nan -DNDEBUG -DNVALGRIND -DDYNAMIC_ANNOTATIONS_ENABLED=0 -DDLOPEN_KERBEROS -DNET_IMPLEMENTATION -DUSE_KERBEROS -DENABLE_BUILT_IN_DNS -DGOOGLE_PROTOBUF_NO_RTTI -DGOOGLE_PROTOBUF_NO_STATIC_INITIALIZER -DHAVE_PTHREAD -I../.. -Igen -I/usr/include/kerberosV -I../../third_party/protobuf/src -Igen/protoc_out -I../../third_party/protobuf/src -I../../third_party/boringssl/src/include -I../../third_party/zlib -Igen/net/net_jni_headers -Igen/net/net_jni_headers/net -I../../third_party/brotli/include -fno-strict-aliasing --param=ssp-buffer-size=4 -fstack-protector -Wno-builtin-macro-redefined -D__DATE__= -D__TIME__= -D__TIMESTAMP__= -funwind-tables -fPIC -pipe -fcolor-diagnostics -no-canonical-prefixes -ffunction-sections -fno-short-enums --target=arm-linux-androideabi -march=armv7-a -mfloat-abi=softfp -mtune=generic-armv7-a -mfpu=neon -mthumb -Wall -Werror -Wextra -Wno-missing-field-initializers -Wno-unused-parameter -Wno-c++11-narrowing -Wno-covered-switch-default -Wno-unneeded-internal-declaration -Wno-inconsistent-missing-override -Wno-undefined-var-template -Wno-nonportable-include-path -Wno-address-of-packed-member -Wno-unused-lambda-capture -Wno-user-defined-warnings -Wno-enum-compare-switch -Wno-tautological-unsigned-zero-compare -Wno-null-pointer-arithmetic -Wno-tautological-constant-compare -Wtautological-constant-out-of-range-compare -Oz -fno-ident -fdata-sections -ffunction-sections -fomit-frame-pointer -gdwarf-3 -g1 -fdebug-info-for-profiling -fvisibility=hidden -Xclang -load -Xclang ../../third_party/llvm-build/Release+Asserts/lib/libFindBadConstructs.so -Xclang -add-plugin -Xclang find-bad-constructs -Xclang -plugin-arg-find-bad-constructs -Xclang check-ipc -Wheader-hygiene -Wstring-conversion -Wtautological-overlap-compare -Wexit-time-destructors -std=gnu++14 -fno-exceptions -fno-rtti -isystem../../../../../../home/malin/ndk/android-ndk-r12b/sources/cxx-stl/llvm-libc++/libcxx/include -isystem../../../../../../home/malin/ndk/android-ndk-r12b/sources/cxx-stl/llvm-libc++abi/libcxxabi/include -isystem../../../../../../home/malin/ndk/android-ndk-r12b/sources/android/support/include --sysroot=../../../../../../home/malin/ndk/android-ndk-r12b/platforms/android-16/arch-arm -fvisibility-inlines-hidden -c ../../net/ftp/ftp_util.cc -o obj/net/net/ftp_util.o
In file included from ../../net/ftp/ftp_util.cc:12:
In file included from ../../base/i18n/unicodestring.h:9:
../../third_party/icu/source/common/unicode/unistr.h:32:10: fatal error: 'unicode/utypes.h' file not found
#include "unicode/utypes.h"
         ^~~~~~~~~~~~~~~~~~
1 error generated.
[66/85] CXX obj/third_party/metrics_proto/metrics_proto/perf_data.pb.o
ninja: build stopped: subcommand failed.
```

解决办法:

`sudo apt-get install libunistring-dev`
`cd xx/xx/chromium/src/`

这个命令用于对历史构建进行清理。
`gn clean out/Default/`

产生ninja构建所需的 .ninja 文件。
`gn gen out/Default`

执行如下命令生成cronet so文件：
`ninja -C out/Default/ cronet`


查看所有可编译的目标
`gn ls out/Default`

jar包需要如下命令
`ninja -C out/Default/ components/cronet/android:repackage_extracted_jars`



`ninja -C out/Default/ components/cronet/android:jar_cronet_sample_source`

```
malin@malin:/onet/malin/chromium/src/out/Default/lib.java$ tree -L 7
.
├── android.interface.jar
├── base
│   ├── base_java.interface.jar
│   └── base_java.jar
├── build
│   └── android
│       ├── buildhooks
│       │   ├── build_hooks_android_java.interface.jar
│       │   ├── build_hooks_android_java.jar
│       │   ├── build_hooks_java.interface.jar
│       │   └── build_hooks_java.jar
│       └── bytecode
│           ├── java_bytecode_rewriter.interface.jar
│           └── java_bytecode_rewriter.jar
├── components
│   └── cronet
│       └── android
│           ├── cronet_api.interface.jar
│           ├── cronet_api.jar
│           ├── cronet_impl_common_java.interface.jar
│           ├── cronet_impl_common_java.jar
│           ├── cronet_impl_native_java.interface.jar
│           └── cronet_impl_native_java.jar
├── net
│   └── android
│       ├── net_java.interface.jar
│       └── net_java.jar
├── third_party
│   ├── android_tools
│   │   └── support
│   │       ├── android_support_multidex_java.interface.jar
│   │       ├── android_support_multidex_java.jar
│   │       ├── support-annotations-27.0.0.interface.jar
│   │       └── support-annotations-27.0.0.jar
│   ├── auto
│   │   ├── auto_common_java.interface.jar
│   │   ├── auto_common_java.jar
│   │   ├── auto_service_java.interface.jar
│   │   └── auto_service_java.jar
│   ├── bazel
│   │   └── desugar
│   │       ├── Desugar-runtime.interface.jar
│   │       └── Desugar-runtime.jar
│   ├── errorprone
│   │   ├── error_prone_ant-2.1.2.interface.jar
│   │   └── error_prone_ant-2.1.2.jar
│   ├── guava
│   │   ├── guava.interface.jar
│   │   └── guava.jar
│   ├── jsr-305
│   │   ├── jsr_305_javalib.interface.jar
│   │   └── jsr_305_javalib.jar
│   └── ow2_asm
│       ├── asm.interface.jar
│       ├── asm.jar
│       ├── asm-tree.interface.jar
│       ├── asm-tree.jar
│       ├── asm-util.interface.jar
│       └── asm-util.jar
├── tools
│   └── android
│       └── errorprone_plugin
│           ├── errorprone_plugin_java.interface.jar
│           └── errorprone_plugin_java.jar
└── url
    ├── url_java.interface.jar
    └── url_java.jar

```


`ninja -C out/Default/ components/cronet/android:cronet`
base_java.jar
url_java.jar


`ninja -C out/Default/ components/cronet/android:cronet_api_java`
`ninja -C out/Default/ components/cronet/android:jar_cronet_api_source`
cronet_api.interface.jar
cronet_api.jar


`ninja -C out/Default/ components/cronet/android:jar_cronet_impl_common_java_source`
cronet_impl_common_java.interface.jar
cronet_impl_common_java.jar

`ninja -C out/Default/ components/cronet/android:jar_cronet_impl_native_java_source`
cronet_impl_native_java.interface.jar
cronet_impl_native_java.jar

` ninja -C out/Default/ components/cronet/android:jar_cronet_impl_platform_java_source`
cronet_impl_platform_java.interface.jar
cronet_impl_platform_java.jar


f5dfb14f537bde791ff1a46e8c328c8b  url_java.jar



========
ninja -C out/Default/ components/cronet/android:cronet
========



其他参考文章有些不一样
[Chromium Android编译指南](http://www.jianshu.com/p/5fce18cbe016)
