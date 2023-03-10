/*
 * Created by guo on 2020/11/26.
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
package com.zalldata.analytics.android.sdk.internal.api;

import android.text.TextUtils;

import com.zalldata.analytics.android.sdk.ZALog;
import com.zalldata.analytics.android.sdk.ZallDataAPI;
import com.zalldata.analytics.android.sdk.ZallDataIgnoreTrackAppViewScreen;
import com.zalldata.analytics.android.sdk.ZallDataIgnoreTrackAppViewScreenAndAppClick;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class FragmentAPI implements IFragmentAPI {
    private static final String TAG = "FragmentAPI";
    /* $AppViewScreen 事件是否支持 Fragment*/
    private boolean mTrackFragmentAppViewScreen;
    private Set<Integer> mAutoTrackFragments;
    private Set<Integer> mAutoTrackIgnoredFragments;

    public FragmentAPI() {
    }

    @Override
    public void trackFragmentAppViewScreen() {
        this.mTrackFragmentAppViewScreen = true;
    }

    @Override
    public boolean isTrackFragmentAppViewScreenEnabled() {
        return this.mTrackFragmentAppViewScreen;
    }

    @Override
    public void enableAutoTrackFragment(Class<?> fragment) {
        try {
            if (fragment == null) {
                return;
            }

            if (mAutoTrackFragments == null) {
                mAutoTrackFragments = new CopyOnWriteArraySet<>();
            }

            String canonicalName = fragment.getCanonicalName();
            if (!TextUtils.isEmpty(canonicalName)) {
                mAutoTrackFragments.add(canonicalName.hashCode());
            }
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
    }

    @Override
    public void enableAutoTrackFragments(List<Class<?>> fragmentsList) {
        if (fragmentsList == null || fragmentsList.size() == 0) {
            return;
        }

        if (mAutoTrackFragments == null) {
            mAutoTrackFragments = new CopyOnWriteArraySet<>();
        }

        try {
            String canonicalName;
            for (Class fragment : fragmentsList) {
                canonicalName = fragment.getCanonicalName();
                if (!TextUtils.isEmpty(canonicalName)) {
                    mAutoTrackFragments.add(canonicalName.hashCode());
                }
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    @Override
    public boolean isFragmentAutoTrackAppViewScreen(Class<?> fragment) {
        if (fragment == null) {
            return false;
        }
        try {
            if (ZallDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(ZallDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)
                    || !mTrackFragmentAppViewScreen) {
                return false;
            }

            if (mAutoTrackFragments != null && mAutoTrackFragments.size() > 0) {
                String canonicalName = fragment.getCanonicalName();
                if (!TextUtils.isEmpty(canonicalName)) {
                    return mAutoTrackFragments.contains(canonicalName.hashCode());
                }
            }

            if (fragment.getAnnotation(ZallDataIgnoreTrackAppViewScreen.class) != null) {
                return false;
            }

            if (fragment.getAnnotation(ZallDataIgnoreTrackAppViewScreenAndAppClick.class) != null) {
                return false;
            }

            if (mAutoTrackIgnoredFragments != null && mAutoTrackIgnoredFragments.size() > 0) {
                String canonicalName = fragment.getCanonicalName();
                if (!TextUtils.isEmpty(canonicalName)) {
                    return !mAutoTrackIgnoredFragments.contains(canonicalName.hashCode());
                }
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }

        return true;
    }

    @Override
    public void ignoreAutoTrackFragments(List<Class<?>> fragmentList) {
        try {
            if (fragmentList == null || fragmentList.size() == 0) {
                return;
            }

            if (mAutoTrackIgnoredFragments == null) {
                mAutoTrackIgnoredFragments = new CopyOnWriteArraySet<>();
            }

            for (Class<?> fragment : fragmentList) {
                if (fragment != null) {
                    String canonicalName = fragment.getCanonicalName();
                    if (!TextUtils.isEmpty(canonicalName)) {
                        mAutoTrackIgnoredFragments.add(canonicalName.hashCode());
                    }
                }
            }
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
    }

    @Override
    public void ignoreAutoTrackFragment(Class<?> fragment) {
        try {
            if (fragment == null) {
                return;
            }

            if (mAutoTrackIgnoredFragments == null) {
                mAutoTrackIgnoredFragments = new CopyOnWriteArraySet<>();
            }

            String canonicalName = fragment.getCanonicalName();
            if (!TextUtils.isEmpty(canonicalName)) {
                mAutoTrackIgnoredFragments.add(canonicalName.hashCode());
            }
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
    }

    @Override
    public void resumeIgnoredAutoTrackFragments(List<Class<?>> fragmentList) {
        try {
            if (fragmentList == null || fragmentList.size() == 0 ||
                    mAutoTrackIgnoredFragments == null) {
                return;
            }

            for (Class fragment : fragmentList) {
                if (fragment != null) {
                    String canonicalName = fragment.getCanonicalName();
                    if (!TextUtils.isEmpty(canonicalName)) {
                        mAutoTrackIgnoredFragments.remove(canonicalName.hashCode());
                    }
                }
            }
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
    }

    @Override
    public void resumeIgnoredAutoTrackFragment(Class<?> fragment) {
        try {
            if (fragment == null || mAutoTrackIgnoredFragments == null) {
                return;
            }

            String canonicalName = fragment.getCanonicalName();
            if (!TextUtils.isEmpty(canonicalName)) {
                mAutoTrackIgnoredFragments.remove(canonicalName.hashCode());
            }
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
    }
}
