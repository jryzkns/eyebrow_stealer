package com.jek.Pokemote;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import java.io.File;

import static org.opencv.imgproc.Imgproc.fillPoly;

public class MainActivity extends AppCompatActivity implements
        CameraBridgeViewBase.CvCameraViewListener2 {

    // camera ind 0: back cam; camera ind 1: selfie cam
    private int                     currentCamera = 1;

    private CameraBridgeViewBase    cameraBridgeViewBase;
    private BaseLoaderCallback      baseLoaderCallback;

    private boolean                 isFrozen = false;
    private Mat                     processingFrame;

    private Detector                detect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // remove UI elements
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);     //  remove title bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);    //  remove notification bar
        setContentView(R.layout.activity_main);

        OpenCVLoader.initDebug(); // start opencv

        //  Request Camera permissions
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                MainActivity.this, Manifest.permission.CAMERA)) {
            } else {
                ActivityCompat.requestPermissions(
                MainActivity.this,new String[]{Manifest.permission.CAMERA},1);
            }
        }

        // start camera bridge view base
        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.CameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        cameraBridgeViewBase.setCameraIndex(currentCamera);
        cameraBridgeViewBase.setMaxFrameSize(AppUtils.frameW + 1, AppUtils.frameH + 1);

        // set up base loader
        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
                switch(status){
                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };

        processingFrame  = new Mat();

        detect           = new Detector(this.getApplicationContext(),
                            new File(getDir(    "cascade", Context.MODE_PRIVATE),
                                                "lbpcascade_frontalface.xml"),
                            R.raw.lbpcascade_frontalface,
                            new File(getDir(    "lbf", Context.MODE_PRIVATE),
                                                "lbfmodel.yaml"),
                            R.raw.lbfmodel);

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat baseFrame = inputFrame.rgba();

        if (!isFrozen) {
            // img has to be flipped so it displays correctly when selfie camera is used
            if (currentCamera == 1) { Core.flip(baseFrame, baseFrame, 1); }
        }

        Imgproc.cvtColor(baseFrame, processingFrame, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.equalizeHist(processingFrame, processingFrame);

        for (Rect b_ : detect.getFaces(processingFrame)) {
            b_ = AppUtils.rectExpand(b_, 1.7);
            Point[] k_ = detect.getKeyPoints(processingFrame
                    .colRange(  (int) b_.tl().x, (int) b_.br().x)
                    .rowRange(  (int) b_.tl().y, (int) b_.br().y));
            Mat p_ = AppUtils.getBlankFrame();
            fillPoly(p_, CvUtils.getExpandedROIContour(
                    new int[]{22, 23, 24, 25, 26},1.3, k_, b_), AppUtils.WHITE);
            fillPoly(p_, CvUtils.getExpandedROIContour(
                    new int[]{21, 17, 18, 19, 20},1.3, k_, b_), AppUtils.WHITE);
            Imgproc.cvtColor(p_, p_,Imgproc.COLOR_RGBA2GRAY);
            Imgproc.cvtColor(baseFrame, baseFrame,Imgproc.COLOR_RGBA2RGB);
            Photo.inpaint(baseFrame, p_, baseFrame,8, Photo.INPAINT_TELEA);
            Imgproc.cvtColor(baseFrame, baseFrame,Imgproc.COLOR_RGB2RGBA);
        }

        return baseFrame;
    }

    @Override protected void onResume() { super.onResume();
        if (!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"Load Failed!", Toast.LENGTH_SHORT).show();
        } else { baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS); } }
    @Override protected void onPause() { super.onPause();
        if (cameraBridgeViewBase != null){ cameraBridgeViewBase.disableView(); } }
    @Override protected void onDestroy() { super.onDestroy();
        if (cameraBridgeViewBase != null){ cameraBridgeViewBase.disableView(); } }
    @Override public void onCameraViewStarted(int width, int height) { }
    @Override public void onCameraViewStopped() { }

}
