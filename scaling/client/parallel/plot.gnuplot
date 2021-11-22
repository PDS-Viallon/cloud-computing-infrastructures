set title 'Parallel Workload 10 clients (100 iteration)'
set datafile separator ','
set ylabel "Time (ms)"
set xlabel 'Iteration' # label for the X axis
set terminal png size 600,500
set output 'plot.png'
plot 'result.data' with lines
