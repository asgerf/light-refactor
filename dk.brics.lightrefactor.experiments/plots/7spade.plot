#!/bin/bash
set -e

CMD="echo"
if [[ "$1" = "open" ]]
then
    CMD="xdg-open"
fi

./benchquestions ../output/namestats.txt -b 7spade >tmp/7spade.dat

gnuplot <<\EOF 

set size 0.5,0.9
set term pdf
set output "output/7spade.pdf"
#set t wxt persist
set auto x
set yrange [0:60]
set border 3
set style data histogram
set style histogram rowstacked

set style fill pattern border -1
set boxwidth 0.75
set format y "%g"
set ytics 10,10,50
set ytics add (58)
set xtics nomirror
#set key inside
set key at 9.75,55


titles = " x search-replace rename neither "

f(x) = 58

plot 'tmp/7spade.dat' using (0):xticlabels(1) title 'rename' fill pattern 2 linecolor rgb "#00FF00", \
     ''               using (0):xticlabels(1) title 'search-replace' fill solid linecolor rgb "#8888FF", \
     ''               using 2:xticlabels(1) notitle fill solid linecolor rgb "#8888FF", \
     ''               using ($3-$2):xticlabels(1) notitle fill pattern 2 linecolor rgb "#00FF00", \
     f(x) notitle linecolor rgb "#777777" with lines 

EOF

"$CMD" "output/7spade.pdf"
