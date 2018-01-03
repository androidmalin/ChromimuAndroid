

cd /onet/malin/chromium/src/out/Release/lib.java

find . -name "*.jar" -type f | grep -v third_party | grep -v testing | xargs -i cp {} /home/malin/github_demo/ChromimuAndroid/app/alljars



find . -name "*.jar" -type f | grep -v third_party | grep -v testing | grep -v build | grep -v interface | grep -v test | xargs -i cp {} /home/malin/github_demo/ChromimuAndroid/app/libs