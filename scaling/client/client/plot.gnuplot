set title 'Client Workload 100 iterations'
set datafile separator ','
set ylabel "Time (ms)"
set xlabel 'Iteration' # label for the X axis
set terminal png size 600,500
set output 'plot.png'
plot 'result.data' with lines
