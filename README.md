# Pi4J Test

Prepare your Raspberry Pi:

    curl -s "https://raw.githubusercontent.com/eitch/pi4j-test/develop/src/assembly/setup.sh" | bash

Simple testing of pi4j libraries, clone this repository:

    git clone https://github.com/eitch/pi4j-test.git

Build and deploy to a running Raspberry Pi:

    mvn -o clean package -Prelease && rsync -av target/Pi4jTest.jar target/libs 192.168.1.185:src/Pi4jTest/

