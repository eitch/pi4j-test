#!/bin/bash -e
sudo raspi-config nonint do_i2c 0
sudo raspi-config nonint do_ssh 0
sudo raspi-config nonint do_serial_hw 0
sudo raspi-config nonint do_serial_cons 1
sudo raspi-config nonint do_onewire 0
sudo systemctl disable hciuart
echo "dtoverlay=disable-bt" | sudo tee -a /boot/firmware/config.txt

sudo apt install -y i2c-tools vim git java-common libxi6 libxrender1 libxtst6
curl -s "https://get.sdkman.io" | bash
source .bashrc
sdk install maven

mkdir -p ~/Downloads
cd ~/Downloads
wget https://cdn.azul.com/zulu/bin/zulu21.34.19-ca-jdk21.0.3-linux_arm64.deb
sudo dpkg -i zulu21.34.19-ca-jdk21.0.3-linux_arm64.deb

#mkdir -p ~/src
#cd ~/src
#git clone https://github.com/Pi4J/pi4j-example-fxgl
#cd pi4j-example-fxgl/
#mvn clean package
