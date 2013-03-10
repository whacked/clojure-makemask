clojure-makemask
================

A simple interface for hand-drawing image masks, because I couldn't find a plain old mask drawer, and don't want to fire up a whole image processing suite for it

for use in image processing, algorithm training or whatnot

Also a paint program example using clojure, a shoddy port from Java that foregoes classes by abusing scope

## Usage

specify an input image path (now, hard-coded in core.clj), an output image path

run the program, draw the area you want to mask, and click save

## TODO

loop through a directory or list of images and auto-load a new image once you save

## License

My clojure parts is public domain, but see `core.clj` regarding original credit for paint interface
