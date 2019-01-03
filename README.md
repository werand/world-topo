# world-topo

Usage example of d3 with clojurescript

## Overview

Usage of d3 with clojurescript, inspired from:
https://lambdaisland.com/blog/26-04-2018-d3-clojurescript
and this d3 Example http://bl.ocks.org/dwtkns/4973620 (i transferred
this code to clojurescript).

This project was mainly an exercise with clojurescript for me. All the credit goes to
the respective authors.

## Setup

To get an interactive development environment run:
   
    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL. 

## License

Copyright © 2019 Andreas Werner

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
