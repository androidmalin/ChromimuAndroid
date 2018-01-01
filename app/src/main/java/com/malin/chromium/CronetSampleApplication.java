package com.malin.chromium;// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.


import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

/**
 * Application for managing the Cronet Sample.
 */
public class CronetSampleApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
