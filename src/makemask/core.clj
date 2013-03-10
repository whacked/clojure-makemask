;; paint interface code translated from
;; http://forum.codecall.net/topic/58137-java-mini-paint-program/
;; Code by NomNom (24 Aug 2010)
;; modified by Bruceoutdoors(2 Aug 2012)

(ns makemask.core
  (:import (
            javax.swing JPanel JComponent JFrame JButton JOptionPane
                        JLabel ImageIcon JTextField
            )
           (java.awt Color Dimension Graphics2D BorderLayout)
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
      ipath "/tmp/000_1_1.png"
      opath "/tmp/test.png"
      
      ifile (File. ipath)
      ibuff (ImageIO/read ifile)

      mcoord (ref {:x0 nil :y0 nil})

      image (ref nil)
      g2d (ref nil)

      gpath (GeneralPath.)
      
      text (JTextField.)
      pad (let [comp (proxy [JComponent] []
                       (paintComponent [g]
                         ;; TODO
                         ;; the @image object is unused througout
                         ;; change it to something more intelligent
                         (if (nil? @image)
                           (dosync
                            (ref-set image (.createImage this
                                                         (.width (.getSize this))
                                                         (.height (.getSize this))
                                                         ))
                            ;; getGraphics is old
                            ;; (ref-set g2d (cast Graphics2D (.getGraphics @image)))
                            ;; createGraphics returns Graphics2D
                            (ref-set g2d (.createGraphics ibuff))
                            
                            )
                           )
                         ;; (.drawImage g @image 0 0 nil)
                         (.drawImage g ibuff 0 0 nil)
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
                                           (when-not (nil? @g2d)
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

      clearPad (fn []
                 (when-not (nil? @g2d)
                   (.reset gpath)
                   (doto @g2d
                     (.setPaint Color/WHITE);
                     (.fillRect 0 0 (.width (.getSize pad)) (.height (.getSize pad)))
                     (.setPaint Color/BLACK);
                     )
                   (.repaint pad)
                   )
                 )
      
      clearButton (doto (JButton. "Clear")
                    (.addActionListener
                     (reify ActionListener
                       (actionPerformed [this e]
                         (clearPad)
                         ))))
      
      saveButton (doto (JButton. "Save")
                   (.addActionListener
                    (reify ActionListener
                      (actionPerformed [this e]

                        (let [
                              ioW (.getWidth ibuff)
                              ioH (.getHeight ibuff)
                              iout (BufferedImage. ioW ioH BufferedImage/TYPE_INT_RGB)

                              g (.createGraphics iout)
                              ]
                          (doto g
                            (.setColor Color/BLACK)
                            (.fillRect 0 0 ioW ioH)
                            (.setColor Color/WHITE)
                            (.fill gpath)
                            )
                          (ImageIO/write
                           iout
                           "PNG" (File. opath))
                          )

                        ))))
      paintwindow (let [
                        paintwindow (JFrame.)
                        panel (let [pn (JPanel.)
                                    makeColorButton #(let [tempButton (JButton.)
                                                           ]
                                                       (doto tempButton
                                                         (.setBackground %)
                                                         (.setPreferredSize (Dimension. 16 16))
                                                         (.addActionListener (reify ActionListener
                                                                               (actionPerformed [this e]
                                                                                 (when-not (nil? @g2d)
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
                                  (.add clearButton)
                                  (.add saveButton)
                                  ))
                        
                        ]
                    (doto (.getContentPane paintwindow)
                      (.setLayout (BorderLayout.))
                      (.add text BorderLayout/NORTH)
                      (.add panel BorderLayout/WEST)
                      (.add pad BorderLayout/CENTER)
                      )
                    paintwindow
                    )

      ]
  (doto paintwindow
    (.setTitle "mask maker")
    (.setVisible true)
    )
  )


