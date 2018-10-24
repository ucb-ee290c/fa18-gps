package gps
import java.io._

object BinaryTester extends App {
  var inFile = None: Option[FileInputStream]
  try {
    inFile = Some(new FileInputStream("/home/nwerblun/ee290c/fa18-gps/src/main/scala/gps/data.bin"))
    var c = 0
    while ({c = inFile.get.read; c != -1}) {
      println(c)
    }
  } catch {
    case e: IOException => e.printStackTrace
  } finally {
    if (inFile.isDefined) inFile.get.close
  }

}
