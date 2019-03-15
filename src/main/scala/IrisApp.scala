import java.awt.image.BufferedImage

import org.apache.spark.sql.{Row, SparkSession}
import java.nio.file.{Path, Paths}

import javax.imageio.ImageIO
import org.apache.spark.ml.image.ImageSchema
import org.apache.spark.ml.image.ImageSchema._
import org.opencv.core.{Core, Point}
import org.opencv.objdetect.CascadeClassifier


object IrisApp {

  def main(args: Array[String]): Unit = {
    val sparkSession = SparkSession.builder().appName("Consumer").master("local[*]").getOrCreate()
    val images = readImages("/Users/techops/Documents/FreelanceProjects/SampleImages")
    images.foreach(rrow => {
      val row: Row = rrow.getAs[Row](0)
      val filename = Paths.get(getOrigin(row)).toAbsolutePath.toString.split("file:")(1)
      println(filename)
      val imageData: Array[Byte] = getData(row)
      val height: Int = getHeight(row)
      val width: Int = getWidth(row)

      //      val image: BufferedImage = ImageIO.read(filename.toFile)
      println(s" Image Path : ${filename} Image Size is : ${height}x${width}")
      detectFace(filename)
    })

  }

  private def detectFace(imagePath1: String): Unit = {


    System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // load opencv_java

  val imagePath = "/Users/techops/Documents/My Documents/AmeyaPhoto.jpg"
    //Enter your openCV path here
    val faceDetecter = new CascadeClassifier(getClass()
      .getResource("/lbpcascade_frontalface.xml").getPath())
    import org.opencv.core.Mat
    import org.opencv.imgcodecs.Imgcodecs
    println(s"file path is : ${imagePath}")
    val image = Imgcodecs.imread(getClass().getResource(
      "/AmeyaPhoto.jpg").getPath())
    import org.opencv.core.MatOfRect
    val faceVectors = new MatOfRect
    faceDetecter.detectMultiScale(image, faceVectors)


    println(s"For image at path : ${imagePath}  " + faceVectors.toArray.length + " faces found in image")
    import org.opencv.core.Scalar
    import org.opencv.imgcodecs.Imgcodecs
    import org.opencv.imgproc.Imgproc
    import scala.collection.JavaConversions._
    for (rect <- faceVectors.toArray) {
      Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0))
    }
    //Imgcodecs.imwrite("SampleData_1.png", image)
  }

}
