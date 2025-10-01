# Pi4J Test

Prepare your Raspberry Pi:

    curl -s "https://raw.githubusercontent.com/eitch/pi4j-test/develop/src/assembly/setup.sh" | bash

Simple testing of pi4j libraries, clone this repository:

    git clone https://github.com/eitch/pi4j-test.git

Build and deploy to a running Raspberry Pi:

    mvn clean package -Prelease && rsync -av target/Pi4jTest.jar target/libs gsiadmin@rpi3-dinrail:src/Pi4jTest/

