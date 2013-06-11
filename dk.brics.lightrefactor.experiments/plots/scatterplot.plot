#!/bin/bash
set -e

/usr/bin/env gnuplot <<\EOF 

f(x) = x

set terminal pdf
set output "output/scatterplot.pdf"

plot [0:100] [0:100] "../output/namestats.txt" using 3:4 notitle linecolor rgb "#000077" with points, \
     f(x) notitle linecolor rgb "#777777" with lines 

EOF

echo "output/scatterplot.pdf"
