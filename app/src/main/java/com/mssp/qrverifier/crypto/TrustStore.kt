package com.mssp.qrverifier.crypto

import android.util.Log
import java.math.BigInteger
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.ECParameterSpec
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.AlgorithmParameters

/**
 * Repositório de chaves públicas dos emissores autorizados.
 *
 * As chaves públicas são os valores "pub" do emissor HTML (formato uncompressed 04||X||Y).
 * A chave privada NUNCA é armazenada aqui — somente o lado verificador precisa da pública.
 *
 * Para adicionar um novo emissor, inclua uma entrada em [entries] com:
 *  - id:      mesmo ID usado no payload (ex: "01")
 *  - name:    nome de exibição
 *  - role:    nível de acesso
 *  - pubHex:  chave pública uncompressed (128 hex chars = 64 bytes X + 64 bytes Y, sem o prefixo "04")
 */
object TrustStore {

    data class CertEntry(
        val id: String,
        val name: String,
        val role: AccessRole,
        val pubHex: String   // 128 hex chars — X||Y sem prefixo 04
    )

    enum class AccessRole(val label: String, val description: String) {
        ADMIN(
            label = "Administrador",
            description = "Acesso total — todas as áreas"
        ),
        FINANCEIRO(
            label = "Financeiro",
            description = "Acesso operacional — áreas financeiras"
        ),
        VISITANTE(
            label = "Visitante",
            description = "Acesso restrito — áreas públicas apenas"
        )
    }

    /**
     * Chaves públicas extraídas do emissor HTML (campo "pub" do trustStore).
     * Somente o certificado 01 (ADMIN) tem chave real válida P-256.
     * Os certificados 02 e 03 têm chaves simuladas inválidas — substituir por pares reais.
     */
    private val entries = mapOf(
        "01" to CertEntry(
            id = "01",
            name = "ADMIN",
            role = AccessRole.ADMIN,
            // Chave pública real P-256 do emissor (sem prefixo 04)
            pubHex = "590a616ba5d36418b53b5ae694d15142892b1cbe409d09269e754655282d86a4" +
                     "a1134723750b7d591efd7d29001fe98f3bd65d122cd5d48b1328efb6c1cdb198"
        ),
        "02" to CertEntry(
            id = "02",
            name = "FINANCEIRO",
            role = AccessRole.FINANCEIRO,
            // ⚠ Chave simulada — substituir por chave real antes de produção
            pubHex = "e440e369f24c21142a31a12fbe7780ab3ae21705d3c73f33f15bc816842ba900" +
                     "13c026dbf7ed547a50552484d1dbe90d516b1d160a4240356d770c80019aaa7a"
        ),
        "03" to CertEntry(
            id = "03",
            name = "VISITANTE",
            role = AccessRole.VISITANTE,
            // ⚠ Chave simulada — substituir por chave real antes de produção
            pubHex = "53af5b96bd8d7c5a0fae4a2dac905a904531b9503dbcf197190322c39410cda7" +
                     "84f93074f641bb2af5138728eb2ba93756034ac505fd4745ce28d65aaa8b4446"
        )
    )

    fun getEntry(id: String): CertEntry? = entries[id]

    /**
     * Constrói um ECPublicKey a partir dos bytes X||Y (64+64) da chave uncompressed P-256.
     */
    private fun buildPublicKey(pubHex: String): ECPublicKey {
        require(pubHex.length == 128) { "pubHex deve ter 128 hex chars (64 bytes X + 64 bytes Y)" }

        val xBytes = pubHex.substring(0, 64).hexToByteArray()
        val yBytes = pubHex.substring(64, 128).hexToByteArray()

        val x = BigInteger(1, xBytes)
        val y = BigInteger(1, yBytes)

        val params = AlgorithmParameters.getInstance("EC").apply {
            init(ECGenParameterSpec("secp256r1"))
        }.getParameterSpec(ECParameterSpec::class.java)

        val spec = ECPublicKeySpec(ECPoint(x, y), params)
        return KeyFactory.getInstance("EC").generatePublic(spec) as ECPublicKey
    }

    /**
     * Verifica a assinatura DER-encoded (hex) sobre [data] usando a chave do emissor [certId].
     *
     * @return true se assinatura válida e certificado conhecido, false caso contrário
     */
    fun verify(certId: String, data: String, signatureHex: String): Boolean {
        val entry = entries[certId] ?: run {
            Log.w("TrustStore", "Certificado desconhecido: $certId")
            return false
        }

        return try {
            val publicKey = buildPublicKey(entry.pubHex)
            val sigBytes = signatureHex.hexToByteArray()

            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initVerify(publicKey)
            sig.update(data.toByteArray(Charsets.UTF_8))
            sig.verify(sigBytes)
        } catch (e: Exception) {
            Log.e("TrustStore", "Erro na verificação: ${e.message}")
            false
        }
    }

    private fun String.hexToByteArray(): ByteArray {
        val s = if (length % 2 != 0) "0$this" else this
        return ByteArray(s.length / 2) { i ->
            s.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
