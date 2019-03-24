import java.nio.file.Paths
import java.util

import IrisApp.detectFace
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.ml.image.ImageSchema
import org.apache.spark.ml.image.ImageSchema.{getData, getHeight, getOrigin, getWidth}
import org.apache.spark.sql.{DataFrame, Dataset, Row}
import org.opencv.core.{Core, Mat, MatOfKeyPoint}
import org.opencv.features2d.{DescriptorExtractor, DescriptorMatcher, FeatureDetector}
import org.opencv.imgcodecs.Imgcodecs

class IRISDatasetBuilder extends Serializable with LazyLogging {

  def buildIRISDataset(pathToDataset:String) = {

    val images = ImageSchema.readImages(pathToDataset)
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

    implicit val encoder = org.apache.spark.sql.Encoders.kryo[Mat]
    val irisDatasetDescriptors = images.map(rrow => {
      val row: Row = rrow.getAs[Row](0)
      val filename: String = Paths.get(getOrigin(row)).toAbsolutePath.toString.split("file:")(1)
      println(filename)
      val imageData: Array[Byte] = getData(row)
      val height: Int = getHeight(row)
      val width: Int = getWidth(row)
      logger.info("[IRISDatasetBuilder] - Adding image {} to IRIS Dataset",filename)
      logger.info("[IRISDatasetBuilder] - Image {} size is {}", filename,height*width)
      buildImageMatrix(filename)
    })

    irisDatasetDescriptors.collect().toList

  }

  private def buildImageMatrix(filePath:String) = {
    val img = Imgcodecs.imread(filePath, Imgcodecs.IMREAD_COLOR)
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
