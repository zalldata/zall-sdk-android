package com.zalldata.analytics.android.demo.jpush;

import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;

import cn.jpush.android.service.JCommonService;
import cn.jpush.android.service.PluginHuaweiPlatformsService;

public class XService extends HmsMessageService{

    final PluginHuaweiPlatformsService service = new PluginHuaweiPlatformsService();

    @Override
    public void onNewToken(String s) {
        service.onNewToken(s);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        service.onMessageReceived(remoteMessage);
    }

    @Override
    public void onMessageSent(String s) {
        service.onMessageSent(s);
    }

    @Override
    public void onSendError(String s, Exception e) {
        service.onSendError(s,e);
    }

    @Override
    public void onDeletedMessages() {
        service.onDeletedMessages();
    }
}