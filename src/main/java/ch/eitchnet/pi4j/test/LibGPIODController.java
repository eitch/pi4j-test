package ch.eitchnet.pi4j.test;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.logging.Logger;

public class LibGPIODController {

	private static final Logger logger = Logger.getLogger(LibGPIODController.class.getName());

	// Path to the libgpiod library
	private static final String LIBGPIOD_PATH = "/usr/lib/aarch64-linux-gnu/libgpiod.so.2";

	// ForeignLinker and MemorySession for memory management
	private static final Linker linker = Linker.nativeLinker();

	// Method handles for libgpiod functions
	private static final MethodHandle gpiod_chip_open_by_name;
	private static final MethodHandle gpiod_chip_get_line;
	private static final MethodHandle gpiod_line_request_output;
	private static final MethodHandle gpiod_line_request_input;
	private static final MethodHandle gpiod_line_get_value;
	private static final MethodHandle gpiod_line_set_value;
	private static final MethodHandle gpiod_line_request_both_edges_events;
	private static final MethodHandle gpiod_line_event_wait;
	private static final MethodHandle gpiod_line_event_read;
	private static final MethodHandle gpiod_line_event_get_fd;
	private static final MethodHandle gpiod_line_event_read_fd;
	private static final MethodHandle gpiod_line_release;
	private static final MethodHandle gpiod_chip_close;

	static {
		try {
			SymbolLookup gpiodLib = SymbolLookup.libraryLookup(Path.of(LIBGPIOD_PATH), Arena.global());

			gpiod_chip_open_by_name = linker.downcallHandle(gpiodLib.find("gpiod_chip_open_by_name").orElseThrow(),
					FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

			gpiod_chip_get_line = linker.downcallHandle(gpiodLib.find("gpiod_chip_get_line").orElseThrow(),
					FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

			gpiod_line_request_output = linker.downcallHandle(gpiodLib.find("gpiod_line_request_output").orElseThrow(),
					FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
							ValueLayout.JAVA_INT));

			gpiod_line_request_input = linker.downcallHandle(gpiodLib.find("gpiod_line_request_input").orElseThrow(),
					FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

			gpiod_line_get_value = linker.downcallHandle(gpiodLib.find("gpiod_line_get_value").orElseThrow(),
					FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

			gpiod_line_set_value = linker.downcallHandle(gpiodLib.find("gpiod_line_set_value").orElseThrow(),
					FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

			gpiod_line_request_both_edges_events = linker.downcallHandle(
					gpiodLib.find("gpiod_line_request_both_edges_events").orElseThrow(),
					FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

			gpiod_line_event_wait = linker.downcallHandle(gpiodLib.find("gpiod_line_event_wait").orElseThrow(),
					FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

			gpiod_line_event_read = linker.downcallHandle(gpiodLib.find("gpiod_line_event_read").orElseThrow(),
					FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

			gpiod_line_event_get_fd = linker.downcallHandle(gpiodLib.find("gpiod_line_event_get_fd").orElseThrow(),
					FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

			gpiod_line_event_read_fd = linker.downcallHandle(gpiodLib.find("gpiod_line_event_read_fd").orElseThrow(),
					FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

			gpiod_line_release = linker.downcallHandle(gpiodLib.find("gpiod_line_release").orElseThrow(),
					FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

			gpiod_chip_close = linker.downcallHandle(gpiodLib.find("gpiod_chip_close").orElseThrow(),
					FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize libgpiod function handles", e);
		}
	}

	public void toggleGPIO(String chipName, int lineNumber) {
		try (Arena arena = Arena.ofConfined()) {
			java.lang.foreign.MemorySegment chipNameSegment = arena.allocateFrom(chipName);

			// Open chip
			MemorySegment chip = (MemorySegment) gpiod_chip_open_by_name.invoke(chipNameSegment);
			if (chip.equals(MemorySegment.NULL)) {
				logger.severe("Failed to open GPIO chip");
				return;
			}

			// Get line
			MemorySegment line = (MemorySegment) gpiod_chip_get_line.invoke(chip, lineNumber);
			if (line.equals(MemorySegment.NULL)) {
				logger.severe("Failed to get GPIO line");
				gpiod_chip_close.invoke(chip);
				return;
			}

			// Request line as output
			int requestResult = (int) gpiod_line_request_output.invoke(line, MemorySegment.NULL, 0);
			if (requestResult != 0) {
				logger.severe("Failed to request GPIO line as output. Error code: " + requestResult);
				gpiod_line_release.invoke(line);
				gpiod_chip_close.invoke(chip);
				return;
			}

			// Toggle GPIO line
			logger.info("Setting GPIO high");
			gpiod_line_set_value.invoke(line, 1);
			Thread.sleep(500);

			logger.info("Setting GPIO low");
			gpiod_line_set_value.invoke(line, 0);

			// Clean up
			gpiod_line_release.invoke(line);
			gpiod_chip_close.invoke(chip);
		} catch (Throwable e) {
			logger.severe("Error during GPIO operation: " + e.getMessage());
		}
	}

	public void readGPIO(String chipName, int lineNumber) {
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment chipNameSegment = arena.allocateFrom(chipName);

			// Open chip
			MemorySegment chip = (MemorySegment) gpiod_chip_open_by_name.invoke(chipNameSegment);
			if (chip.equals(MemorySegment.NULL)) {
				logger.severe("Failed to open GPIO chip");
				return;
			}

			// Get line
			MemorySegment line = (MemorySegment) gpiod_chip_get_line.invoke(chip, lineNumber);
			if (line.equals(MemorySegment.NULL)) {
				logger.severe("Failed to get GPIO line");
				gpiod_chip_close.invoke(chip);
				return;
			}

			// Request line as input
			if ((int) gpiod_line_request_input.invoke(line, MemorySegment.NULL) != 0) {
				logger.severe("Failed to request GPIO line as input");
				gpiod_line_release.invoke(line);
				gpiod_chip_close.invoke(chip);
				return;
			}

			// Read GPIO line value
			int value = (int) gpiod_line_get_value.invoke(line);
			logger.info("GPIO line " + lineNumber + " value: " + value);

			// Clean up
			gpiod_line_release.invoke(line);
			gpiod_chip_close.invoke(chip);
		} catch (Throwable e) {
			logger.severe("Error during GPIO operation: " + e.getMessage());
		}
	}

	public void monitorGPIO(String chipName, int lineNumber) {
		try (Arena arena = Arena.ofConfined()) {
			// Correct usage of allocateFrom for UTF-8 string allocation
			MemorySegment chipNameSegment = arena.allocateFrom(chipName);
			MemorySegment eventStruct = arena.allocate(16); // gpiod_line_event struct (adjust size if necessary)
			MemorySegment timeout = arena.allocate(
					16); // struct timespec (8 bytes for seconds, 8 bytes for nanoseconds)

			// Set timeout to 2 seconds
			timeout.set(ValueLayout.JAVA_LONG, 0, 2); // 2 seconds
			timeout.set(ValueLayout.JAVA_LONG, 8, 0); // 0 nanoseconds

			// Open GPIO chip
			MemorySegment chip = (MemorySegment) gpiod_chip_open_by_name.invoke(chipNameSegment);
			if (chip.equals(MemorySegment.NULL)) {
				logger.severe("Failed to open GPIO chip");
				return;
			}

			// Get GPIO line
			MemorySegment line = (MemorySegment) gpiod_chip_get_line.invoke(chip, lineNumber);
			if (line.equals(MemorySegment.NULL)) {
				logger.severe("Failed to get GPIO line");
				gpiod_chip_close.invoke(chip);
				return;
			}

			// Request GPIO line for edge events
			if ((int) gpiod_line_request_both_edges_events.invoke(line, MemorySegment.NULL) != 0) {
				logger.severe("Failed to request GPIO line for edge events");
				gpiod_line_release.invoke(line);
				gpiod_chip_close.invoke(chip);
				return;
			}

			logger.info("Monitoring GPIO line " + lineNumber + " for button presses...");

			// Poll for events
			while (true) {
				int waitResult = (int) gpiod_line_event_wait.invoke(line, timeout);
				if (waitResult < 0) {
					logger.severe("Error waiting for GPIO event");
					break;
				} else if (waitResult == 0) {
					logger.info("No event detected within timeout");
					continue;
				}

				// Read the event
				if ((int) gpiod_line_event_read.invoke(line, eventStruct) != 0) {
					logger.severe("Failed to read GPIO event");
					break;
				}

				// Process event
				int eventType = eventStruct.get(ValueLayout.JAVA_INT, 0); // First 4 bytes indicate event type
				if (eventType == 1) {
					logger.info("Rising edge detected (button pressed)");
				} else if (eventType == 2) {
					logger.info("Falling edge detected (button released)");
				} else {
					logger.warning("Unknown event type: " + eventType);
				}
			}

			// Clean up
			gpiod_line_release.invoke(line);
			gpiod_chip_close.invoke(chip);
		} catch (Throwable e) {
			logger.severe("Error monitoring GPIO: " + e.getMessage());
		}
	}

	public void monitorGPIOEvents(String chipName, int lineNumber) {
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment chipNameSegment = arena.allocateFrom(chipName);
			MemorySegment eventStruct = arena.allocate(24); // 16 bytes for timespec + 4 bytes for event_type + padding

			// Open GPIO chip
			MemorySegment chip = (MemorySegment) gpiod_chip_open_by_name.invoke(chipNameSegment);
			if (chip.equals(MemorySegment.NULL)) {
				logger.severe("Failed to open GPIO chip");
				return;
			}

			// Get GPIO line
			MemorySegment line = (MemorySegment) gpiod_chip_get_line.invoke(chip, lineNumber);
			if (line.equals(MemorySegment.NULL)) {
				logger.severe("Failed to get GPIO line");
				gpiod_chip_close.invoke(chip);
				return;
			}

			// Request GPIO line for edge events
			if ((int) gpiod_line_request_both_edges_events.invoke(line, MemorySegment.NULL) != 0) {
				logger.severe("Failed to request GPIO line for edge events");
				gpiod_line_release.invoke(line);
				gpiod_chip_close.invoke(chip);
				return;
			}

			// Get the file descriptor for the line
			int fd = (int) gpiod_line_event_get_fd.invoke(line);
			if (fd < 0) {
				logger.severe("Failed to get file descriptor for GPIO line");
				gpiod_line_release.invoke(line);
				gpiod_chip_close.invoke(chip);
				return;
			}

			logger.info("Monitoring GPIO line " + lineNumber + " for events...");

			// Monitor the file descriptor using a blocking loop
			while (true) {
				// Block until an event occurs
				int result = (int) gpiod_line_event_read_fd.invoke(fd, eventStruct);
				if (result != 0) {
					logger.severe("Failed to read GPIO event from file descriptor");
					break;
				}

				// Access event_type at offset 16 (after timespec)
				int eventType = eventStruct.get(ValueLayout.JAVA_INT, 16);
				if (eventType == 1) {
					logger.info("Rising edge detected (button pressed)");
				} else if (eventType == 2) {
					logger.info("Falling edge detected (button released)");
				} else {
					logger.warning("Unknown event type: " + eventType);
				}
			}

			// Clean up
			gpiod_line_release.invoke(line);
			gpiod_chip_close.invoke(chip);
		} catch (Throwable e) {
			logger.severe("Error monitoring GPIO: " + e.getMessage());
		}
	}

	public static void main(String[] args) {
		LibGPIODController controller = new LibGPIODController();
		controller.toggleGPIO("gpiochip0", 22);
		controller.readGPIO("gpiochip0", 24);
		//controller.monitorGPIO("gpiochip0", 24);
		controller.monitorGPIOEvents("gpiochip0", 24);
	}
}
