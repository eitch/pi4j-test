package ch.eitchnet.pi4j.test;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.i2c.I2C;

import java.util.concurrent.atomic.AtomicReference;

public class PCF8574Test {
	public static void main(String[] args) {

		Context pi4j = Pi4J.newAutoContext();
		I2C inputI2c = pi4j.i2c().create(1, 0x38);
		I2C outputI2c = pi4j.i2c().create(1, 0x21);
		DigitalInput interruptPin = pi4j.din().create(13);

		// write output state
		// default is all outputs off, i.e. 1
		AtomicReference<Byte> currentOutputState = new AtomicReference<>((byte) 0xff);
		outputI2c.write(currentOutputState.get());

		// read current input state
		AtomicReference<Byte> currentInputState = new AtomicReference<>((byte) inputI2c.read());

		for (int pin = 0; pin < Byte.SIZE; pin++) {
			if (isBitSet(currentInputState.get(), pin)) {
				System.out.println("Bit " + pin + " is set!");
			} else {
				System.out.println("Bit " + pin + " is NOT set!");
			}
		}

		// register a listener on the interrupt pin, reading the new state
		interruptPin.addListener(e -> {
			if (e.state().isHigh())
				handleNewState(outputI2c, inputI2c, currentInputState, currentOutputState);
		});
	}

	private static void handleNewState(I2C outputI2c, I2C inputI2c, AtomicReference<Byte> currentInputState,
			AtomicReference<Byte> currentOutputState) {
		// read new state
		byte inputState = (byte) inputI2c.read();
		for (int pin = 0; pin < Byte.SIZE; pin++) {
			boolean currentBitState = isBitSet(currentInputState.get(), pin);
			boolean newBitState = isBitSet(inputState, pin);

			if (currentBitState != newBitState) {
				System.out.println("Bit " + pin + " changed to " + newBitState + ". Toggling output pin!");

				byte outputData = setBit(currentOutputState.get(), pin, newBitState);
				outputI2c.write(outputData);
				currentOutputState.set(outputData);
			}
		}

		currentInputState.set(inputState);
	}

	public static boolean isBitSet(byte data, int position) {
		if (position > 7)
			throw new IllegalStateException("Position " + position + " is not available in a byte!");
		return ((data >> position) & 1) == 1;
	}

	public static byte setBit(byte data, int position, boolean state) {
		if (position > 7)
			throw new IllegalStateException("Position " + position + " is not available in a byte!");
		if (state)
			return (byte) (data | (1 << position));
		return (byte) (data & ~(1 << position));
	}

	public static String asBinary(byte b) {
		StringBuilder sb = new StringBuilder();

		sb.append(((b >>> 7) & 1));
		sb.append(((b >>> 6) & 1));
		sb.append(((b >>> 5) & 1));
		sb.append(((b >>> 4) & 1));
		sb.append(((b >>> 3) & 1));
		sb.append(((b >>> 2) & 1));
		sb.append(((b >>> 1) & 1));
		sb.append(((b) & 1));

		return sb.toString();
	}
}
