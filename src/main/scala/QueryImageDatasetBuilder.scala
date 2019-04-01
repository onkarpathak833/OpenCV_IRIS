import org.opencv.core.{Core, Mat, MatOfKeyPoint}
import org.opencv.features2d.{DescriptorExtractor, DescriptorMatcher, FeatureDetector}
import org.opencv.imgcodecs.Imgcodecs

class QueryImageDatasetBuilder extends Serializable {

   def buildDescriptorForQueryImage(imagePath:String, openCVJavaDll: String) = {

     //System.load(openCVJavaDll)
     System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    val img = Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_COLOR)
    val keypoints = new MatOfKeyPoint
    val descriptor = new Mat
    val detector = FeatureDetector.create(FeatureDetector.FAST)
    val extractor = DescriptorExtractor.create(DescriptorExtractor.ORB)
    detector.detect(img, keypoints)
    extractor.compute(img, keypoints, descriptor)
    val descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)
    descriptor

  }


}
