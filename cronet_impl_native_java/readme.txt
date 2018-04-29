
cronet_impl_native_java这个module中cronet_impl_native_java-src.jar中没有aidl文件.需要从chromium项目中拷贝.


1.
cd /onet/malin/chromium/src/
find . -name "FileDescriptorInfo.aidl" -type f

./base/android/java/src/org/chromium/base/process_launcher/FileDescriptorInfo.aidl

所有的aidl文件在
/onet/malin/chromium/src/base/android/java/src/org/chromium/base/process_launcher