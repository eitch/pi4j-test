package ch.eitchnet.pi4j.leds;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfig;
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder;
import com.pi4j.io.gpio.digital.DigitalState;

public class Lamp {

	private final DigitalOutput red;
	private final DigitalOutput yellow;
	private final DigitalOutput green;

	public Lamp(Context pi4j, int pinRed, int pinYellow, int pinGreen) {
		DigitalOutputConfigBuilder builder = DigitalOutputConfig.newBuilder(pi4j).shutdown(DigitalState.LOW);

		red = pi4j.dout().create(builder.address(pinRed));
		yellow = pi4j.dout().create(builder.address(pinYellow));
		green = pi4j.dout().create(builder.address(pinGreen));
	}

	public void redOn() {
		this.red.on();
	}

	public void redOff() {
		this.red.off();
	}

	public void yellowOn() {
		this.yellow.on();
	}

	public void yellowOff() {
		this.yellow.off();
	}

	public void greenOn() {
		this.green.on();
	}

	public void greenOff() {
		this.green.off();
	}
}
