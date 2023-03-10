/*
 * Created by guo on 2020/09/22.
 * Copyright 2015－2020 Zall Data Inc.
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

package com.zalldata.analytics.android.sdk.visual.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.zalldata.analytics.android.sdk.ZALog;
import com.zalldata.analytics.android.sdk.ZallDataAPI;
import com.zalldata.analytics.android.sdk.network.HttpCallback;
import com.zalldata.analytics.android.sdk.network.HttpMethod;
import com.zalldata.analytics.android.sdk.network.RequestHelper;
import com.zalldata.analytics.android.sdk.util.ZallDataUtils;

import org.json.JSONObject;

import java.util.HashMap;

public class PairingCodeRequestHelper {

    private static final String TAG = "ZA.ParingCodeHttpRequest";
    private static final String URL_VERIFY_SUFFIX = "api/sdk/heat_maps/scanning/pairing_code";

    public interface IApiCallback {
        void onSuccess();

        void onFailure(String message);
    }

    public void verifyPairingCodeRequest(final Context context, String paringCode, final IApiCallback callback) {
        try {
            String serverUrl = ZallDataAPI.sharedInstance().getServerUrl();
            if (TextUtils.isEmpty(serverUrl)) {
                ZALog.i(TAG, "verifyParingCodeRequest | server url is null and return");
                return;
            }

            final Uri uri = Uri.parse(ZallDataAPI.sharedInstance().getServerUrl());
            final Uri.Builder builder = new Uri.Builder();
            builder.scheme(uri.getScheme()).encodedAuthority(uri.getAuthority());

            HashMap<String, String> params = new HashMap<>();
            params.put("pairing_code", paringCode);
            HashMap<String, String> header = new HashMap<>();
            header.put("zalldata-project", uri.getQueryParameter("project"));

            new RequestHelper.Builder(HttpMethod.GET, builder.appendEncodedPath(URL_VERIFY_SUFFIX).toString())
                    .params(params)
                    .header(header)
                    .callback(new HttpCallback.JsonCallback() {
                        @Override
                        public void onFailure(int code, String errorMessage) {
                            if (callback != null) {
                                callback.onFailure(errorMessage);
                            }
                        }

                        @Override
                        public void onResponse(JSONObject response) {
                            if (response == null) {
                                return;
                            }

                            ZALog.i(TAG, "verifyParingCodeRequest onResponse | response: " + response.toString());
                            boolean isSuccess = response.optBoolean("is_success");

                            if (isSuccess) {
                                String urlString = response.optString("url");
                                ZALog.i(TAG, "verifyParingCodeRequest onResponse | url: " + urlString);
                                if (!TextUtils.isEmpty(urlString)) {
                                    ZallDataUtils.handleSchemeUrl((Activity) context, new Intent().setData(Uri.parse(urlString)));
                                }
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            } else {
                                if (callback != null) {
                                    callback.onFailure(response.optString("error_msg"));
                                }
                            }
                        }

                        @Override
                        public void onAfter() {
                        }
                    }).execute();
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

}
