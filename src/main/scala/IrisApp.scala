import java.awt.image.BufferedImage
import java.io.File

import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
import java.nio.file.{Path, Paths}
import java.util

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import javax.imageio.ImageIO
import org.apache.spark.ml.image.ImageSchema
import org.apache.spark.ml.image.ImageSchema._
import org.apache.spark.rdd.RDD
import org.opencv.core._
import org.opencv.features2d.{BOWImgDescriptorExtractor, DescriptorMatcher}
import org.opencv.objdetect.CascadeClassifier

import scala.util.Random


object IrisApp extends Serializable with LazyLogging {
  lazy val irisDatasetBuilder = new IRISDatasetBuilder
  lazy val queryImageDatasetBuilder = new QueryImageDatasetBuilder
  lazy val ImageComparator = new ImageComparator
  var applicationPath: String = ""

  def main(args: Array[String]): Unit = {
    val sparkSession = SparkSession.builder().appName("Consumer").master("local[*]").getOrCreate()

    if (args.length < 1) {
      throw new Exception("application.conf file path not provided.")
    }

    applicationPath = args(0)

    val applicationConfigs: Config = ConfigFactory.parseFile(new File(applicationPath)).withFallback(ConfigFactory.load("application.conf")).resolve()
    val irisDatasets = applicationConfigs.getString("sample_images_path")
    val queryImagePath = applicationConfigs.getString("query_images_path")
    val openCVJavaLibs = applicationConfigs.getString("opencv_java_dylib")
    val outputImagePath = applicationConfigs.getString("image_output_path")

    val images: DataFrame = readImages(irisDatasets)
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // load opencv_java
    System.load(openCVJavaLibs)

    val irisDataset = irisDatasetBuilder.buildIRISDataset(irisDatasets)
    val queryImage = queryImageDatasetBuilder.buildDescriptorForQueryImage(queryImagePath)

    val data: Dataset[Row] = images.filter(imageRow => {
      val row: Row = imageRow.getAs[Row](0)
      val filename = Paths.get(getOrigin(row)).toAbsolutePath.toString.split("file:")(1)
      logger.info("[IrisApp] - Detect face and eyes in the image")
      detectFace(filename, outputImagePath)
      println(filename)
      val comparison = ImageComparator.compareFeature(filename, queryImagePath)
      println(comparison)
      comparison
    })

    val ifExist = data.collect().toList.nonEmpty

    if (ifExist) {
      println(s"[IrisApp] - Image at Path ${queryImagePath} EXISTS in IRIS Datasets ")
    }
    else {
      println(s"[IrisApp] - Image at Path ${queryImagePath} DOES NOT EXIST in IRIS Datasets")
    }
  }

  private def checkIfImagesAreEqual(file1: String, file2: String): Boolean = {
    ImageComparator.compareFeature(file1, file2)
  }

  private def checkIfImageExists(irisDataset: List[Mat], queryImage: Mat) = {

    var retVal = 0
    val startTime = System.currentTimeMillis

    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    val filteredDataset: List[Mat] = irisDataset.filter(image => ImageComparator.ifImagesEqual(image, queryImage))

    filteredDataset.nonEmpty

  }

  private def detectFace(imagePath: String, outputImagePath: String): Unit = {
    //Enter your openCV path here
    val faceDetecter = new CascadeClassifier(getClass()
      .getResource("/lbpcascade_frontalface.xml").getPath())
    import org.opencv.core.Mat
    import org.opencv.imgcodecs.Imgcodecs
    logger.info("[DetectFace] - File Path to detect face : {}", imagePath)
    val image: Mat = Imgcodecs.imread(imagePath)

    //FeatureDetector.create(FeatureDetector.ORB)
    import org.opencv.core.MatOfRect
    val faceVectors: MatOfRect = new MatOfRect
    faceDetecter.detectMultiScale(image, faceVectors)
    val lastIndexOfSlash = imagePath.lastIndexOf("/")
    val fileName = outputImagePath + imagePath.substring(lastIndexOfSlash, imagePath.length)
    logger.info("[DetectFace] - Output File Name {}", fileName)

    logger.info("[DetectFace] - Image at path {} has {} faces in it", imagePath, faceVectors.toArray.length)
    import org.opencv.core.Scalar
    import org.opencv.imgcodecs.Imgcodecs
    import org.opencv.imgproc.Imgproc
    for (rect <- faceVectors.toArray) {
      Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0))
      val random = Random
      val randomFileName = random.alphanumeric.take(5).mkString + ".jpg"
      logger.info("[DetectFace] - Writing Processed Image File with faces at path : {}", randomFileName)
      Imgcodecs.imwrite(fileName, image)
    }
  }


}
