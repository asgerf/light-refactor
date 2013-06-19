#!/bin/bash
set -e

CMD="echo"
if [[ "$1" = "open" ]]
then
    CMD="xdg-open"
fi

./benchquestions ../output/namestats.txt -b jslinux >tmp/jslinux.dat

gnuplot <<\EOF 

set size 0.5,0.9
set term pdf
set output "output/jslinux.pdf"
#set t wxt persist
set auto x
set yrange [0:225]
set border 3
set style data histogram
set style histogram rowstacked

set style fill pattern border -1
set boxwidth 0.75
set format y "%g"
set ytics 50,50,211
set xtics nomirror
set ytics add (211)
#set key outside top right horiz

set nokey

titles = " x search-replace rename neither "

f(x) = 211

plot 'tmp/jslinux.dat' using 2:xticlabels(1) title 'search-replace' fill solid linecolor rgb "#8888FF", \
     ''                using ($3-$2):xticlabels(1) title 'rename' fill pattern 2 linecolor rgb "#00FF00", \
     f(x) notitle linecolor rgb "#777777" with lines 

EOF

"$CMD" "output/jslinux.pdf"
