/*
 * Created by guo on 2021/03/22.
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

package com.zalldata.analytics.android.sdk.encrypt;

import com.zalldata.analytics.android.sdk.ZALog;

import java.security.NoSuchAlgorithmException;

/**
 * RSA + AES 加密
 */
class ZARSAEncrypt implements SAEncryptListener {
    /**
     * 对称密钥
     */
    byte[] aesKey;

    /**
     * 加密后的对称密钥
     */
    String mEncryptKey;

    @Override
    public String symmetricEncryptType() {
        return "AES";
    }

    @Override
    public String encryptEvent(byte[] event) {
        return EncryptUtils.symmetricEncrypt(aesKey, event, SymmetricEncryptMode.AES);
    }

    @Override
    public String asymmetricEncryptType() {
        return "RSA";
    }

    @Override
    public String encryptSymmetricKeyWithPublicKey(String publicKey) {
        if (mEncryptKey == null) {
            try {
                aesKey = EncryptUtils.generateSymmetricKey(SymmetricEncryptMode.AES);
                mEncryptKey = EncryptUtils.encryptAESKey(publicKey, aesKey, "RSA");
            } catch (NoSuchAlgorithmException e) {
                ZALog.printStackTrace(e);
                return null;
            }
        }
        return mEncryptKey;
    }
}
