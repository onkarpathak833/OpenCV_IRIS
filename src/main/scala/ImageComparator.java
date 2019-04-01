import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.BOWImgDescriptorExtractor;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FastFeatureDetector;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;

import java.awt.*;
import java.io.Serializable;

public class ImageComparator implements Serializable {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    //test main method
    public static void main(String[] args) {
        // Set image path
        String filename1 = "/Users/techops/Documents/FreelanceProjects/SampleImages/Image1.jpg";
        String filename2 = "/Users/techops/Documents/queryImage.jpeg";

        ImageComparator comp = new ImageComparator();
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        boolean ret;
        ret = comp.compareFeature(filename1, filename2);

        System.out.println(ret);
//        if (ret > 0) {
//            System.out.println("Two images are same.");
//        } else {
//            System.out.println("Two images are different.");
//        }

    }


    /**
     * Compare that two images is similar using feature mapping
     * @author minikim
     * @param filename1 - the first image
     * @param filename2 - the second image
     * @return integer - count that has the similarity within images
     */
    public boolean  compareFeature(String filename1, String filename2) {
        int retVal = 0;
        long startTime = System.currentTimeMillis();

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Load images to compare
        Mat img1 = Imgcodecs.imread(filename1, Imgcodecs.IMREAD_COLOR);
        Mat img2 = Imgcodecs.imread(filename2, Imgcodecs.IMREAD_COLOR);

        // Declare key point of images
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat();
        Mat descriptors2 = new Mat();

        // Definition of ORB key point detector and descriptor extractors
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);

        // Detect key points
        detector.detect(img1, keypoints1);
        detector.detect(img2, keypoints2);


        extractor.compute(img1, keypoints1, descriptors1);
        extractor.compute(img2, keypoints2, descriptors2);

        // Definition of descriptor matcher
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        // Match points of two images
        MatOfDMatch matches = new MatOfDMatch();

        // Avoid to assertion failed
        // Assertion failed (type == src2.type() && src1.cols == src2.cols && (type == CV_32F || type == CV_8U)
        if (descriptors2.cols() == descriptors1.cols()) {
            matcher.match(descriptors1, descriptors2 ,matches);

            // Check matches of key points
            DMatch[] match = matches.toArray();
            double max_dist = 0; double min_dist = 100;

            for (int i = 0; i < descriptors1.rows(); i++) {
                double dist = match[i].distance;
//                System.out.println("distance : "+dist);
                if( dist < min_dist ) min_dist = dist;
                if( dist > max_dist ) max_dist = dist;
            }
//            System.out.println("max_dist=" + max_dist + ", min_dist=" + min_dist);

            // Extract good images (distances are under 10)
            for (int i = 0; i < descriptors1.rows(); i++) {
                if (match[i].distance <= 10) {
                    retVal++;
                }
            }
        }

        long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println("estimatedTime=" + estimatedTime + "ms");

        return retVal > 0;
    }

    public boolean ifImagesEqual(Mat image1, Mat image2) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        int retVal = 0;
        long startTime = System.currentTimeMillis();
        // Match points of two images
        MatOfDMatch matches = new MatOfDMatch();

        if (image1.cols() == image1.cols()) {
            matcher.match(image1, image2 ,matches);

            // Check matches of key points
            DMatch[] match = matches.toArray();
            double max_dist = 0; double min_dist = 100;

            for (int i = 0; i < image1.rows(); i++) {
                double dist = 0;
                if(i >= (match.length-1)) {
                     dist = match[i].distance;
                    System.out.println("distance : "+dist);
                    if( dist < min_dist ) min_dist = dist;
                }

                if( dist > max_dist ) max_dist = dist;
            }
            System.out.println("max_dist=" + max_dist + ", min_dist=" + min_dist);

            // Extract good images (distances are under 10)
            for (int i = 0; i < image1.rows(); i++) {
                if (match[i].distance <= 10) {
                    retVal++;
                }
            }
        }

        if(retVal > 0) {
            return true;
        }

        return false;
    }




}
