# lettuce-threads
Measuring impact of threading and pooling on communications with Redis

Made as an answer to https://stackoverflow.com/questions/49409344/redis-is-single-thread-then-why-should-i-use-lettuce

Usage:  
mvn clean inastall  
java -jar target/LettuceThreadsBenchmark-1.0-SNAPSHOT.jar -i 5 -wi 2 -f 5 -t 4 -r 5 -w 10 -o ~/LettuceThreads4.log

Here -t is the number of threads to test on. Interesting thing is that because JMH adds its overhead, CPU thread won't be used only to work with Redis. It is one of the reasons why it will scale past your available hardware threads. The other is that data transfer may be orders of magnitude slower than performing a command.
