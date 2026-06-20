package com.mssp.qrverifier.model

import com.mssp.qrverifier.crypto.TrustStore

/**
 * Representa o resultado da leitura + verificação de um QR Code.
 */
sealed class VerificationResult {

    /** QR válido: assinatura verificada com sucesso */
    data class Valid(
        val certId: String,
        val data: String,
        val entry: TrustStore.CertEntry
    ) : VerificationResult()

    /** Assinatura inválida — dado adulterado ou chave errada */
    data class InvalidSignature(val certId: String, val data: String) : VerificationResult()

    /** Emissor desconhecido (ID não está no TrustStore) */
    data class UnknownIssuer(val certId: String) : VerificationResult()

    /** QR Code não segue o formato esperado ID|DADO|ASSINATURA */
    object MalformedPayload : VerificationResult()
}

object QrVerifier {

    /**
     * Analisa e verifica um payload no formato: ID|DADO|ASSINATURA_HEX
     * Retorna um [VerificationResult] com o estado da verificação.
     */
    fun verify(rawPayload: String): VerificationResult {
        val parts = rawPayload.split("|")

        if (parts.size != 3) return VerificationResult.MalformedPayload

        val (certId, data, signatureHex) = parts

        if (certId.isBlank() || data.isBlank() || signatureHex.isBlank()) {
            return VerificationResult.MalformedPayload
        }

        val entry = TrustStore.getEntry(certId)
            ?: return VerificationResult.UnknownIssuer(certId)

        val valid = TrustStore.verify(certId, data, signatureHex)

        return if (valid) {
            VerificationResult.Valid(certId, data, entry)
        } else {
            VerificationResult.InvalidSignature(certId, data)
        }
    }
}
