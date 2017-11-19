package app.objectrecognition;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";
    private static final String detectImageFileName = "android_logo.png";
    private static final String url = "https://www.android.com/";
    private static final Scalar RED = new Scalar(255, 0, 0);
    private static final Scalar GREEN = new Scalar(0, 255, 0);

    private enum Option {SELECT, TRACK, STOP_TRACK, DETECT}

    private static Option mode = Option.SELECT;
    private MenuItem select;
    private MenuItem track;
    private MenuItem stopTrack;
    private MenuItem view3d;
    private MenuItem gps;
    private MenuItem simpleCamera;
    private MenuItem detect;


    private CameraBridgeViewBase mOpenCvCameraView;
    private Intent camera3D;
    private Intent camera;
    private Intent gpsSensor;
    private Recognizer recognizer;

    private int x1 = 0, y1 = 0;
    private int x2 = 0, y2 = 0;
    private int[] ROI_Center = {0, 0};
    private int ROI_Width = 0;
    private int ROI_Height = 0;
    private Mat selectedCameraArea;
    private boolean trackInit = false;

    static {
        if (!OpenCVLoader.initDebug())
            Log.e("ERROR", "OpenCVLoader.initDebug(), not working.");
        else
            Log.d("SUCCESS", "OpenCVLoader.initDebug(), working.");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    AssetManager assetManager = getAssets();
                    InputStream inputStream = null;
                    try {
                        inputStream = assetManager.open(detectImageFileName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    recognizer = new Recognizer(bitmap);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.open_cv_activity);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            return;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        camera3D = new Intent(MainActivity.this, Camera3DActivity.class);
        gpsSensor = new Intent(MainActivity.this, GPSSensorActivity.class);
        camera = new Intent(MainActivity.this, CameraActivity.class);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.open_cv_activity);
        mOpenCvCameraView.setMaxFrameSize(640, 480);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        select = menu.add("Select Target");
        track = menu.add("Start Track");
        stopTrack = menu.add("Stop Track");
        view3d = menu.add("3D View");
        gps = menu.add("GPS & Sensor");
        simpleCamera = menu.add("Camera");
        detect = menu.add("Detect");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item == select) {
            mode = Option.SELECT;
        } else if (item == track) {
            mode = Option.TRACK;
        } else if (item == stopTrack) {
            mode = Option.STOP_TRACK;
        } else if (item == detect) {
            mode = Option.DETECT;
        } else if (item == view3d) {
            startActivity(camera3D);
        } else if (item == gps) {
            startActivity(gpsSensor);
        } else if (item == simpleCamera) {
            startActivity(camera);
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX() / 2;
        int y = (int) event.getY() / 2;
        if (mode == Option.SELECT) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x1 = x;
                    y1 = y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    x2 = x;
                    y2 = y;
                    break;
                case MotionEvent.ACTION_UP:
                    x2 = x;
                    y2 = y;
                    break;
            }
        }
        return false;
    }

    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat currentlyFrameRGBA = inputFrame.rgba();
        switch (mode) {
            case SELECT:
                cutSelectedArea(currentlyFrameRGBA);
                break;
            case TRACK:
                if (!trackInit) {
                    initTrack(currentlyFrameRGBA);
                    trackInit = true;
                }
                trackObject(inputFrame, currentlyFrameRGBA);
                break;
            case STOP_TRACK:
                trackInit = false;
                break;
            case DETECT:
                detect(inputFrame);
                break;
        }
        System.gc();
        return currentlyFrameRGBA;
    }

    private void cutSelectedArea(Mat currentlyFrameRGBA) {
        ROI_Width = Math.abs(x2 - x1);
        ROI_Height = Math.abs(y2 - y1);
        ROI_Center[0] = Math.max(x1, x2) - ROI_Width / 2;
        ROI_Center[1] = Math.max(y1, y2) - ROI_Height / 2;
        Imgproc.circle(currentlyFrameRGBA, new Point(ROI_Center[0], ROI_Center[1]), 10, GREEN);
        Imgproc.rectangle(currentlyFrameRGBA, new Point(x1, y1), new Point(x2, y2), GREEN);
    }

    private void initTrack(Mat currentlyFrameRGBA) {
        Mat currentlyFrameGray = new Mat();
        Mat tmpFrame = currentlyFrameRGBA.clone();
        Imgproc.cvtColor(tmpFrame, currentlyFrameGray, Imgproc.COLOR_RGB2GRAY);

        Size imgSize = currentlyFrameGray.size();
        int height = (int) imgSize.height;
        int width = (int) imgSize.width;
        int y = ROI_Center[1] - ROI_Height / 2;
        int x = ROI_Center[0] - ROI_Width / 2;

        int r = Math.max(y, 0);
        int r2 = Math.min(height - 1, y + ROI_Height - 1);
        int c = Math.max(x, 0);
        int c2 = Math.min(width - 1, x + ROI_Width - 1);

        selectedCameraArea = currentlyFrameGray.submat(r, r2, c, c2);
        if (currentlyFrameGray != null) {
            currentlyFrameGray.release();
        }
        if (tmpFrame != null) {
            tmpFrame.release();
        }
    }

    private void trackObject(CameraBridgeViewBase.CvCameraViewFrame inputFrame, Mat currentlyFrameRGBA) {
        // Source image to display
        Mat imgDisplay = new Mat();
        inputFrame.gray().copyTo(imgDisplay);

        // Create the result matrix
        int result_cols = imgDisplay.cols() - selectedCameraArea.cols() + 1;
        int result_rows = imgDisplay.rows() - selectedCameraArea.rows() + 1;
        Mat result = new Mat();
        result.create(result_rows, result_cols, CvType.CV_32FC1);

        // Do the Matching and Normalize
        int match_method = Imgproc.TM_SQDIFF;
        Imgproc.matchTemplate(imgDisplay, selectedCameraArea, result, match_method);

        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());

        Core.MinMaxLocResult minMaxLocResult = Core.minMaxLoc(result, new Mat());

        Point matchLoc = null;
        // For SQDIFF and SQDIFF_NORMED, the best matches are lower values.
        // For all the other methods, the higher the better
        if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
            matchLoc = minMaxLocResult.minLoc;
        } else {
            matchLoc = minMaxLocResult.maxLoc;
        }

        if (imgDisplay != null) {
            imgDisplay.release();
        }
        if (result != null) {
            result.release();
        }

        Imgproc.rectangle(currentlyFrameRGBA, matchLoc,
                new Point(matchLoc.x + selectedCameraArea.cols(),
                        matchLoc.y + selectedCameraArea.rows()), RED);
    }

    private void detect(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.gray().clone();
        boolean match = recognizer.detect(frame);
        if (match) {
            openWebURL(url);
        }
        frame.release();
    }

    private void openWebURL(String url) {
        Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browse);
    }
}