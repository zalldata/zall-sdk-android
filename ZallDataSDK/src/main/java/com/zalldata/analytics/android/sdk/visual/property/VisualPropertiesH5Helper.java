/*
 * Created by guo on 2021/07/30.
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

package com.zalldata.analytics.android.sdk.visual.property;

import android.text.TextUtils;
import android.util.Base64;
import android.util.SparseArray;
import android.view.View;

import com.zalldata.analytics.android.sdk.AopConstants;
import com.zalldata.analytics.android.sdk.ZALog;
import com.zalldata.analytics.android.sdk.ZallDataAPI;
import com.zalldata.analytics.android.sdk.listener.ZAEventListener;
import com.zalldata.analytics.android.sdk.util.Base64Coder;
import com.zalldata.analytics.android.sdk.visual.ViewTreeStatusObservable;
import com.zalldata.analytics.android.sdk.visual.bridge.JSBridgeHelper;
import com.zalldata.analytics.android.sdk.visual.bridge.OnBridgeCallback;
import com.zalldata.analytics.android.sdk.visual.bridge.WebViewJavascriptBridge;
import com.zalldata.analytics.android.sdk.visual.model.ViewNode;
import com.zalldata.analytics.android.sdk.visual.model.VisualConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class VisualPropertiesH5Helper implements WebViewJavascriptBridge {

    private JSBridgeHelper mJSBridgeHelper;
    private ZAEventListener mZAEventListener;
    private SparseArray<JSONArray> mSparseArray = new SparseArray<>();

    public VisualPropertiesH5Helper() {
        mJSBridgeHelper = new JSBridgeHelper();
    }

    void mergeJSVisualProperties(final JSONObject srcObject, HashSet<String> hashSet, String eventName) {
        try {
            if (hashSet == null || hashSet.size() == 0) {
                return;
            }
            Iterator<String> entries = hashSet.iterator();
            final CountDownLatch latch = new CountDownLatch(hashSet.size());
            while (entries.hasNext()) {
                String webViewElementPath = entries.next();
                ViewNode viewNode = ViewTreeStatusObservable.getInstance().getViewNode(webViewElementPath);
                if (viewNode != null && viewNode.getView() != null) {
                    View view = viewNode.getView().get();
                    if (view != null) {
                        getJSVisualProperties(view, viewNode.getViewPath(), eventName, new OnBridgeCallback() {
                            @Override
                            public void onCallBack(String data) {
                                try {
                                    JSONObject obj = new JSONObject(data);
                                    Iterator<String> iterator = obj.keys();
                                    while (iterator.hasNext()) {
                                        String key = iterator.next();
                                        String value = obj.optString(key);
                                        // WebClick 自定义属性优先级最高
                                        if (!TextUtils.isEmpty(key)) {
                                            srcObject.put(key, value);
                                        }
                                    }
                                } catch (JSONException e) {
                                    ZALog.printStackTrace(e);
                                } finally {
                                    latch.countDown();
                                }
                            }
                        });
                    }
                }
            }
            try {
                latch.await(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                ZALog.printStackTrace(e);
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    private void getJSVisualProperties(View webView, String elementPath, String eventName, OnBridgeCallback onBridgeCallback) {
        try {
            JSONArray array = VisualPropertiesManager.getInstance().getVisualPropertiesCache().getH5JsonArrayFromCache(eventName, elementPath);
            if (array == null) {
                return;
            }
            JSONObject obj = new JSONObject();
            try {
                obj.put("zalldata_js_visual_properties", array);
            } catch (JSONException e) {
                ZALog.printStackTrace(e);
            }
            sendToWeb(webView, "getJSVisualProperties", obj, onBridgeCallback);
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    /**
     * 注册监听
     */
    public void registerListeners() {
        try {
            mJSBridgeHelper.addZAJSListener();
            addZAEventListener();
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    private void addZAEventListener() {
        if (mZAEventListener == null) {
            mZAEventListener = new ZAEventListener() {
                @Override
                public void trackEvent(JSONObject jsonObject) {
                    try {
                        String eventType = jsonObject.optString("event");
                        if (!TextUtils.equals(AopConstants.WEB_CLICK_EVENT_NAME, eventType)) {
                            return;
                        }
                        JSONObject propertiesObj = jsonObject.optJSONObject("properties");
                        if (propertiesObj == null) {
                            return;
                        }

                        if (propertiesObj.has("zalldata_web_visual_eventName")) {
                            mSparseArray.put(jsonObject.hashCode(), propertiesObj.optJSONArray("zalldata_web_visual_eventName"));
                            propertiesObj.remove("zalldata_web_visual_eventName");
                        }

                        String base64Message = propertiesObj.optString("zalldata_app_visual_properties");
                        propertiesObj.remove("zalldata_app_visual_properties");
                        if (TextUtils.isEmpty(base64Message) || !ZallDataAPI.getConfigOptions().isVisualizedPropertiesEnabled()) {
                            return;
                        }
                        String appVisualProperties = Base64Coder.decodeString(base64Message);
                        if (TextUtils.isEmpty(appVisualProperties)) {
                            return;
                        }
                        try {
                            JSONArray array = new JSONArray(appVisualProperties);
                            if (array.length() > 0) {
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject obj = array.getJSONObject(i);
                                    VisualConfig.VisualProperty visualProperty = new VisualConfig.VisualProperty();
                                    visualProperty.elementPath = obj.optString("element_path");
                                    visualProperty.elementPosition = obj.optString("element_position");
                                    visualProperty.screenName = obj.optString("screen_name");
                                    visualProperty.name = obj.optString("name");
                                    visualProperty.regular = obj.optString("regular");
                                    visualProperty.isH5 = obj.optBoolean("h5");
                                    visualProperty.type = obj.optString("type");
                                    visualProperty.webViewElementPath = obj.optString("webview_element_path");
                                    VisualPropertiesManager.getInstance().mergeAppVisualProperty(visualProperty, null, propertiesObj, null);
                                }
                            }
                        } catch (JSONException e) {
                            ZALog.printStackTrace(e);
                        }
                    } catch (Exception e) {
                        ZALog.printStackTrace(e);
                    }
                }

                @Override
                public void login() {

                }

                @Override
                public void logout() {

                }

                @Override
                public void identify() {

                }

                @Override
                public void resetAnonymousId() {

                }
            };
            ZallDataAPI.sharedInstance().addEventListener(mZAEventListener);
        }
    }

    public JSONArray getEventName(int hashCode) {
        try {
            return mSparseArray.get(hashCode);
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
        return null;
    }

    public void clearCache(int hashCode) {
        try {
            mSparseArray.remove(hashCode);
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    @Override
    public void sendToWeb(View webView, String methodName, Object data, OnBridgeCallback
            responseCallback) {
        mJSBridgeHelper.sendToWeb(webView, methodName, data, responseCallback);
    }

    @Override
    public void sendToWeb(View webView, String methodName, Object data) {
        mJSBridgeHelper.sendToWeb(webView, methodName, data);
    }

    private static String Base642string(String s) {
        return new String(Base64.decode(s.getBytes(), Base64.DEFAULT));
    }
}