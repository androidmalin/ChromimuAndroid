#!/bin/bash
adb shell am force-stop com.malin.chromium && \
gradle installDebug -x lint --build-cache --daemon --parallel --offline --configure-on-demand --continue && \
adb shell am start com.malin.chromium/.CronetSampleActivity

#adb exec-out run-as com.malin.chromium cat cache/netlog.json > ./netlog.json
