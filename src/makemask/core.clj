;; paint interface code translated from
;; http://forum.codecall.net/topic/58137-java-mini-paint-program/
;; Code by NomNom (24 Aug 2010)
;; modified by Bruceoutdoors(2 Aug 2012)

(ns makemask.core
  (:import (
            javax.swing JPanel JComponent JFrame JButton JOptionPane
                        JLabel ImageIcon JTextField
            )
           (java.awt Color Dimension Graphics2D BorderLayout GridBagLayout GridBagConstraints)
           (java.awt.image BufferedImage)
           (java.awt.geom GeneralPath)
           (java.awt.event ActionListener MouseAdapter MouseMotionAdapter)
           (java.io File)
           (javax.imageio ImageIO)
           )
  ;; (:use [clojure.reflect]
  ;;       [clojure.pprint])
  
  ;; (:require [])
  )


;; http://stackoverflow.com/questions/5821892/why-should-i-use-reify-instead-of-proxy-in-clojure
;; - Only protocols or interfaces are supported, no concrete superclass.
;; - The method bodies are true methods of the resulting class, not external fns.
;; - Invocation of methods on the instance is direct, not using map lookup.
;; - No support for dynamic swapping of methods in the method map.

(let [
      ;; ipath "/tmp/000_1_1.png"
      ipath "/tmp/input"
      opath "/tmp/test.png"
      
      ifile (File. ipath)

      v-img (filter (fn [path] (some identity (map #(.endsWith (.toLowerCase (str path)) %) ["png" "jpg" "bmp"])))
                    (if (.isDirectory ifile)
                      (file-seq ifile)
                      [ifile]))
      ]

  (when v-img
    (let [
          mcoord (ref {:x0 nil :y0 nil})

          image (ref nil)
          g2d (ref nil)

          gpath (GeneralPath.)

          text-input (JTextField. ipath)
          text-output (JTextField. opath)

          nthcounter (ref 0)
          counterLabel (JLabel.)

          ibuff (ref (ImageIO/read (nth v-img @nthcounter)))

          pad (let [comp (proxy [JComponent] []
                           (paintComponent [g]
                             (when-not @g2d
                               (dosync
                                (prn "RESET")
                                ;; getGraphics is old
                                ;; (ref-set g2d (cast Graphics2D (.getGraphics @image)))
                                ;; createGraphics returns Graphics2D
                                (ref-set g2d (.createGraphics @ibuff))
                                )
                               )
                             (.drawImage g @ibuff 0 0 nil)
                             )

                           )]
                (doto comp
                  (.setDoubleBuffered false)
                  (.addMouseListener (proxy [MouseAdapter] []
                                       (mousePressed [event]
                                         (prn "clicked" (.getX event) (.getY event))
                                         (.moveTo gpath (float (.getX event)) (float (.getY event)))
                                         (dosync
                                          (alter
                                           mcoord
                                           assoc :x0 (.getX event) :y0 (.getY event))))
                                       (mouseReleased [event]
                                         (prn "mouseup")
                                         (doto @g2d
                                           (.setColor Color/YELLOW)
                                           (.fill gpath)
                                           )
                                         (.repaint comp)
                                         )
                                       ))
                  (.addMouseMotionListener (proxy [MouseMotionAdapter] []
                                             (mouseDragged [event]
                                               (when @g2d
                                                 (let [curX (.getX event)
                                                       curY (.getY event)]
                                                   (.lineTo gpath (float curX) (float curY))
                                                   (.drawLine @g2d (:x0 @mcoord) (:y0 @mcoord) curX curY)
                                                   (.repaint comp)
                                                   (dosync
                                                    (alter mcoord assoc :x0 curX :y0 curY)
                                                    ))))
                                             )))
                )

          resetPad (fn []
                     (when @g2d
                       (.reset gpath)
                       (doto @g2d
                         (.setPaint Color/WHITE) ;
                         (.fillRect 0 0 (.width (.getSize pad)) (.height (.getSize pad)))
                         (.setPaint Color/BLACK) ;
                         )
                       (dosync
                        (ref-set g2d nil))
                       (.repaint pad)
                       )
                     (dosync
                      (ref-set ibuff (ImageIO/read (File. (.getText text-input)))))
                     )
          
          resetButton (doto (JButton. "Reset")
                        (.addActionListener
                         (reify ActionListener
                           (actionPerformed [this e]
                             (resetPad)
                             ))))
          
          saveButton (doto (JButton. "Save")
                       (.addActionListener
                        (reify ActionListener
                          (actionPerformed [this e]

                            (let [
                                  ioW (.getWidth @ibuff)
                                  ioH (.getHeight @ibuff)
                                  iout (BufferedImage. ioW ioH BufferedImage/TYPE_INT_RGB)
                                  ]
                              (doto (.createGraphics iout)
                                (.setColor Color/BLACK)
                                (.fillRect 0 0 ioW ioH)
                                (.setColor Color/WHITE)
                                (.fill gpath)
                                )
                              (ImageIO/write
                               iout
                               "PNG" (File. (.getText text-output)))
                              )

                            ))))

          loadNthImage (fn [n]
                         (dosync
                          (ref-set nthcounter n)
                          ;; (ref-set ibuff (ImageIO/read (nth v-img @nthcounter)))
                          )
                         (.setText text-input (str (nth v-img @nthcounter)))
                         (.setText text-output (str (nth v-img @nthcounter) ".msk"))
                         (.setText counterLabel (str @nthcounter " of " (count v-img)))
                         (resetPad))

          prevButton (doto (JButton. "prev")
                       (.addActionListener
                        (reify ActionListener
                          (actionPerformed [this e]
                            (loadNthImage (dec (if (= @nthcounter 0) (count v-img) @nthcounter)))))))

          nextButton (doto (JButton. "next")
                       (.addActionListener
                        (reify ActionListener
                          (actionPerformed [this e]
                            (loadNthImage (if (= @nthcounter (dec (count v-img))) 0 (inc @nthcounter)))))))

          paintwindow (let [
                            paintwindow (JFrame.)
                            panel-control (let [pn (JPanel.)
                                                makeColorButton #(let [tempButton (JButton.)
                                                                       ]
                                                                   (doto tempButton
                                                                     (.setBackground %)
                                                                     (.setPreferredSize (Dimension. 16 16))
                                                                     (.addActionListener (reify ActionListener
                                                                                           (actionPerformed [this e]
                                                                                             (when @g2d
                                                                                               (.setPaint @g2d %)
                                                                                               (.repaint pad)
                                                                                               ))))
                                                                     )
                                                                   (.add pn tempButton)
                                                                   )
                                                ]
                                            (makeColorButton Color/BLUE)
                                            (makeColorButton Color/MAGENTA)
                                            (makeColorButton Color/RED)
                                            (makeColorButton Color/GREEN)
                                            (makeColorButton Color/BLACK)
                                            (doto pn
                                              (.setPreferredSize (Dimension. 80 68))
                                              (.add resetButton)
                                              (.add saveButton)
                                              (.add prevButton)
                                              (.add nextButton)
                                              (.add counterLabel)
                                              ))

                            panel-image (let [p (JPanel.)
                                              c (GridBagConstraints.)]
                                          (.setLayout p (GridBagLayout.))
                                          (set! (.fill c) GridBagConstraints/HORIZONTAL)
                                          
                                          (set! (.weightx c) 0.0)
                                          (set! (.gridx c) 0)
                                          (set! (.gridy c) 0)
                                          (.add p (JLabel. "input") c)

                                          (set! (.weightx c) 1.0)
                                          (set! (.gridx c) 1)
                                          (.add p text-input c)

                                          (set! (.weightx c) 0.0)
                                          (set! (.gridx c) 0)
                                          (set! (.gridy c) 1)
                                          (.add p (JLabel. "output") c)

                                          (set! (.weightx c) 1.0)
                                          (set! (.gridx c) 1)
                                          (.add p text-output c)

                                          (set! (.fill c) GridBagConstraints/BOTH)
                                          (set! (.weighty c) 1.0)
                                          (set! (.gridy c) 2)

                                          (.add p pad c)
                                          
                                          p
                                          )
                            
                            ]
                        (doto (.getContentPane paintwindow)
                          (.setLayout (BorderLayout.))
                          (.add panel-control BorderLayout/WEST)
                          (.add panel-image BorderLayout/CENTER)
                          )
                        paintwindow
                        )
          ]
      (doto paintwindow
        (.setTitle "mask maker")
        (.setVisible true)
        )
      
      
      ))
  )
