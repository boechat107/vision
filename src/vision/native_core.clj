(ns vision.native-core
  (:use
    [seesaw.core :as w]
    )
  (:import 
    [org.opencv.core Mat MatOfByte CvType Scalar] 
    [org.opencv.imgproc Imgproc]
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
                     :original -1
                     ;; Default value.
                     1))))

(defn encode-image
  "Encodes an image into a specific format."
  ([img ext] (encode-image img ext nil))
  ([img ext param]
   (let [encod-img (MatOfByte.)]
     (Highgui/imencode ext img encod-img)
     encod-img)))

(defn to-bufferedimage
  [img]
  (->> (encode-image img ".jpeg")
       (.toArray)
       (java.io.ByteArrayInputStream.)
       (javax.imageio.ImageIO/read)))

(defn view 
  "Shows the images on a grid-panel window."
  [& imgs]
  (let [buff-imgs (map #(if (instance? java.awt.image.BufferedImage %)
                          %
                          (to-bufferedimage %))
                       imgs)
        grid (w/grid-panel
               :border 5
               :hgap 10 :vgap 10
               :columns (min 6 (max 1 (count imgs))) 
               :items (map #(w/label :icon %) buff-imgs))]
    (-> (w/frame :title "Image Viewer" 
                 :content grid)
        w/pack!
        w/show!)))

(defn- apply-to-newmat
  "Creates a new Mat object, applies the given function on it and returns the object."
  [f]
  (doto (Mat.) f))

(defn convert-color
  "Converts an image from one color space to another and returns the converted image.
  Possible values of key-type:
      :bgr->gray
"
  [img key-type]
  (let [res (Mat.)
        apply-color (fn [k] (Imgproc/cvtColor img res k))]
    (apply-color (condp = key-type
                   :bgr->gray Imgproc/COLOR_BGR2GRAY))
    res))

(defn threshold
  "Returns the image resulting of the applying a fixed level threshold to the given
  image.
  Possible types of threshold:
      :bin, :bin-inv, :trucate, :zero, :zero-inv"
  ([img th-val] (threshold img th-val 255 :bin))
  ([img th-val max-val type]
   (apply-to-newmat 
     #(Imgproc/threshold img % th-val max-val
                         (condp = type 
                           :bin Imgproc/THRESH_BINARY
                           :bin-inv Imgproc/THRESH_BINARY_INV
                           :truncate Imgproc/THRESH_TRUNC
                           :zero Imgproc/THRESH_TOZERO
                           :zero-inv Imgproc/THRESH_TOZERO_INV)))))

;; Todo: macro to thread an image through functions and visualize each partial
;; resulting image.
