# OpenCV_IRIS
repo to code opencv iris libraary examples

Requirements :

1. Java8
2. Spark standalone setup from spark home page
3. sbt (Scala Build Tool)

Install OpenCV on System using following instructions : 

https://opencv-java-tutorials.readthedocs.io/en/latest/01-installing-opencv-for-java.html#install-opencv-3-x-under-macos

Make Sure you are installing correct version of OpenCV

Once OpenCV is installed as per given instructions above :

1. Set OpenCV path in your local IDE (Eclipse or IntelliJ IDEA) in VM Arguments while running code
e.g. -Djava.library.path=/Users/techops/Documents/opencv_java320 #This is path to your opencv package. You can get this path information after installation.

2. Refresh sbt project

3. Run Spark Batch job by running main app from IDE.
