package app.objectrecognition;

import android.graphics.Bitmap;

import org.opencv.android.Utils;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class Recognizer {

    private Mat imgTemplate;

    public Recognizer(Bitmap bitmap) {
        imgTemplate = new Mat();
        Utils.bitmapToMat(bitmap, imgTemplate);
        Imgproc.cvtColor(imgTemplate, imgTemplate, Imgproc.COLOR_RGB2GRAY);
        //converting the image to match with the type of the cameras image
        imgTemplate.convertTo(imgTemplate, 0);
    }

    public boolean detect(Mat img_display) {
        Mat result = new Mat();
        // Create the result matrix
        result.create(imgTemplate.cols(), imgTemplate.rows(), CvType.CV_32FC1);

        // Do the Matching and Normalize
        //using TM_CCOEFF_NORMED result will be between 0..1 (1=perfect match)
        int match_method = Imgproc.TM_CCOEFF_NORMED ;
        Imgproc.matchTemplate(img_display, imgTemplate, result, match_method);

        Core.MinMaxLocResult minMaxLocResult = Core.minMaxLoc(result, new Mat());
        result.release();
        double thresholdMatch = 0.75;
        double matchValue = minMaxLocResult.maxVal;

        if (matchValue > thresholdMatch) {
            return true;
        } else {
            return false;
        }
    }
}
