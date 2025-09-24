package ch.eitchnet.pi4j.test;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.PullResistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class GpioInputOutputTest {

	private static final Logger logger = LoggerFactory.getLogger(GpioInputOutputTest.class);

	private static final int PIN_LED = 22;
	private static final int PIN_BTN = 24;
	private static final int PIN_5 = 5;

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		Context pi4j = Pi4J.newContextBuilder().autoDetect().disableShutdownHook().build();
		DigitalOutput led = pi4j.dout().create(PIN_LED);

		var buttonConfig = DigitalInput
				.newConfigBuilder(pi4j)
				.id("button")
				.name("Press button")
				.address(PIN_BTN)
				.pull(PullResistance.PULL_DOWN)
				.debounce(3000L);
		DigitalInput btn = pi4j.din().create(buttonConfig);

		btn.addListener(e -> {
			logger.info("Button state: {}", e.state());
			switch (e.state()) {
				case LOW -> led.low();
				case HIGH -> led.high();
			}
		});

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
