/*
 * Created by guo on 2021/07/04.
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

package com.zalldata.analytics.android.sdk.util;

import android.content.Context;
import android.text.TextUtils;

import com.zalldata.analytics.android.sdk.ZALog;
import com.zalldata.analytics.android.sdk.ZallDataAPI;

import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ZAContextManager {
    private final Context mContext;
    private boolean mDisableTrackDeviceId;
    private Map<String, Object> mDeviceInfo;
    /* AndroidID */
    private String mAndroidId;
    private boolean isAppStartSuccess;

    public ZAContextManager(Context context, boolean isTrackDeviceId) {
        this.mContext = context;
        this.mDisableTrackDeviceId = isTrackDeviceId;
    }

    /**
     * 获取 DeviceInfo
     * @return DeviceInfo
     */
    public Map<String, Object> getDeviceInfo() {
        try {
            if (mDeviceInfo == null && ZallDataAPI.getConfigOptions().isDataCollectEnable()) {
                setupDeviceInfo();
            }
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
        return mDeviceInfo;
    }

    /**
     * 从 DeviceInfo 中添加指定 Key
     * @param jsonObject JSONObject
     * @param key 指定 Key
     */
    public void addKeyIfExist(JSONObject jsonObject, String key) {
        try {
            if (mDeviceInfo == null && ZallDataAPI.getConfigOptions().isDataCollectEnable()) {
                setupDeviceInfo();
            }
            if (mDeviceInfo != null && mDeviceInfo.containsKey(key)) {
                jsonObject.put(key, mDeviceInfo.get(key));
            }
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
    }

    /**
     * 获取 AndroidID
     * @return AndroidID
     */
    public String getAndroidId() {
        if (TextUtils.isEmpty(mAndroidId) && ZallDataAPI.getConfigOptions().isDataCollectEnable()) {
            mAndroidId = ZallDataUtils.getAndroidID(mContext);
        }
        return mAndroidId;
    }

    /**
     * 获取预置属性信息
     * @return 预置属性信息
     */
    public JSONObject getPresetProperties() {
        JSONObject properties = new JSONObject();
        try {
            if (mDeviceInfo == null) {
                setupDeviceInfo();
            }
            properties.put("$app_version", mDeviceInfo.get("$app_version"));
            properties.put("$lib", "Android");
            properties.put("$lib_version", mDeviceInfo.get("$lib_version"));
            properties.put("$manufacturer", mDeviceInfo.get("$manufacturer"));
            properties.put("$model", mDeviceInfo.get("$model"));
            properties.put("$brand", mDeviceInfo.get("$brand"));
            properties.put("$os", mDeviceInfo.get("$os"));
            properties.put("$os_version", mDeviceInfo.get("$os_version"));
            properties.put("$screen_height", mDeviceInfo.get("$screen_height"));
            properties.put("$screen_width", mDeviceInfo.get("$screen_width"));
            String networkType = NetworkUtils.networkType(mContext);
            properties.put("$wifi", "WIFI".equals(networkType));
            properties.put("$network_type", networkType);
            properties.put("$carrier", mDeviceInfo.get("$carrier"));
            properties.put("$app_id", mDeviceInfo.get("$app_id"));
            properties.put("$timezone_offset", mDeviceInfo.get("$timezone_offset"));
            if (mDeviceInfo.containsKey("$device_id")) {
                properties.put("$device_id", mDeviceInfo.get("$device_id"));
            }
            properties.put("$app_name", mDeviceInfo.get("$app_name"));
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
        return properties;
    }

    /**
     * 获取并配置 App 的一些基本属性
     */
    private void setupDeviceInfo() {
        final Map<String, Object> deviceInfo = new HashMap<>();
        String osVersion = DeviceUtils.getHarmonyOSVersion();
        if (!TextUtils.isEmpty(osVersion)) {
            deviceInfo.put("$os", "HarmonyOS");
            deviceInfo.put("$os_version", osVersion);
        } else {
            deviceInfo.put("$os", "Android");
            deviceInfo.put("$os_version", DeviceUtils.getOS());
        }

        deviceInfo.put("$lib", "Android");
        deviceInfo.put("$lib_version", ZallDataAPI.sharedInstance().getSDKVersion());
        deviceInfo.put("$manufacturer", DeviceUtils.getManufacturer());
        deviceInfo.put("$model", DeviceUtils.getModel());
        deviceInfo.put("$brand", DeviceUtils.getBrand());
        deviceInfo.put("$app_version", AppInfoUtils.getAppVersionName(mContext));
        int[] size = DeviceUtils.getDeviceSize(mContext);
        deviceInfo.put("$screen_width", size[0]);
        deviceInfo.put("$screen_height", size[1]);

        String carrier = ZallDataUtils.getCarrier(mContext);
        if (!TextUtils.isEmpty(carrier)) {
            deviceInfo.put("$carrier", carrier);
        }

        mAndroidId = ZallDataUtils.getAndroidID(mContext);
        if (!mDisableTrackDeviceId && !TextUtils.isEmpty(mAndroidId)) {
            deviceInfo.put("$device_id", mAndroidId);
        }

        Integer zone_offset = TimeUtils.getZoneOffset();
        if (zone_offset != null) {
            deviceInfo.put("$timezone_offset", zone_offset);
        }

        deviceInfo.put("$app_id", AppInfoUtils.getProcessName(mContext));
        deviceInfo.put("$app_name", AppInfoUtils.getAppName(mContext));
        mDeviceInfo =  Collections.unmodifiableMap(deviceInfo);
    }

    public boolean isAppStartSuccess() {
        return isAppStartSuccess;
    }

    public void setAppStartSuccess(boolean appStartSuccess) {
        isAppStartSuccess = appStartSuccess;
    }
}
