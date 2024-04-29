package ch.eitchnet.pi4j.leds;

import ch.eitchnet.pi4j.i2c.Pi4jI2cBus;
import ch.eitchnet.pi4j.oled.SH1106;
import ch.eitchnet.pi4j.oled.fonts.DotMatrixFont5x7;
import ch.eitchnet.pi4j.pboe.PixelBlazeOutputExpander;
import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.event.ShutdownEvent;
import com.pi4j.event.ShutdownListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static ch.eitchnet.pi4j.pboe.ImageHelper.getImageData;
import static ch.eitchnet.pi4j.pboe.ImageHelper.imageToMatrix;

public class EitchLeds {

	public static final String SERIAL_PORT = "/dev/serial0";
	public static int PIN_LED_MATRIX = 14;

	public static int PIN_LAMP_1_R = 17;
	public static int PIN_LAMP_1_Y = 18;
	public static int PIN_LAMP_1_G = 27;

	public static int PIN_LAMP_2_R = 22;
	public static int PIN_LAMP_2_Y = 23;
	public static int PIN_LAMP_2_G = 24;

	public static int I2C_ADDR_DISPLAY = 0x3c;

	private static final int MATRIX_CHANNEL = 1;
	private static final int MATRIX_BYTES_PER_PIXEL = 3;
	private static final int MATRIX_WIDTH = 8;
	private static final int MATRIX_HEIGHT = 8;
	private static final int MATRIX_NUMBER_OF_LEDS = MATRIX_HEIGHT * MATRIX_WIDTH;

	private static final Logger logger = LoggerFactory.getLogger(EitchLeds.class);

	public static void main(String[] args) throws InterruptedException, IOException {
		Context pi4j = Pi4J.newAutoContext();

		Lamp leftLamp = new Lamp(pi4j, PIN_LAMP_1_R, PIN_LAMP_1_Y, PIN_LAMP_1_G);
		Lamp rightLamp = new Lamp(pi4j, PIN_LAMP_2_R, PIN_LAMP_2_Y, PIN_LAMP_2_G);
		Lamps lamps = new Lamps(leftLamp, rightLamp);

		Pi4jI2cBus i2cBus = new Pi4jI2cBus(pi4j, false);
		SH1106 display = new SH1106(i2cBus);
		display.setFont(new DotMatrixFont5x7());
		display.writeLine(1, "Hello World", true);
		display.display();

		pi4j.addListener(new ShutdownListener() {
			@Override
			public void beforeShutdown(ShutdownEvent event) {
				try {
					display.clear(true);
				} catch (Exception e) {
					logger.error("Error clearing display", e);
				}
			}

			@Override
			public void onShutdown(ShutdownEvent event) {
				// do nothing
			}
		});

		PixelBlazeOutputExpander pixelBlaze = new PixelBlazeOutputExpander(SERIAL_PORT);
		pixelBlaze.sendAllOff(MATRIX_CHANNEL, MATRIX_NUMBER_OF_LEDS);
		Thread.sleep(100L);

		showImage(pixelBlaze, "/heart_8_8.png");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				logger.info("Shutdown hook called...");

				logger.info("Clearing pixelblaze...");
				PixelBlazeOutputExpander tmp = new PixelBlazeOutputExpander(SERIAL_PORT);
				tmp.sendAllOff(MATRIX_CHANNEL, MATRIX_NUMBER_OF_LEDS);
				logger.info("Shutting down pixelblaze...");
				tmp.closePort();

			} catch (Exception e) {
				logger.error("Failed to shutdown", e);
			}
		}));

		logger.info("Running...");
		while (true) {
			lamps.getLeft().redOn();
			display.writeLine(2, "Red On      ", true);
			display.display();
			Thread.sleep(1000);
			lamps.getLeft().redOff();

			lamps.getLeft().yellowOn();
			display.writeLine(2, "Yellow On   ", true);
			display.display();
			Thread.sleep(1000);
			lamps.getLeft().yellowOff();

			lamps.getLeft().greenOn();
			display.writeLine(2, "Green On    ", true);
			display.display();
			Thread.sleep(1000);
			lamps.getLeft().greenOff();
		}
	}

	private static void showImage(PixelBlazeOutputExpander pixelBlaze, String imageName) throws IOException {

		InputStream imageStream = EitchLeds.class.getResourceAsStream(imageName);
		if (imageStream == null)
			throw new IllegalStateException("Image " + imageName + " was not found!");
		BufferedImage bufferedImage = ImageIO.read(imageStream);

		// Get the bytes from the given image
		byte[] pixelsRgb = imageToMatrix(
				getImageData(imageName, bufferedImage, MATRIX_BYTES_PER_PIXEL, MATRIX_WIDTH, MATRIX_HEIGHT, 90, 10),
				MATRIX_BYTES_PER_PIXEL, MATRIX_WIDTH, MATRIX_HEIGHT);

		pixelBlaze.sendColors(MATRIX_CHANNEL, pixelsRgb, false);
	}
}
