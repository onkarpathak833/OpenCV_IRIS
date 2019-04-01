import java.awt.image.BufferedImage
import java.io.File
import java.net.URI

import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
import java.nio.file.{Path, Paths}
import java.util

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import javax.imageio.ImageIO
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs
import org.apache.hadoop.fs.FileSystem
import org.apache.spark.ml.image.ImageSchema
import org.apache.spark.ml.image.ImageSchema._
import org.apache.spark.rdd.RDD
import org.opencv.core.{Mat, _}
import org.opencv.features2d.{BOWImgDescriptorExtractor, DescriptorMatcher}
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.{CascadeClassifier, Objdetect}

import scala.io.{BufferedSource, Source}
import scala.util.Random


object IrisApp extends Serializable with LazyLogging {
  lazy val irisDatasetBuilder = new IRISDatasetBuilder
  lazy val queryImageDatasetBuilder = new QueryImageDatasetBuilder
  lazy val ImageComparator = new ImageComparator
  var applicationPath: String = ""

  //  {
  //    System.setProperty("java.library.path","/Users/techops/Documents/FreelanceProjects/Applications/opencv-3.2.0/opencv_java320")
  //  }

  def main(args: Array[String]): Unit = {
    val pupilDetector = new IrisPupilDetect
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
    val pupilDetectionPath = applicationConfigs.getString("pupil_detect_path")
    val inputLocalPath = applicationConfigs.getString("local_input_path")
    val outputHdfsPath = "/OutputImages"
    var faceCascade: String = applicationConfigs.getString("face_cascade_xml_hdfs")
    var eyeCascade: String = applicationConfigs.getString("eye_cascade_xml_hdfs")
    val faceCascadeLocal = applicationConfigs.getString("face_cascade_xml_local")
    val eyeCascadeLocal = applicationConfigs.getString("eye_cascade_xml_local")
    val images: DataFrame = readImages(irisDatasets)


    System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // load opencv_java
    //System.load(openCVJavaLibs)

    //val irisDataset = irisDatasetBuilder.buildIRISDataset(irisDatasets)
    //val queryImage = queryImageDatasetBuilder.buildDescriptorForQueryImage(queryImagePath, openCVJavaLibs)

    val irisFiles = new util.ArrayList[java.lang.String]()

    implicit val encoder = org.apache.spark.sql.Encoders.kryo[String]

    import scala.collection.JavaConversions._
    val data: util.List[String] = images.map(imageRow => {
      val hadoopConf = new Configuration()
      val hdfs = FileSystem.get(hadoopConf)
      val eyeCascadePath = new fs.Path(eyeCascade)
      val faceCascadePath = new fs.Path((faceCascade))
      val tempCascadePath = new fs.Path("/tmp")
      if (hdfs.exists(eyeCascadePath)) {
        logger.info("[IrisApp] - Eye cascade exists!")
        hdfs.copyToLocalFile(false, eyeCascadePath, tempCascadePath, true)
        eyeCascade = "/tmp/haarcascade_lefteye_2splits.xml"
      }
      else {
        logger.info("[IrisApp] - Eye cascade does not exists. Using local cascade xml ")
        eyeCascade = eyeCascadeLocal
      }
      if (hdfs.exists(faceCascadePath)) {
        logger.info("[IrisApp] - Face cascade exists.")
        hdfs.copyToLocalFile(false, faceCascadePath, tempCascadePath, true)
        faceCascade = "/tmp/haarcascade_frontalface_alt.xml"
      }
      else {
        logger.info("[IrisApp] -  Face Cascade does not exist. Using Local cascade xml")
        faceCascade = faceCascadeLocal
      }
      val row: Row = imageRow.getAs[Row](0)
      //      val eyeCascadeStatus = hdfs.getFileStatus(new fs.Path(faceCascade))
      //       val path =  Paths.get(getOrigin(row)).toAbsolutePath.toString
      val path: String = ImageSchema.getOrigin(imageRow.getAs[Row](0)).toString
      logger.info("[IrisApp] hdfs/file image path : {}", path)

      var splitString: String = "randomString"
      var isHdfsFile = false
      var filename = ""
      var imagePath = ""

      if (path.contains("file:")) {
        splitString = "file:"
        val absPath: Array[String] = path.split(splitString)
        logger.info("Absolute HDFS Image Path : {}", absPath)
        filename = absPath(1)
        imagePath = filename
      } else if (path.contains("hdfs:")) {
        isHdfsFile = true
        filename = path
        val sourceInputPath = new org.apache.hadoop.fs.Path(filename)
        val lastIndexOfSlash = filename.lastIndexOf("/")
        val fileName = filename.substring(lastIndexOfSlash, filename.length)
        imagePath = inputLocalPath + fileName
        val destInputPath = new org.apache.hadoop.fs.Path(imagePath)
        logger.info("[IrisApp] - copying file to hadoop local... from {} to {}", filename, imagePath)
        hdfs.copyToLocalFile(sourceInputPath, destInputPath)
        logger.info("FILENAME : {}", imagePath)
      }

      logger.info("[IrsiApp] - Adding Iris File information - {}", imagePath)

      irisFiles.add(imagePath)

      logger.info("[IrisApp] - IRIS Data files size - {}",irisFiles.size())

      logger.info("[IrisApp] - Detecting faces in the image {} {} {}", imagePath, outputImagePath, faceCascade)
      val eyeImageInputPath = detectFace(imagePath, outputImagePath, faceCascade)
      logger.info("[IrisApp] - Detecting eyes in the same image")
      val eyePupilInputPath = detectEye(eyeImageInputPath, outputImagePath, eyeCascade)
      logger.info("[DetectEye] - with {} {} {}", eyePupilInputPath, pupilDetectionPath)
      val localEyePath = IrisPupilDetect.detectEyePupil(eyePupilInputPath, pupilDetectionPath, openCVJavaLibs)

      if (isHdfsFile) {
        val lastIndex = localEyePath.lastIndexOf("/")
        val outputImageName = localEyePath.substring(lastIndex, localEyePath.length)
        val hdfsPath = outputHdfsPath + outputImageName
        logger.info("[IrisApp] - Writing result to hdfs from : {}  to : {}  ", localEyePath, hdfsPath)
        hdfs.copyFromLocalFile(false, true, new org.apache.hadoop.fs.Path(localEyePath), new org.apache.hadoop.fs.Path(hdfsPath))
      }

      eyeCascade = applicationConfigs.getString("eye_cascade_xml_hdfs")
      faceCascade = applicationConfigs.getString("face_cascade_xml_hdfs")

      imagePath
      //      val comparison = ImageComparator.compareFeature(filename, queryImagePath)
      //      println(comparison)
      //      comparison
    }).collectAsList()

    logger.info("[QueryImage] - Checking if Image Exists...")

    import scala.collection.JavaConversions._
    import scala.collection.JavaConverters._


    val irisPaths =  data.asScala.toList
    println(irisPaths)
    checkIfImageExists(irisPaths, queryImagePath)


    //    val data = checkIfImageExists(irisDataset,queryImage)
    //    println(data)


    //        val ifExist = data.collect().toList.nonEmpty
    //
    //        if (ifExist) {
    //          println(s"[IrisApp] - Image at Path ${queryImagePath} EXISTS in IRIS Datasets ")
    //        }
    //        else {
    //          println(s"[IrisApp] - Image at Path ${queryImagePath} DOES NOT EXIST in IRIS Datasets")
    //        }
  }

  private def checkIfImagesAreEqual(file1: String, file2: String): Boolean = {
    ImageComparator.compareFeature(file1, file2)
  }

  private def checkIfImageExists(irisDataset: List[String], queryImage: String) = {

    val hadoopConf = new Configuration()
    val hdfs = FileSystem.get(hadoopConf)
    var retVal = 0
    val startTime = System.currentTimeMillis
//    System.load(Core.NATIVE_LIBRARY_NAME)
    //System.load("/Users/techops/Documents/FreelanceProjects/Applications/opencv-3.2.0/opencv_java320/opencv-320.jar")
    //System.load("/Users/techops/Documents/FreelanceProjects/Applications/opencv-3.2.0/opencv_java320/libopencv_java320.dylib")
    System.setProperty("java.library.path","/Users/techops/Documents/FreelanceProjects/Applications/opencv-3.2.0/opencv_java320")

    val queryImageHdfsPath = new fs.Path(queryImage)
    var qImagePath = ""
    if (hdfs.exists(queryImageHdfsPath)) {
      logger.info("[ImageExists] - Image exists on hdfs. copying to hadoop local")
      val lastIndexOfSlash = queryImage.lastIndexOf("/")
      val fileName = queryImage.substring(lastIndexOfSlash, queryImage.length)
      val destQueryImagePath = new fs.Path("/tmp" + fileName)
      hdfs.copyToLocalFile(false, queryImageHdfsPath, destQueryImagePath, true)
      logger.info("[ImageExists] - copied query image to hadoop local")
      qImagePath = "/tmp" + fileName
    }
    else {
      logger.info("[ImageExist] -  Image does not exist on hdfs. Assuming its on local Filesystem.")
      qImagePath = queryImage
    }

    val images = irisDataset.filter(path => {
      logger.info("[ImageExists] - Dataset Image Path {}",path)
      val datasetImage = Imgcodecs.imread(path)

      val qImageMat = Imgcodecs.imread(qImagePath)
      val isEqual = ImageComparator.compareFeature(path, qImagePath)
      //val isEqual = ImageComparator.ifImagesEqual(datasetImage, qImageMat)
      isEqual

    })

    logger.info("[ImageExist] - Query Image Exists ?  -- {}", images.nonEmpty)
    println("[ImageExist] - Query Image Exist in IRIS Dataset : "+images.nonEmpty)
    images.nonEmpty

  }


  def detectEye(imagePath: String, outputPath: String, eyeCascadeXmlPath: String): String = {

    //val eyeDetecter = new CascadeClassifier(new URI("/Users/techops/Documents/FreelanceProjects/SparkKafka_HDInsights/OpenCV_IRIS/src/main/resources/haarcascade_lefteye_2splits.xml").getPath)

    val eyeDetecter = new CascadeClassifier(new URI(eyeCascadeXmlPath).getPath)
    //    val eyeDetecter = new CascadeClassifier(this.getClass.getResource("/haarcascade_lefteye_2splits.xml").getPath)
    var image: Mat = Imgcodecs.imread(imagePath)

    import org.opencv.core.MatOfRect
    val eyes = new MatOfRect
    eyeDetecter.detectMultiScale(image, eyes)

    import org.opencv.core.Scalar
    import org.opencv.imgproc.Imgproc
    val lastIndexOfSlash = imagePath.lastIndexOf("/")
    val imageName = imagePath.substring(lastIndexOfSlash, imagePath.length)
    val fileName = outputPath + imageName
    println(fileName)
    val eyesImagePath = outputPath + imageName.split("\\.")(0) + "_eye.jpeg"
    import scala.collection.JavaConversions._
    var rectImg = new Rect()
    for (rect <- eyes.toArray) { //Sol üst kö?esine metin yaz
      Imgproc.putText(image, "Eye", new Point(rect.x, rect.y - 5), 1, 2, new Scalar(0, 0, 255))
      //Kare im
      Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(200, 200, 100), 2)

      rectImg = new Rect(rect.x, rect.y, rect.width, rect.height)
      Imgcodecs.imwrite(fileName, image)
      val eyesImage = new Mat(image, rectImg)
      Imgcodecs.imwrite(eyesImagePath, eyesImage)

    }

    val eyeFile = new File(eyesImagePath)
    if (!eyeFile.exists()) {
      val defaultImage = Imgcodecs.imread(imagePath)
      Imgcodecs.imwrite(eyesImagePath, defaultImage)
    }

    eyesImagePath
  }


  private def detectFace(imagePath: String, outputImagePath: String, faceCascadeXmlPath: String): String = {
    //Enter your openCV path here

    //    val faceDetecter = new CascadeClassifier(new URI("/Users/techops/Documents/FreelanceProjects/SparkKafka_HDInsights/OpenCV_IRIS/src/main/resources/lbpcascade_frontalface.xml").getPath)

    val faceDetecter = new CascadeClassifier(new URI(faceCascadeXmlPath).getPath)
    //      val faceDetecter = new CascadeClassifier(this.getClass()
    //      .getResource("/lbpcascade_frontalface.xml").getPath())

    logger.info("[DetectFace] - faceDectector Status : {}", faceDetecter.empty())
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
    val imageFile = new File(fileName)
    if (!imageFile.exists()) {
      val defaultImage = Imgcodecs.imread(imagePath)
      Imgcodecs.imwrite(fileName, defaultImage)
    }

    logger.info("[DeetctFace] - detected face image written at : {}", fileName)

    fileName
  }


}
