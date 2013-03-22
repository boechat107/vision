(ns vision.native-core
  (:use
    [seesaw.core :as w]
    )
  (:import 
    [org.opencv.core Mat MatOfByte MatOfInt MatOfFloat CvType Scalar] 
    [org.opencv.imgproc Imgproc]
    [org.opencv.highgui Highgui]
    )
  )

(clojure.lang.RT/loadLibrary "opencv_java")

(defn mat-to-coll
  "Converts a Mat object to a clojure collection."
  [mat]
  (when (.isContinuous mat)
    (let [get-pixel (fn [r c] 
                      (if (> (.channels mat) 1)
                        (vec (.get mat r c))
                        (aget (.get mat r c) 0)))]
      (if (> (.cols mat) 1)
        ;; mat is really a matrix.
        (map (fn [row] 
               (map #(get-pixel row %) (.cols mat)))
             (range (.rows mat)))
        ;; mat is a column vector.
        (map #(get-pixel % 0) (range (.rows mat)))))))

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

(defn histogram
  "Calculates the histogram of an image using the opencv method calcHist. It shows
  the number of pixels of a specific color level or intensity.
  bins gives the number of levels to be considered."
  ([img bins]
   (apply-to-newmat 
     #(Imgproc/calcHist [img]
                        (MatOfInt. (int-array [0]))
                        (Mat.)
                        %
                        (MatOfInt. (int-array [bins])) 
                        (MatOfFloat. (float-array [0.0 255.0]))))))

(defn concentration-histogram
  "Counts the number of non-white pixels on the columns or rows of an image. The image 
  is expected to be in grayscale or black-white.
  dim values:
      :row    The pixels of each row are grouped. 
      :col    Pixels of each column are grouped."
  [img th-val dim]
  (letfn [(is-white [rc]
            (-> (.get img (:row rc) (:col rc))
                (aget 0)
                (> th-val))) 
          (inc-1 [val rc]
            (if (is-white rc)
              (if val val 0)
              (if val (inc val) 1)))] 
    (reduce #(update-in %1 [(dim %2)] inc-1 %2) 
      []
      (for [r (range (.rows img))
            c (range (.cols img))]
        {:row r, :col c})
      ))
  )

;; Todo: macro to thread an image through functions and visualize each partial
;; resulting image.

;; todo: type hints, pre and pro validation 
