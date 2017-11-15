package app.objectrecognition;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

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

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";

    public enum Option {SELECT, TRACK, STOP_TRACK}

    private MenuItem select;
    private MenuItem track;
    private MenuItem stopTrack;
    private static Option mode = Option.SELECT;

    private CameraBridgeViewBase mOpenCvCameraView;

    private int w, h;


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
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.open_cv_activity);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

    }

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item == select)
            mode = Option.SELECT;
        if (item == track)
            mode = Option.TRACK;
        else if (item == stopTrack)
            mode = Option.STOP_TRACK;
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
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
        w = width;
        h = height;
    }

    @Override
    public void onCameraViewStopped() {

    }

    private Scalar RED = new Scalar(255, 0, 0);
    private Scalar GREEN = new Scalar(0, 255, 0);

    private int x1 = 0, y1 = 0;
    private int x2 = 0, y2 = 0;

    private int[] ROI_Center = {0, 0};
    private int ROI_Width = 0;
    private int ROI_Height = 0;
    private Mat selectedCameraArea;

    private boolean trackInit = false;

    private Mat currentlyFrame;
    private Mat tmpFrame;
    Mat currentlyFrameGray = new Mat();
    Mat result = new Mat();
    Mat img_display = new Mat();

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        currentlyFrame = inputFrame.rgba();

        switch (mode) {
            case SELECT:
                if (tmpFrame != null) {
                    tmpFrame.release();
                }

                ROI_Width = Math.abs(x2 - x1);
                ROI_Height = Math.abs(y2 - y1);
                ROI_Center[0] = Math.max(x1, x2) - ROI_Width / 2;
                ROI_Center[1] = Math.max(y1, y2) - ROI_Height / 2;
                tmpFrame = currentlyFrame.clone();
                Imgproc.circle(currentlyFrame, new Point(ROI_Center[0], ROI_Center[1]), 10, GREEN);
                Imgproc.rectangle(currentlyFrame, new Point(x1, y1), new Point(x2, y2), GREEN);

                break;
            case TRACK:

                if (!trackInit) {
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

                    trackInit = true;
                }


                /// Source image to display
                inputFrame.gray().copyTo(img_display);

                /// Create the result matrix
                int result_cols = img_display.cols() - selectedCameraArea.cols() + 1;
                int result_rows = img_display.rows() - selectedCameraArea.rows() + 1;
                result.create(result_rows, result_cols, CvType.CV_32FC1);

                /// Do the Matching and Normalize
                int match_method = Imgproc.TM_SQDIFF;
                Imgproc.matchTemplate(img_display, selectedCameraArea, result, match_method);

                Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());

                Core.MinMaxLocResult minMaxLocResult = Core.minMaxLoc(result, new Mat());

                Point matchLoc = null;
                /// For SQDIFF and SQDIFF_NORMED, the best matches are lower values. For all the other methods, the higher the better
                if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
                    matchLoc = minMaxLocResult.minLoc;
                } else {
                    matchLoc = minMaxLocResult.maxLoc;
                }

                Imgproc.rectangle(currentlyFrame, matchLoc, new Point(matchLoc.x + selectedCameraArea.cols(), matchLoc.y + selectedCameraArea.rows()), new Scalar(255, 0, 0));
                //Imgproc.rectangle(result, matchLoc, new Point(matchLoc.x + imPatch.cols(), matchLoc.y + imPatch.rows()), new Scalar(255, 0, 0));

//
//                Imgproc.circle(mRgba, new Point(ROI_Center[0],ROI_Center[1]), 10, new Scalar(0, 255, 0, 255));
//                Imgproc.rectangle(mRgba, new Point(ROI_Center[0]-ROI_Width/2,ROI_Center[1]-ROI_Height/2), new Point(ROI_Center[0]+ROI_Width/2,ROI_Center[1]+ROI_Height/2),  new Scalar(0, 255, 0, 255));

                break;

            case STOP_TRACK:
                trackInit = false;
                break;
        }
        System.gc();
        return currentlyFrame;
    }
}
