package com.qmdeve.authenticator.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.DynamicColors;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.qmdeve.authenticator.R;
import com.qmdeve.authenticator.base.BaseActivity;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanActivity extends BaseActivity {

    private static final String TAG = "QRScanner";
    public static final String EXTRA_SCAN_RESULT = "scan_result";
    public static final int RESULT_SCAN_SUCCESS = 1001;
    public static final int RESULT_SCAN_FAILED = 1002;

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), this::onCameraPermissionResult);

    private PreviewView viewFinder;
    private ExecutorService cameraExecutor;
    private MultiFormatReader multiFormatReader;
    private boolean isResultReturned = false;
    private byte[] rotatedDataCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivitiesIfAvailable(getApplication());
        setContentView(R.layout.activity_scan);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        viewFinder = findViewById(R.id.viewFinder);
        cameraExecutor = Executors.newSingleThreadExecutor();

        initMultiFormatReader();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void initMultiFormatReader() {
        multiFormatReader = new MultiFormatReader();
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        List<BarcodeFormat> formats = new ArrayList<>();
        formats.add(BarcodeFormat.QR_CODE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        multiFormatReader.setHints(hints);
    }

    private void onCameraPermissionResult(boolean granted) {
        if (granted) {
            startCamera();
        } else {
            Toast.makeText(this, "需要相机权限", Toast.LENGTH_SHORT).show();
            setResult(RESULT_SCAN_FAILED);
            finish();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::scanBarcode);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                setResult(RESULT_SCAN_FAILED);
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void scanBarcode(ImageProxy imageProxy) {
        if (isResultReturned) {
            imageProxy.close();
            return;
        }

        try (imageProxy) {
            if (imageProxy.getPlanes().length == 0) {
                return;
            }

            ImageProxy.PlaneProxy plane = imageProxy.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            int width = imageProxy.getWidth();
            int height = imageProxy.getHeight();
            int rowStride = plane.getRowStride();

            int rotatedWidth = height;
            int rotatedHeight = width;

            if (rotatedDataCache == null || rotatedDataCache.length != rotatedWidth * rotatedHeight) {
                rotatedDataCache = new byte[rotatedWidth * rotatedHeight];
            }

            rotateYUV90(buffer, width, height, rowStride, rotatedDataCache);

            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                    rotatedDataCache,
                    rotatedWidth,
                    rotatedHeight,
                    0, 0,
                    rotatedWidth,
                    rotatedHeight,
                    false
            );

            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                Result result = multiFormatReader.decodeWithState(bitmap);
                if (result != null) {
                    handleResult(result.getText());
                }
            } catch (ReaderException ignored) {
            } finally {
                multiFormatReader.reset();
            }

        } catch (Exception e) {
            Log.e(TAG, "Analysis Error", e);
        }
    }

    private void rotateYUV90(ByteBuffer srcBuffer, int srcWidth, int srcHeight, int rowStride, byte[] dest) {
        srcBuffer.rewind();
        for (int y = 0; y < srcHeight; y++) {
            for (int x = 0; x < srcWidth; x++) {
                int srcIndex = y * rowStride + x;
                int destIndex = x * srcHeight + (srcHeight - 1 - y);
                dest[destIndex] = srcBuffer.get(srcIndex);
            }
        }
    }

    private void handleResult(String rawValue) {
        if (rawValue == null || isResultReturned) return;

        runOnUiThread(() -> {
            isResultReturned = true;
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_SCAN_RESULT, rawValue);
            setResult(RESULT_SCAN_SUCCESS, resultIntent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        rotatedDataCache = null;
    }
}