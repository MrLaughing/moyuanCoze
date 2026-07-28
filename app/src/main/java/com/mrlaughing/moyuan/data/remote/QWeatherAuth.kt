package com.mrlaughing.moyuan.data.remote

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

/**
 * 和风天气 JWT 身份认证工具。
 *
 * 按官方文档实现（Ed25519 / EdDSA 算法）：
 * https://dev.qweather.com/docs/configuration/authentication/#json-web-token
 *
 * Header:  {"alg":"EdDSA","kid":"<凭据ID>"}
 * Payload: {"sub":"<项目ID>","iat":<now-30s>,"exp":<iat+900s>}
 * Signature: 对 base64url(header).base64url(payload) 用 Ed25519 私钥签名，再 base64url
 * 最终 token = header.payload.signature
 */
object QWeatherAuth {

    private val provider = BouncyCastleProvider()

    /**
     * 生成 JWT（Base64URL 无填充）。
     *
     * @param projectId   项目 ID（对应 Payload 的 sub）
     * @param keyId       凭据 ID（对应 Header 的 kid）
     * @param privateKeyB64 Ed25519 私钥 PKCS8 的裸 Base64（单行、无 PEM 头尾/换行）
     */
    fun createToken(projectId: String, keyId: String, privateKeyB64: String): String {
        val privateKey = loadPrivateKey(privateKeyB64)
        val header = """{"alg":"EdDSA","kid":"$keyId"}"""
        val now = System.currentTimeMillis() / 1000L
        val iat = now - 30L
        val exp = iat + 900L
        val payload = """{"sub":"$projectId","iat":$iat,"exp":$exp}"""

        val headerB64 = base64url(header.toByteArray(Charsets.UTF_8))
        val payloadB64 = base64url(payload.toByteArray(Charsets.UTF_8))
        val data = "$headerB64.$payloadB64"

        val sig = Signature.getInstance("Ed25519", provider)
        sig.initSign(privateKey)
        sig.update(data.toByteArray(Charsets.UTF_8))
        val signatureB64 = base64url(sig.sign())

        return "$data.$signatureB64"
    }

    private fun loadPrivateKey(b64: String): PrivateKey {
        val der = Base64.getDecoder().decode(b64)
        val keySpec = PKCS8EncodedKeySpec(der)
        return KeyFactory.getInstance("Ed25519", provider).generatePrivate(keySpec)
    }

    private fun base64url(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
