package com.example.a230508;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;

public class Client extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;

    private CameraBridgeViewBase mCameraView;
    private Mat mFrame;

    // Socket 相關變數
    private Socket clientSocket;
    private DataOutputStream dataOutputStream;
    private int YOUR_SERVER_PORT = 5000;
    private String YOUR_SERVER_IP = "163.14.68.47";

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");
                mCameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mCameraView = (JavaCameraView) findViewById(R.id.camera_view);
        mCameraView.setVisibility(SurfaceView.VISIBLE);
        mCameraView.setCvCameraViewListener(this);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            initializeOpenCV();
        }
    }

    private void initializeOpenCV() {
        OpenCVLoader.initDebug();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeOpenCV();
            } else {
                Log.e(TAG, "Camera permission denied");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mFrame = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {

        mFrame.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mFrame = inputFrame.rgba();
        // 傳送影像
        try {
            if (clientSocket == null) {
                // 建立 Socket 連線
                clientSocket = new Socket(YOUR_SERVER_IP, YOUR_SERVER_PORT);
                dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
            }

            // 将图像帧转JPEG格式
            Bitmap bitmap = Bitmap.createBitmap(mFrame.cols(), mFrame.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mFrame, bitmap);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            // 发送JPEG图像数据
            dataOutputStream.writeInt(byteArray.length);
            dataOutputStream.write(byteArray);

            Log.i(TAG, "影像傳送成功");
        } catch (IOException e) {
            Log.e(TAG, "影像傳送失敗: " + e.getMessage());
        }
        return mFrame;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCameraView != null && !OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraView != null) {
            mCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraView != null) {
            mCameraView.disableView();
        }

        // 關閉 socket
        try {
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
