package com.catering.trade.payment.infrastructure.wechat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.catering.properties.WeChatProperties;
import com.catering.shared.constant.MessageConstant;
import com.catering.shared.exception.PaymentNotifyException;
import com.wechat.pay.contrib.apache.httpclient.auth.CertificatesVerifier;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Collections;

/**
 * 微信支付 API v3 通知的公共验签与 AES-GCM 解密组件。
 */
@Component
@RequiredArgsConstructor
public class WechatNotificationVerifier {

    private static final long MAX_TIMESTAMP_DIFFERENCE_SECONDS = 300L;
    private static final String AEAD_AES_256_GCM = "AEAD_AES_256_GCM";

    private final WeChatProperties weChatProperties;
    private volatile Verifier verifier;

    public VerifiedWechatNotification verifyAndDecrypt(
            String serial,
            String timestamp,
            String nonce,
            String signature,
            String body
    ) {
        validateHeaders(serial, timestamp, nonce, signature, body);
        validateTimestamp(timestamp);
        verifySignature(serial, timestamp, nonce, signature, body);

        try {
            JSONObject envelope = JSON.parseObject(body);
            JSONObject encryptedResource = envelope.getJSONObject("resource");
            if (encryptedResource == null
                    || !AEAD_AES_256_GCM.equals(encryptedResource.getString("algorithm"))) {
                throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_BODY_INVALID);
            }
            String plainText = decryptResource(encryptedResource);
            return new VerifiedWechatNotification(
                    envelope.getString("id"),
                    envelope.getString("event_type"),
                    JSON.parseObject(plainText)
            );
        } catch (PaymentNotifyException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_BODY_INVALID, ex);
        }
    }

    private void validateHeaders(String serial, String timestamp, String nonce, String signature, String body) {
        if (!StringUtils.hasText(serial)
                || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(nonce)
                || !StringUtils.hasText(signature)
                || !StringUtils.hasText(body)) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_HEADER_MISSING);
        }
    }

    private void validateTimestamp(String timestamp) {
        try {
            long callbackTime = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - callbackTime)
                    > MAX_TIMESTAMP_DIFFERENCE_SECONDS) {
                throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_TIMESTAMP_INVALID);
            }
        } catch (NumberFormatException ex) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_TIMESTAMP_INVALID, ex);
        }
    }

    private void verifySignature(String serial, String timestamp, String nonce, String signature, String body) {
        String message = timestamp + "\n" + nonce + "\n" + body + "\n";
        try {
            if (!getVerifier().verify(serial, message.getBytes(StandardCharsets.UTF_8), signature)) {
                throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_SIGNATURE_INVALID);
            }
        } catch (PaymentNotifyException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_SIGNATURE_INVALID, ex);
        }
    }

    private String decryptResource(JSONObject resource) throws Exception {
        String apiV3Key = weChatProperties.getApiV3Key();
        String ciphertext = resource.getString("ciphertext");
        String nonce = resource.getString("nonce");
        String associatedData = resource.getString("associated_data");
        if (!StringUtils.hasText(apiV3Key)
                || !StringUtils.hasText(ciphertext)
                || !StringUtils.hasText(nonce)) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_CONFIG_MISSING);
        }
        AesUtil aesUtil = new AesUtil(apiV3Key.getBytes(StandardCharsets.UTF_8));
        return aesUtil.decryptToString(
                (associatedData == null ? "" : associatedData).getBytes(StandardCharsets.UTF_8),
                nonce.getBytes(StandardCharsets.UTF_8),
                ciphertext
        );
    }

    private Verifier getVerifier() {
        if (verifier == null) {
            synchronized (this) {
                if (verifier == null) {
                    verifier = createVerifier();
                }
            }
        }
        return verifier;
    }

    private Verifier createVerifier() {
        String certificatePath = weChatProperties.getWeChatPayCertFilePath();
        if (!StringUtils.hasText(certificatePath)) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_CONFIG_MISSING);
        }
        try (InputStream inputStream = new FileInputStream(certificatePath)) {
            X509Certificate certificate = PemUtil.loadCertificate(inputStream);
            return new CertificatesVerifier(Collections.singletonList(certificate));
        } catch (Exception ex) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_CONFIG_MISSING, ex);
        }
    }
}
