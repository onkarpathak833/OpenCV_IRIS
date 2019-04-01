import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class IrisPupilDetect {


    private static Mat unsharpMask(Mat input_image, Size size, double sigma) {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        // Make sure the {input_image} is gray.
        Mat sharpened_image = new Mat(input_image.rows(), input_image.cols(), input_image.type());
        Mat Blurred_image = new Mat(input_image.rows(), input_image.cols(), input_image.type());
        Imgproc.GaussianBlur(input_image, Blurred_image, size, sigma);
        Core.addWeighted(input_image, 2.0D, Blurred_image, -1.0D, 0.0D, sharpened_image);
        return sharpened_image;
    }

    public static void testPupil(String filename, String outputFileName) {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Mat img = Imgcodecs.imread(filename);
        System.out.println(filename);
        Mat img_hue = new Mat(img.height(), img.width(), CvType.CV_8UC2);
        Mat hierarchy = new Mat(img.height(), img.width(), CvType.CV_8UC2);
        Mat circles = new Mat(img.height(), img.width(), CvType.CV_8UC2);

        //Imgproc.cvtColor(img, img_hue, Imgproc.COLOR_BGR2HSV);
//
//       Imgproc.cvtColor(img, img_hue, Imgproc.COLOR_RGB2GRAY);// COLOR_BGR2HSV);
        Imgproc.cvtColor(img, img_hue, Imgproc.COLOR_RGB2HSV);// COLOR_BGR2HSV);

        double threshold = Imgproc.threshold(img, img_hue,100,255,Imgproc.THRESH_BINARY);
        //Imgproc.cvtColor(img, img_hue, Imgproc.COLOR_RGB2RGBA);
        Core.inRange(img_hue, new Scalar(0, 0, 0), new Scalar(255, 255, 32), img_hue);


    }

    public static String detectEyePupil(String filename, String outputFileName, String openCVJavaDLL) {

        //System.load(openCVJavaDLL);

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Mat img = Imgcodecs.imread(filename);
        System.out.println(filename);
        Mat img_hue = new Mat(img.height(), img.width(), CvType.CV_8UC2);
        Mat hierarchy = new Mat(img.height(), img.width(), CvType.CV_8UC2);
        Mat circles = new Mat(img.height(), img.width(), CvType.CV_8UC2);

        //Imgproc.cvtColor(img, img_hue, Imgproc.COLOR_BGR2HSV);
//
//       Imgproc.cvtColor(img, img_hue, Imgproc.COLOR_RGB2GRAY);// COLOR_BGR2HSV);
        Imgproc.cvtColor(img, img_hue, Imgproc.COLOR_RGB2HSV);// COLOR_BGR2HSV);

        //Imgproc.cvtColor(img, img_hue, Imgproc.COLOR_RGB2RGBA);
        Core.inRange(img_hue, new Scalar(0, 0, 0), new Scalar(255, 255, 32), img_hue);

        //Imgproc.erode(img_hue, img_hue, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(6, 6)));

        //Imgproc.dilate(img_hue, img_hue, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(6, 6)));

        Imgproc.Canny(img_hue, img_hue, 170, 220);
        Imgproc.GaussianBlur(img_hue, img_hue, new Size(9, 9), 2, 2);
        // Apply Hough Transform to find the circles
        Imgproc.HoughCircles(img_hue, circles, Imgproc.CV_HOUGH_GRADIENT, 5, img_hue.rows(), 200, 75, 10, 25);

        int lastIndexOfSlash = filename.lastIndexOf("/");
        String imageName = filename.substring(lastIndexOfSlash, filename.length());
        String fileName = outputFileName + imageName;
        outputFileName = outputFileName + imageName.split("\\.")[0] + "_pupil.jpeg";

        System.out.println(outputFileName);
        if (circles.cols() > 0) {
            System.out.println("PUPIL DETECTED : "+imageName);
            for (int x = 0; x < circles.cols(); x++) {
                double vCircle[] = circles.get(0, x);

                if (vCircle == null)
                    break;

                Point pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
                int radius = (int) Math.round(vCircle[2]);

                // draw the found circle
                Imgproc.circle(img, pt, radius / 3, new Scalar(0, 255, 0), 15);
                Imgproc.circle(img, pt, radius/3 , new Scalar(0, 0, 255), 15);
            }
        }

        Imgcodecs.imwrite(outputFileName, img);

        return outputFileName;


    }


    public static void main(String[] args) {

        String applicationPath = "";
        applicationPath = args[0];
        Config applicationConfigs = ConfigFactory.parseFile(new File(applicationPath)).withFallback(ConfigFactory.load("application.conf")).resolve();
//  val irisDatasets = applicationConfigs.getString("sample_images_path")
//  val queryImagePath = applicationConfigs.getString("query_images_path")
        String openCVJavaLibs = applicationConfigs.getString("opencv_java_dylib");

//  val images: DataFrame = readImages(irisDatasets)
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // load opencv_java
        System.load(openCVJavaLibs);

        String filePath = "/Users/techops/Documents/FreelanceProjects/SampleImages/eye79.jpeg";
        String outputPath = "/Users/techops/Documents/FreelanceProjects/OutputImages";
        String openCVJavaDLL = "/Users/techops/Documents/FreelanceProjects/Applications/opencv-3.2.0/build/lib/libopencv_java320.dylib";
        detectEyePupil(filePath, outputPath, openCVJavaDLL);
        try {
            Thread.sleep(2000);

            System.out.println("sleeping...");
        } catch (Exception e) {
            System.out.println("sleeping...");
        }
    }


}
