package com.mssp.qrverifier.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.mssp.qrverifier.crypto.TrustStore
import com.mssp.qrverifier.databinding.ActivityMainBinding
import com.mssp.qrverifier.model.QrVerifier
import com.mssp.qrverifier.model.VerificationResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

    // Evita processar múltiplos frames do mesmo QR
    private var lastScannedRaw: String = ""
    private var isShowingResult = false

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else {
            Toast.makeText(this, "Permissão de câmera necessária", Toast.LENGTH_LONG).show()
            binding.tvPermission.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnScanAgain.setOnClickListener { resetScanner() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QrAnalyzer { rawValue ->
                        if (!isShowingResult && rawValue != lastScannedRaw) {
                            lastScannedRaw = rawValue
                            runOnUiThread { handleScanResult(rawValue) }
                        }
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao iniciar câmera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleScanResult(raw: String) {
        isShowingResult = true
        showResultPanel()

        val result = QrVerifier.verify(raw)
        renderResult(result)
    }

    private fun renderResult(result: VerificationResult) {
        binding.apply {
            // Reset de estado visual
            resultCard.setCardBackgroundColor(0)
            tvResultTitle.text = ""
            tvResultSubtitle.text = ""
            tvResultData.text = ""
            tvRoleLabel.visibility = View.GONE
            tvRoleDescription.visibility = View.GONE
            ivResultIcon.setImageResource(0)

            when (result) {
                is VerificationResult.Valid -> {
                    val colorGreen = ContextCompat.getColor(
                        this@MainActivity, android.R.color.holo_green_dark
                    )
                    resultCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            this@MainActivity, com.mssp.qrverifier.R.color.bg_valid
                        )
                    )
                    ivResultIcon.setImageResource(com.mssp.qrverifier.R.drawable.ic_check_circle)
                    ivResultIcon.setColorFilter(colorGreen)
                    tvResultTitle.text = "✅ ACESSO AUTORIZADO"
                    tvResultTitle.setTextColor(colorGreen)
                    tvResultSubtitle.text = "Emissor: ${result.entry.name} (ID: ${result.certId})"
                    tvResultData.text = "Dado: ${result.data}"

                    // Nível de acesso
                    tvRoleLabel.text = result.entry.role.label
                    tvRoleDescription.text = result.entry.role.description
                    tvRoleLabel.visibility = View.VISIBLE
                    tvRoleDescription.visibility = View.VISIBLE

                    // Cor do badge por role
                    val badgeColor = when (result.entry.role) {
                        TrustStore.AccessRole.ADMIN ->
                            ContextCompat.getColor(this@MainActivity, com.mssp.qrverifier.R.color.role_admin)
                        TrustStore.AccessRole.FINANCEIRO ->
                            ContextCompat.getColor(this@MainActivity, com.mssp.qrverifier.R.color.role_financeiro)
                        TrustStore.AccessRole.VISITANTE ->
                            ContextCompat.getColor(this@MainActivity, com.mssp.qrverifier.R.color.role_visitante)
                    }
                    tvRoleLabel.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(badgeColor)
                }

                is VerificationResult.InvalidSignature -> {
                    val colorRed = ContextCompat.getColor(
                        this@MainActivity, android.R.color.holo_red_dark
                    )
                    resultCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            this@MainActivity, com.mssp.qrverifier.R.color.bg_invalid
                        )
                    )
                    ivResultIcon.setImageResource(com.mssp.qrverifier.R.drawable.ic_cancel_circle)
                    ivResultIcon.setColorFilter(colorRed)
                    tvResultTitle.text = "❌ ASSINATURA INVÁLIDA"
                    tvResultTitle.setTextColor(colorRed)
                    tvResultSubtitle.text = "ID: ${result.certId} — dado pode ter sido adulterado"
                    tvResultData.text = "Dado: ${result.data}"
                }

                is VerificationResult.UnknownIssuer -> {
                    val colorOrange = ContextCompat.getColor(
                        this@MainActivity, android.R.color.holo_orange_dark
                    )
                    resultCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            this@MainActivity, com.mssp.qrverifier.R.color.bg_unknown
                        )
                    )
                    ivResultIcon.setImageResource(com.mssp.qrverifier.R.drawable.ic_warning)
                    ivResultIcon.setColorFilter(colorOrange)
                    tvResultTitle.text = "⚠️ EMISSOR DESCONHECIDO"
                    tvResultTitle.setTextColor(colorOrange)
                    tvResultSubtitle.text = "ID '${result.certId}' não está no TrustStore"
                    tvResultData.text = ""
                }

                VerificationResult.MalformedPayload -> {
                    resultCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            this@MainActivity, com.mssp.qrverifier.R.color.bg_unknown
                        )
                    )
                    tvResultTitle.text = "⚠️ QR INVÁLIDO"
                    tvResultTitle.setTextColor(
                        ContextCompat.getColor(this@MainActivity, android.R.color.holo_orange_dark)
                    )
                    tvResultSubtitle.text = "Formato não reconhecido — não é um QR ECC"
                    tvResultData.text = ""
                }
            }
        }
    }

    private fun showResultPanel() {
        binding.viewFinder.visibility = View.INVISIBLE
        binding.overlayHint.visibility = View.GONE
        binding.resultPanel.visibility = View.VISIBLE
    }

    private fun resetScanner() {
        isShowingResult = false
        lastScannedRaw = ""
        binding.viewFinder.visibility = View.VISIBLE
        binding.overlayHint.visibility = View.VISIBLE
        binding.resultPanel.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "QRVerifier"
    }
}

/**
 * ImageAnalysis.Analyzer que usa ML Kit para detectar QR Codes no frame.
 * Chama [onQrDetected] na thread de análise quando um QR é encontrado.
 */
private class QrAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes
                    .filter { it.format == Barcode.FORMAT_QR_CODE }
                    .mapNotNull { it.rawValue }
                    .firstOrNull()
                    ?.let { onQrDetected(it) }
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
