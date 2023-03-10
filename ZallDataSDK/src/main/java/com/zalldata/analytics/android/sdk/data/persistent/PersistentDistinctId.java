/*
 * Created by guo on 2015/08/01.
 * Copyright 2015－2021 Zall Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zalldata.analytics.android.sdk.data.persistent;

import android.content.Context;
import android.content.SharedPreferences;

import com.zalldata.analytics.android.sdk.util.ZallDataUtils;

import java.util.UUID;
import java.util.concurrent.Future;

public class PersistentDistinctId extends PersistentIdentity<String> {
    public PersistentDistinctId(Future<SharedPreferences> loadStoredPreferences, final Context context) {
        super(loadStoredPreferences, PersistentLoader.PersistentName.DISTINCT_ID, new PersistentSerializer<String>() {
            @Override
            public String load(String value) {
                return value;
            }

            @Override
            public String save(String item) {
                return item == null ? create() : item;
            }

            @Override
            public String create() {
                String androidId = ZallDataUtils.getAndroidID(context);
                if (ZallDataUtils.isValidAndroidId(androidId)) {
                    return androidId;
                }
                return UUID.randomUUID().toString();
            }
        });
    }
}
