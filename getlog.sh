#!/bin/bash
adb exec-out run-as com.malin.chromium cat cache/netlog.json > ./netlog.json
