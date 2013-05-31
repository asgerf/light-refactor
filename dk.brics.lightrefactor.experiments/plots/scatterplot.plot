#set xlabel "Search-replace questions"
#set ylabel "Rename tool questions"

f(x) = x

set terminal pdf
set output "scatterplot.pdf"

plot [0:100] [0:100] "../output/namestats.txt" using 3:4 notitle linecolor rgb "#000077" with points, \
     f(x) notitle linecolor rgb "#777777" with lines 
