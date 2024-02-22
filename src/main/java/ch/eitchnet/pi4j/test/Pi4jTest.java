package ch.eitchnet.pi4j.test;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.Digital;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Pi4jTest {

	private static final Logger logger = LoggerFactory.getLogger(Pi4jTest.class);

	// Connect a LED to PIN 15 = BCM 22
	private static final int PIN_LED_YELLOW = 19;
	private static final int PIN_LED_RED = 26;
	private static final int PIN_LED_GREEN = 13;
	private static final int PIN_BTN_RED = 6;
	private static final int PIN_5 = 5;

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		Context pi4j = Pi4J.newAutoContext();
		DigitalOutput ledRed = pi4j.dout().create(PIN_LED_RED);
		DigitalOutput ledYellow = pi4j.dout().create(PIN_LED_YELLOW);
		DigitalOutput ledGreen = pi4j.dout().create(PIN_LED_GREEN);
		DigitalInput redBtn = pi4j.din().create(PIN_BTN_RED);

		ledRed.blink(1, 5, TimeUnit.SECONDS);
		ledYellow.pulse(1, TimeUnit.SECONDS);
		Future<?> blinkRed = ledRed.blinkAsync(1, 5, TimeUnit.SECONDS);
		Future<?> pulseYellow = ledYellow.pulseAsync(1, TimeUnit.SECONDS);
		blinkRed.get();
		pulseYellow.get();

		AtomicReference<Digital> greenBtnRef = new AtomicReference<>();

		redBtn.addListener(e -> {
			if (e.state().isHigh())
				return;

			logger.info("Toggling state...");
			Digital<?, ?, ?> digital = greenBtnRef.get();
			if (digital != null && pi4j.hasIO(digital.id())) {
				logger.info("Shutting down " + digital.id() + "...");
				pi4j.shutdown(digital.id());
				logger.info("Done.");
			}

			// toggle PIN_5 between output or input
			if (digital instanceof DigitalInput) {
				// set to output
				logger.info("Setting pin " + PIN_5 + " to output.");
				digital = pi4j.dout().create(PIN_5);
				logger.info("Pin " + PIN_5 + " is now output " + digital.id());
			} else {
				// set to input
				logger.info("Setting pin " + PIN_5 + " to input.");
				digital = pi4j.din().create(PIN_5);
				digital.addListener(e1 -> {
					if (e1.state().isHigh())
						ledRed.high();
					else
						ledRed.low();
				});
				logger.info("Pin " + PIN_5 + " is now input " + digital.id());
			}

			greenBtnRef.set(digital);
			logger.info("");
			logger.info("");
		});

		ledRed.low();
		ledYellow.low();
		ledRed.high();
		ledYellow.high();
		Thread.sleep(2000L);
		ledRed.low();
		ledYellow.low();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					logger.info("Shutting down...");
					pi4j.shutdown();
				} catch (Exception e) {
					logger.error("Failed to shutdown", e);
				}
			}
		});

		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
