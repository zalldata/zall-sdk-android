/*
 * Created by guo on 2020/03/05.
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

package com.zalldata.analytics.android.sdk.visual.util;

import android.app.Activity;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.zalldata.analytics.android.sdk.AopConstants;
import com.zalldata.analytics.android.sdk.AppStateManager;
import com.zalldata.analytics.android.sdk.R;
import com.zalldata.analytics.android.sdk.ZALog;
import com.zalldata.analytics.android.sdk.util.AopUtil;
import com.zalldata.analytics.android.sdk.util.ReflectUtil;
import com.zalldata.analytics.android.sdk.util.ViewUtil;
import com.zalldata.analytics.android.sdk.visual.model.SnapInfo;
import com.zalldata.analytics.android.sdk.visual.snap.Pathfinder;

import org.json.JSONObject;

public class VisualUtil {
    public static int getVisibility(View view) {
        if (view instanceof Spinner) {
            return View.GONE;
        }
        if (!ViewUtil.isViewSelfVisible(view)) {
            return View.GONE;
        }
        if (!view.isShown()) {
            return View.GONE;
        }
        return View.VISIBLE;
    }

    public static boolean isSupportElementContent(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return !(view instanceof SeekBar || view instanceof RatingBar || view instanceof Switch);
        }
        return false;
    }

    public static boolean isForbiddenClick(View v) {
        if (ViewUtil.instanceOfWebView(v) || v instanceof AdapterView) {
            return true;
        }
        if (v instanceof TextView) {
            TextView textView = (TextView) v;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if (textView.isTextSelectable() && !textView.hasOnClickListeners()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isSupportClick(View v) {
        ViewParent parent = v.getParent();
        if (parent instanceof AdapterView || ViewUtil.instanceOfRecyclerView(parent)) {
            return true;
        }
        if (v instanceof RatingBar || v instanceof SeekBar) {
            return true;
        }
        return false;
    }

    public static int getChildIndex(ViewParent parent, View child) {
        try {
            if (!(parent instanceof ViewGroup)) {
                return -1;
            }
            ViewGroup viewParent = (ViewGroup) parent;
            final String childIdName = AopUtil.getViewId(child);
            String childClassName = child.getClass().getCanonicalName();
            int index = 0;
            for (int i = 0; i < viewParent.getChildCount(); i++) {
                View brother = viewParent.getChildAt(i);
                if (!Pathfinder.hasClassName(brother, childClassName)) {
                    continue;
                }
                String brotherIdName = AopUtil.getViewId(brother);
                if (null != childIdName && !childIdName.equals(brotherIdName)) {
                    index++;
                    continue;
                }
                if (brother == child) {
                    return index;
                }
                index++;
            }
            return -1;
        } catch (Exception e) {
            ZALog.printStackTrace(e);
            return -1;
        }
    }

    /**
     * 取控件响应链的 screen_name
     *
     * @param view ViewTree 中的 控件
     * @param info 可视化临时缓存对象
     * @return 含 $screen_name 和 $title 的 json
     */
    public static JSONObject getScreenNameAndTitle(View view, SnapInfo info) {
        if (view == null) {
            return null;
        }
        JSONObject object = null;
        Activity activity = AopUtil.getActivityFromContext(view.getContext(), view);
        if (activity == null) {
            activity = AppStateManager.getInstance().getForegroundActivity();
        }
        if (activity != null && activity.getWindow() != null && activity.getWindow().isActive()) {
            object = new JSONObject();
            Object fragment = AopUtil.getFragmentFromView(view, activity);
            if (fragment != null) {
                AopUtil.getScreenNameAndTitleFromFragment(object, fragment, activity);
                if (info != null && !info.hasFragment) {
                    info.hasFragment = true;
                }
            } else {
                object = AopUtil.buildTitleAndScreenName(activity);
                mergeRnScreenNameAndTitle(object, view);
            }
        }
        return object;
    }

    public static void mergeRnScreenNameAndTitle(JSONObject jsonObject) {
        mergeRnScreenNameAndTitle(jsonObject, null);
    }

    /**
     * 如果存在 RN 页面，优先获取 RN 的 screen_name
     *
     * @param jsonObject 原生的 object
     */
    public static void mergeRnScreenNameAndTitle(JSONObject jsonObject, View view) {
        try {
            Class<?> rnViewUtils = ReflectUtil.getCurrentClass(new String[]{"com.zalldata.analytics.utils.RNViewUtils"});
            String properties = ReflectUtil.callStaticMethod(rnViewUtils, "getVisualizeProperties");
            if (!TextUtils.isEmpty(properties)) {
                JSONObject object = new JSONObject(properties);
                if (view != null && object.optBoolean("isSetRNViewTag", false)) {
                    Object isRNView = view.getTag(R.id.zall_analytics_tag_view_rn_key);
                    if (isRNView == null || !(Boolean) isRNView) {
                        return;
                    }
                }
                String rnScreenName = object.optString("$screen_name");
                String rnActivityTitle = object.optString("$title");
                if (jsonObject.has(AopConstants.SCREEN_NAME)) {
                    jsonObject.put(AopConstants.SCREEN_NAME, rnScreenName);
                }
                if (jsonObject.has(AopConstants.TITLE)) {
                    jsonObject.put(AopConstants.TITLE, rnActivityTitle);
                }
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

}
