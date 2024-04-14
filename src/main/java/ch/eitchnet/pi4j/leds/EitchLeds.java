package ch.eitchnet.pi4j.leds;

import ch.eitchnet.pi4j.i2c.Pi4jI2cBus;
import ch.eitchnet.pi4j.oled.SH1106;
import ch.eitchnet.pi4j.oled.fonts.DotMatrixFont5x7;
import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EitchLeds {

	public static int PIN_LED_MATRIX = 14;

	public static int PIN_LAMP_1_R = 17;
	public static int PIN_LAMP_1_Y = 18;
	public static int PIN_LAMP_1_G = 27;

	public static int PIN_LAMP_2_R = 22;
	public static int PIN_LAMP_2_Y = 23;
	public static int PIN_LAMP_2_G = 24;

	public static int I2C_ADDR_DISPLAY = 0x3c;

	private static final Logger logger = LoggerFactory.getLogger(EitchLeds.class);

	public static void main(String[] args) throws InterruptedException {
		Context pi4j = Pi4J.newAutoContext();

		Lamp leftLamp = new Lamp(pi4j, PIN_LAMP_1_R, PIN_LAMP_1_Y, PIN_LAMP_1_G);
		Lamp rightLamp = new Lamp(pi4j, PIN_LAMP_2_R, PIN_LAMP_2_Y, PIN_LAMP_2_G);
		Lamps lamps = new Lamps(leftLamp, rightLamp);

		SH1106 display = new SH1106(rawBus == null ? new Pi4jI2cBus(false) : rawBus);
		display.setFont(new DotMatrixFont5x7());

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				logger.info("Shutting down...");
				pi4j.shutdown();
			} catch (Exception e) {
				logger.error("Failed to shutdown", e);
			}
		}));

		while (true) {
			lamps.getLeft().redOn();
			Thread.sleep(500);
			lamps.getLeft().redOff();

			lamps.getLeft().yellowOn();
			Thread.sleep(500);
			lamps.getLeft().yellowOff();

			lamps.getLeft().greenOn();
			Thread.sleep(500);
			lamps.getLeft().greenOff();
		}
	}
}
