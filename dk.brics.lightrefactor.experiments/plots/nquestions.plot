#!/bin/bash
set -e

CMD="echo"
if [[ "$1" = "open" ]]
then
    CMD="xdg-open"
fi

./nquestions ../output/namestats.txt -n 3 >tmp/nquestions-3.dat

gnuplot <<\EOF 

set term pdf
set output "output/nquestions.pdf"
#set t wxt persist
set auto x
set yrange [0:100]
set border 3
set style data histogram
set style histogram rowstacked

set xtic rotate by -50 scale 0

set style fill pattern border -1
set boxwidth 0.75
set ytics 10,10,100
set format y "%g %%"
set format y2 "  "
set y2tics 10,10,100
set key outside
set key horiz
set key right top
#set key inside
#set nokey
set grid ytics

#set key at 35, -15

titles = " x search-replace rename neither "


plot 'tmp/nquestions-3.dat' using (0):xticlabels(1) title 'rename' fill pattern 2 linecolor rgb "#00FF00", \
     ''                     using (0):xticlabels(1) title 'search-replace' fill solid linecolor rgb "#8888FF", \
     ''                     using ($3/$2*100):xticlabels(1) notitle  fill solid linecolor rgb "#8888FF", \
     ''                     using (($4-$3)/$2*100):xticlabels(1) notitle fill pattern 2 linecolor rgb "#00FF00"

EOF

"$CMD" "output/nquestions.pdf"
