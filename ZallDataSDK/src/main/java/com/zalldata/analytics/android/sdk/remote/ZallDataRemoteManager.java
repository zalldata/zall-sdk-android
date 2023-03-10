/*
 * Created by guo on 2020/11/04.
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

package com.zalldata.analytics.android.sdk.remote;

import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.TextUtils;

import com.zalldata.analytics.android.sdk.ZALog;
import com.zalldata.analytics.android.sdk.ZallDataAPI;
import com.zalldata.analytics.android.sdk.data.adapter.DbAdapter;
import com.zalldata.analytics.android.sdk.network.HttpCallback;
import com.zalldata.analytics.android.sdk.util.ZallDataUtils;

import org.json.JSONObject;

import java.security.SecureRandom;

/**
 * SDK 初始化及线上使用时，采集控制管理类
 */
public class ZallDataRemoteManager extends BaseZallDataSDKRemoteManager {
    private static final String SHARED_PREF_REQUEST_TIME = "zalldata.request.time";
    private static final String SHARED_PREF_REQUEST_TIME_RANDOM = "zalldata.request.time.random";
    private static final String TAG = "ZA.ZallDataRemoteManager";
    private SharedPreferences mSharedPreferences;

    // 每次启动 App 时，最多尝试三次
    private CountDownTimer mPullSDKConfigCountDownTimer;

    public ZallDataRemoteManager(
            ZallDataAPI zallDataAPI) {
        super(zallDataAPI);
        ZALog.i(TAG, "Construct a ZallDataRemoteManager");
    }

    /**
     * 是否发起随机请求
     *
     * @return false 代表不发，true 代表发送随机请求
     */
    private boolean isRequestValid() {
        boolean isRequestValid = true;
        try {
            long lastRequestTime = getSharedPreferences().getLong(SHARED_PREF_REQUEST_TIME, 0);
            int randomTime = getSharedPreferences().getInt(SHARED_PREF_REQUEST_TIME_RANDOM, 0);
            if (lastRequestTime != 0 && randomTime != 0) {
                float requestInterval = SystemClock.elapsedRealtime() - lastRequestTime;
                // 当前的时间减去上次请求的时间，为间隔时间，当间隔时间小于随机时间，则不请求后端
                if (requestInterval > 0 && requestInterval / 1000 < randomTime * 3600) {
                    isRequestValid = false;
                }
            }
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
        return isRequestValid;
    }

    /**
     * 缓存远程控制随机时间
     */
    private void writeRemoteRequestRandomTime() {
        if (mZAConfigOptions == null) {
            return;
        }
        //默认情况下，随机请求时间为最小时间间隔
        int randomTime = mZAConfigOptions.mMinRequestInterval;
        long currentTime = SystemClock.elapsedRealtime();
        //最大时间间隔大于最小时间间隔时，生成随机时间
        if (mZAConfigOptions.mMaxRequestInterval > mZAConfigOptions.mMinRequestInterval) {
            randomTime += new SecureRandom().nextInt(mZAConfigOptions.mMaxRequestInterval - mZAConfigOptions.mMinRequestInterval + 1);
        }
        getSharedPreferences().edit()
                .putLong(SHARED_PREF_REQUEST_TIME, currentTime)
                .putInt(SHARED_PREF_REQUEST_TIME_RANDOM, randomTime)
                .apply();
    }

    /**
     * 清除远程控制随机时间的本地缓存
     */
    private void cleanRemoteRequestRandomTime() {
        getSharedPreferences().edit()
                .putLong(SHARED_PREF_REQUEST_TIME, 0)
                .putInt(SHARED_PREF_REQUEST_TIME_RANDOM, 0)
                .apply();
    }

    @Override
    public void pullSDKConfigFromServer() {
        if (mZAConfigOptions == null) {
            return;
        }

        // 关闭随机请求或者分散的最小时间大于最大时间时，清除本地时间，请求后端
        if (mZAConfigOptions.mDisableRandomTimeRequestRemoteConfig ||
                mZAConfigOptions.mMinRequestInterval > mZAConfigOptions.mMaxRequestInterval) {
            requestRemoteConfig(RandomTimeType.RandomTimeTypeClean, true);
            ZALog.i(TAG, "remote config: Request remote config because disableRandomTimeRequestRemoteConfig or minHourInterval greater than maxHourInterval");
            return;
        }

        //开启加密并且传入秘钥为空的，强制请求后端，此时请求中不带 v
        if (mZallDataEncrypt != null && mZallDataEncrypt.isPublicSecretKeyNull()) {
            requestRemoteConfig(RandomTimeType.RandomTimeTypeWrite, false);
            ZALog.i(TAG, "remote config: Request remote config because encrypt key is null");
            return;
        }

        //满足分散请求逻辑时，请求后端
        if (isRequestValid()) {
            requestRemoteConfig(RandomTimeType.RandomTimeTypeWrite, true);
            ZALog.i(TAG, "remote config: Request remote config because satisfy the random request condition");
        }
    }

    @Override
    public void requestRemoteConfig(RandomTimeType randomTimeType, final boolean enableConfigV) {
        if (mZallDataAPI != null && !mZallDataAPI.isNetworkRequestEnable()) {
            ZALog.i(TAG, "Close network request");
            return;
        }

        if (mDisableDefaultRemoteConfig) {
            ZALog.i(TAG, "disableDefaultRemoteConfig is true");
            return;
        }

        switch (randomTimeType) {
            case RandomTimeTypeWrite:
                writeRemoteRequestRandomTime();
                break;
            case RandomTimeTypeClean:
                cleanRemoteRequestRandomTime();
                break;
            default:
                break;
        }

        if (mPullSDKConfigCountDownTimer != null) {
            mPullSDKConfigCountDownTimer.cancel();
            mPullSDKConfigCountDownTimer = null;
        }

        mPullSDKConfigCountDownTimer = new CountDownTimer(90 * 1000, 30 * 1000) {
            @Override
            public void onTick(long l) {
                requestRemoteConfig(enableConfigV, new HttpCallback.StringCallback() {
                    @Override
                    public void onFailure(int code, String errorMessage) {
                        // 304 状态码为后端配置未更新，此时不需要重试
                        // 205 状态码表示后端环境未同步配置，此时需要重试，代码不需要做特殊处理
                        if (code == 304 || code == 404) {
                            resetPullSDKConfigTimer();
                        }
                        ZALog.i(TAG, "Remote request failed,responseCode is " + code +
                                ",errorMessage is " + errorMessage);
                    }

                    @Override
                    public void onResponse(String response) {
                        resetPullSDKConfigTimer();
                        if (!TextUtils.isEmpty(response)) {
                            ZallDataSDKRemoteConfig sdkRemoteConfig = toSDKRemoteConfig(response);
                            try {
                                if (mZallDataEncrypt != null) {
                                    mZallDataEncrypt.saveSecretKey(sdkRemoteConfig.getSecretKey());
                                }
                            } catch (Exception e) {
                                ZALog.printStackTrace(e);
                            }

                            setSDKRemoteConfig(sdkRemoteConfig);
                        }
                        ZALog.i(TAG, "Remote request was successful,response data is " + response);
                    }

                    @Override
                    public void onAfter() {

                    }
                });
            }

            @Override
            public void onFinish() {
            }
        };
        mPullSDKConfigCountDownTimer.start();
    }

    @Override
    public void resetPullSDKConfigTimer() {
        try {
            if (mPullSDKConfigCountDownTimer != null) {
                mPullSDKConfigCountDownTimer.cancel();
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        } finally {
            mPullSDKConfigCountDownTimer = null;
        }
    }

    /**
     * 更新 ZallDataSDKRemoteConfig
     *
     * @param sdkRemoteConfig ZallDataSDKRemoteConfig 在线控制 SDK 的配置
     */
    @Override
    protected void setSDKRemoteConfig(ZallDataSDKRemoteConfig sdkRemoteConfig) {
        try {
            //版本号不一致时，才会返回数据，此时上报事件
            JSONObject eventProperties = new JSONObject();
            String remoteConfigString = sdkRemoteConfig.toJson().toString();
            eventProperties.put("$app_remote_config", remoteConfigString);
            ZallDataAPI.sharedInstance().trackInternal("$AppRemoteConfigChanged", eventProperties);
            ZallDataAPI.sharedInstance().flush();
            DbAdapter.getInstance().commitRemoteConfig(remoteConfigString);
            ZALog.i(TAG, "Save remote data");
            //值为 1 时，表示在线控制立即生效
            if (1 == sdkRemoteConfig.getEffectMode()) {
                applySDKConfigFromCache();
                ZALog.i(TAG, "The remote configuration takes effect immediately");
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    /**
     * 从本地缓存中读取最新的 SDK 配置信息
     */
    @Override
    public void applySDKConfigFromCache() {
        try {
            ZallDataSDKRemoteConfig sdkRemoteConfig = toSDKRemoteConfig(DbAdapter.getInstance().getRemoteConfig());
            if (ZALog.isLogEnabled()) {
                ZALog.i(TAG, "Cache remote config is " + sdkRemoteConfig.toString());
            }
            if (mZallDataAPI != null) {
                //关闭 debug 模式
                if (sdkRemoteConfig.isDisableDebugMode()) {
                    mZallDataAPI.setDebugMode(ZallDataAPI.DebugMode.DEBUG_OFF);
                    ZALog.i(TAG, "Set DebugOff Mode");
                }

                if (sdkRemoteConfig.isDisableSDK()) {
                    try {
                        mZallDataAPI.flush();
                        ZALog.i(TAG, "DisableSDK is true");
                    } catch (Exception e) {
                        ZALog.printStackTrace(e);
                    }
                }
            }
            mSDKRemoteConfig = sdkRemoteConfig;
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    private SharedPreferences getSharedPreferences() {
        if (this.mSharedPreferences == null) {
            this.mSharedPreferences = ZallDataUtils.getSharedPreferences(mContext);
        }
        return mSharedPreferences;
    }
}
