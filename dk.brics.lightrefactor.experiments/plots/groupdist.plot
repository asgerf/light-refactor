#!/bin/sh
set -e

./groupdist ../output/groups.txt >tmp/groupdist.dat

gnuplot <<\EOF 

set term pdf
set output "output/groupdist.pdf"
#set t wxt persist
set auto x
set yrange [0:100]
set border 3
set style data histogram
set style histogram rowstacked

set xtic rotate by -50 scale 0

set style fill pattern border -1
set boxwidth 0.75
set format y "%g %%"
set ytics 10,10,100
#set key horiz
#set key right top
set key outside

#set key at 35, -15

titles = " x search-replace rename neither "


plot 'tmp/groupdist.dat' using ($2*100):xticlabels(1) title '1'    fill solid linecolor rgb "#8888FF", \
     ''                  using ($3*100):xticlabels(1) title '2'    fill pattern 1 linecolor rgb "#88FF88", \
     ''                  using ($4*100):xticlabels(1) title '3..9' fill pattern 6 linecolor rgb "#8888FF", \
     ''                  using ($5*100):xticlabels(1) title '10+'  fill pattern 2 linecolor rgb "#FF2200"

EOF

echo "output/groupdist.pdf"