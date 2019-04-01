import java.io.File

import IrisApp.applicationPath
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.ml.image.ImageSchema.readImages
import org.apache.spark.sql.DataFrame
import org.opencv.core.Core

object IrisEyeDetector extends App {

  var applicationPath: String = ""
  applicationPath = args(0)
  val applicationConfigs: Config = ConfigFactory.parseFile(new File(applicationPath)).withFallback(ConfigFactory.load("application.conf")).resolve()
//  val irisDatasets = applicationConfigs.getString("sample_images_path")
//  val queryImagePath = applicationConfigs.getString("query_images_path")
  val openCVJavaLibs = applicationConfigs.getString("opencv_java_dylib")

//  val images: DataFrame = readImages(irisDatasets)
  //System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // load opencv_java
  System.load(openCVJavaLibs)

  val imagePath = "/Users/techops/Documents/FreelanceProjects/eye79.jpeg"
  val outputImagePath  = "/Users/techops/Documents/FreelanceProjects/OutputImages"
  val openCVDll = "/Users/techops/Documents/FreelanceProjects/Applications/opencv-3.2.0/build/lib/libopencv_java320.dylib"
  val eyeCascadeXml = "/Users/techops/Documents/FreelanceProjects/SparkKafka_HDInsights/OpenCV_IRIS/src/main/resources/haarcascade_lefteye_2splits.xml"
  IrisApp.detectEye(imagePath,outputImagePath, eyeCascadeXml)

}
