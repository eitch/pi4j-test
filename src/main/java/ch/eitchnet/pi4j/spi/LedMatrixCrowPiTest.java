package ch.eitchnet.pi4j.spi;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public class LedMatrixCrowPiTest {
	public static final String LIB_BCM_2835_SO = "libbcm2835.so";

	// Foreignlinker and MemorySession for memory management
	private static final Linker linker = Linker.nativeLinker();

	private static final SymbolLookup LIBRARY_LOOKUP = SymbolLookup.libraryLookup(LIB_BCM_2835_SO, Arena.global());
	private static final MethodHandle BCM2835_INIT;
	private static final MethodHandle BCM2835_SPI_BEGIN;
	private static final MethodHandle BCM2835_SPI_TRANSFER;
	private static final MethodHandle BCM2835_SPI_END;
	private static final MethodHandle BCM2835_CLOSE;

	static {
		try {
			BCM2835_INIT = linker.downcallHandle(LIBRARY_LOOKUP
							.find("bcm2835_init")
							.orElseThrow(() -> new RuntimeException("Function bcm2835_init not found")),
					FunctionDescriptor.of(ValueLayout.JAVA_INT));

			BCM2835_SPI_BEGIN = linker.downcallHandle(LIBRARY_LOOKUP
							.find("bcm2835_spi_begin")
							.orElseThrow(() -> new RuntimeException("Function bcm2835_spi_begin not found")),
					FunctionDescriptor.of(ValueLayout.JAVA_INT));

			BCM2835_SPI_TRANSFER = linker.downcallHandle(LIBRARY_LOOKUP
							.find("bcm2835_spi_transfer")
							.orElseThrow(() -> new RuntimeException("Function bcm2835_spi_transfer not found")),
					FunctionDescriptor.of(ValueLayout.JAVA_BYTE, ValueLayout.JAVA_BYTE));

			BCM2835_SPI_END = linker.downcallHandle(LIBRARY_LOOKUP
							.find("bcm2835_spi_end")
							.orElseThrow(() -> new RuntimeException("Function bcm2835_spi_end not found")),
					FunctionDescriptor.ofVoid());

			BCM2835_CLOSE = linker.downcallHandle(LIBRARY_LOOKUP
							.find("bcm2835_close")
							.orElseThrow(() -> new RuntimeException("Function bcm2835_close not found")),
					FunctionDescriptor.ofVoid());
		} catch (Throwable t) {
			throw new RuntimeException("Error initializing MethodHandles", t);
		}
	}

	public static void main(String[] args) {
		try (Arena arena = Arena.ofConfined()) {
			// Initialize SPI
			int initResult = (int) BCM2835_INIT.invokeExact();
			if (initResult == 0) {
				throw new RuntimeException("Failed to initialize bcm2835");
			}

			int spiBeginResult = (int) BCM2835_SPI_BEGIN.invokeExact();
			if (spiBeginResult == 0) {
				throw new RuntimeException("Failed to start SPI");
			}

			// Perform SPI transfer
			byte dataToSend = 0x55; // Example data
			byte receivedData = (byte) BCM2835_SPI_TRANSFER.invokeExact(dataToSend);
			System.out.println("Received Data: " + receivedData);

			// End SPI and clean up
			BCM2835_SPI_END.invokeExact();
			BCM2835_CLOSE.invokeExact();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
