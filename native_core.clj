(ns vision.native-core
  (:import 
    [org.opencv.core Mat CvType Scalar] 
    [org.opencv.highgui Highgui]
    )
  )

(clojure.lang.RT/loadLibrary "opencv_java")

(defn load-image
  "Loads an image from a file and returns a Mat object.
  The color of the image can be specified by the values :color, :grayscale,
  :any-depth or :original."
  ([file] (load-image file :color))
  ([file color]
   (Highgui/imread file 
                   (condp = color
                     :color Highgui/CV_LOAD_IMAGE_COLOR
                     :grayscale Highgui/CV_LOAD_IMAGE_GRAYSCALE
                     :any-depth Highgui/CV_LOAD_IMAGE_ANYDEPTH
                     ;; Returns the loaded image as is (with alpha channel).
                     :original -1))))
