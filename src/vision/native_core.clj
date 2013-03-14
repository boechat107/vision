(ns vision.native-core
  (:use
    [seesaw core :as w]
    )
  (:import 
    [org.opencv.core Mat MatOfByte CvType Scalar] 
    [org.opencv.highgui Highgui]
    )
  )

(clojure.lang.RT/loadLibrary "opencv_java")

(defn view 
  [buff-img & more]
  (let [grid (w/grid-panel
               :border 5
               :hgap 10 :vgap 10
               :columns (min 6 (max 1 (count more))) 
               :items (map #(w/label :icon %) (conj more buff-img)))]
    (-> (w/frame :title "Image Viewer" 
                 :content grid)
        w/pack!
        w/show!))) 

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

(defn encode-image
  ([img ext] (encode-image img ext nil))
  ([img ext param]
   (let [encod-img (MatOfByte.)]
     (Highgui/imencode ext img encod-img)
     encod-img)))

(defn to-buffedimage
  [img]
  (->> (MatOfByte. img)
       (.toArray)
       (java.io.ByteArrayInputStream.)
       (javax.imageio.ImageIO/read)
       )
  )
