package ch.eitchnet.pi4j.test;

import java.util.concurrent.ExecutionException;

public class Main {
	public static void main(String[] args) throws ExecutionException, InterruptedException {
		if (args.length == 0) {
			System.out.println("Usage: java -jar Pi4jTest.jar <GpioTest|I2cTest> ");
			System.exit(1);
		}

		switch (args[0]) {
			case "GpioTest" -> GpioTest.main(args);
			case "I2cTest" -> I2cTest.main(args);
			default -> {
				System.out.println("Usage: java -jar Pi4jTest.jar <GpioTest|I2cTest> ");
				System.exit(1);
			}
		}
	}
}
