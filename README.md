# Pi4J Test

Simple testing of pi4j libraries

Build and deploy to a running Raspberry Pi:

    mvn -o clean package -Prelease && rsync -av target/Pi4jTest.jar target/libs 192.168.1.185:src/Pi4jTest/

