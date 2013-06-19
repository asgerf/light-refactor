#!/bin/bash
set -e

if [[ "$#" = 0 ]]
then
    echo "Usage: `basename $0` BENCH [open]" >&2
    exit 1
fi

BENCH="$1"
shift    

CMD="echo"
if [[ "$1" = "open" ]]
then
    shift
    CMD="xdg-open"
fi

./benchquestions data/namestats.orig.txt ../output/namestats.txt -b $BENCH $* >tmp/benchquestions.dat

gnuplot <<\EOF 

#set size 0.5,0.9
set term pdf
set output "output/benchquestions.pdf"
#set t wxt persist
set auto x
#set yrange auto
set border 3
set style data histogram
set style histogram rowstacked

set style fill pattern border -1
set boxwidth 0.75
set format y "%g"
#set ytics 50,50,211
set xtics nomirror
#set ytics add (211)
set key outside top right horiz

#set nokey

titles = " x search-replace rename neither "

f(x) = 211

plot 'tmp/benchquestions.dat' using 2:xticlabels(1) title 'search-replace' fill solid linecolor rgb "#8888FF", \
     ''                       using ($3-$2):xticlabels(1) title 'rename' fill pattern 2 linecolor rgb "#00FF00", \
     ''                       using ($4-$3):xticlabels(1) title 'new' fill pattern 2 linecolor rgb "#FF0000"

EOF

"$CMD" "output/benchquestions.pdf"
