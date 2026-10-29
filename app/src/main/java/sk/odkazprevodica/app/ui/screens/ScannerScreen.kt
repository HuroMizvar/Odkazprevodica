package sk.odkazprevodica.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import sk.odkazprevodica.app.R
import sk.odkazprevodica.app.data.PlateParser
import sk.odkazprevodica.app.data.displayPlate
import sk.odkazprevodica.app.ui.components.BackHeader
import sk.odkazprevodica.app.ui.components.PlateLarge
import sk.odkazprevodica.app.ui.theme.AppColors
import java.util.concurrent.Executors

/**
 * Skener ŠPZ. Rozpoznávanie beží v telefóne (ML Kit), fotka sa nikam neposiela.
 * ŠPZ sa ponúkne až keď ju rozpozná 2× za sebou rovnako (menej omylov).
 */
@Composable
fun ScannerScreen(onBack: () -> Unit, onPlate: (String) -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var askedOnce by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        askedOnce = true
    }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    var candidate by remember { mutableStateOf<String?>(null) }
    var stable by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = { BackHeader(stringResource(R.string.scan_title), onBack = onBack) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            if (granted) {
                Box(
                    Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)
                        .background(AppColors.Surface, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    CameraPreview(
                        onText = { lines ->
                            val found = PlateParser.find(lines)
                            if (found != null && found == candidate) stable = found
                            candidate = found
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Rámik na ŠPZ
                    Box(
                        Modifier.fillMaxWidth(0.8f).height(90.dp)
                            .border(BorderStroke(3.dp, AppColors.Amber), RoundedCornerShape(12.dp)),
                    )
                }
                Text(
                    stringResource(R.string.scan_hint),
                    color = AppColors.TextMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                )
                val found = stable
                if (found != null) {
                    PlateLarge(displayPlate(found))
                }
                Button(
                    onClick = { stable?.let(onPlate) },
                    enabled = stable != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Amber, contentColor = AppColors.OnAmber,
                        disabledContainerColor = AppColors.Surface, disabledContentColor = AppColors.TextMuted,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp).height(56.dp),
                ) {
                    Text(
                        stringResource(if (stable != null) R.string.scan_use else R.string.scan_searching),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            } else {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.scan_no_permission),
                        color = AppColors.Text,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                    if (askedOnce) {
                        OutlinedButton(
                            onClick = { launcher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.padding(top = 16.dp),
                        ) { Text(stringResource(R.string.scan_allow), color = AppColors.Amber) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(onText: (List<String>) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnText by rememberUpdatedState(onText)
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor, PlateAnalyzer(recognizer) { currentOnText(it) })
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { if (providerFuture.isDone) providerFuture.get().unbindAll() }
            executor.shutdown()
            recognizer.close()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private class PlateAnalyzer(
    private val recognizer: TextRecognizer,
    private val onText: (List<String>) -> Unit,
) : ImageAnalysis.Analyzer {

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val media = image.image
        if (media == null) {
            image.close()
            return
        }
        val input = InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees)
        recognizer.process(input)
            // Výsledok príde na hlavné vlákno – bezpečné pre Compose stav.
            .addOnSuccessListener { result -> onText(result.textBlocks.flatMap { b -> b.lines.map { it.text } }) }
            .addOnCompleteListener { image.close() }
    }
}
