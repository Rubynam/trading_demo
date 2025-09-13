gcc -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin"  -shared -o libmetricscpu.so -fPIC metricscpu.c
