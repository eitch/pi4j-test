package ch.eitchnet.pi4j.i2c;

public class StringHelper {

	public static String toHexString(byte data) {
		return String.format("%02x", data);
	}

	/**
	 * Formats the given number of milliseconds to a time like #h/m/s/ms/us/ns
	 *
	 * @param millis the number of milliseconds
	 *
	 * @return format the given number of milliseconds to a time like #h/m/s/ms/us/ns
	 */
	public static String formatMillisecondsDuration(final long millis) {
		return formatNanoDuration(millis * 1000000L);
	}

	/**
	 * Formats the given number of nanoseconds to a time like #h/m/s/ms/us/ns
	 *
	 * @param nanos the number of nanoseconds
	 *
	 * @return format the given number of nanoseconds to a time like #h/m/s/ms/us/ns
	 */
	public static String formatNanoDuration(final long nanos) {
		if (nanos >= 3600000000000L) {
			return String.format("%.0fh", (nanos / 3600000000000.0D));
		} else if (nanos >= 60000000000L) {
			return String.format("%.0fm", (nanos / 60000000000.0D));
		} else if (nanos >= 1000000000L) {
			return String.format("%.0fs", (nanos / 1000000000.0D));
		} else if (nanos >= 1000000L) {
			return String.format("%.0fms", (nanos / 1000000.0D));
		} else if (nanos >= 1000L) {
			return String.format("%.0fus", (nanos / 1000.0D));
		} else {
			return nanos + "ns";
		}
	}

	public static String toPrettyHexString(byte[] raw) {
		return toPrettyHexString(raw, 0, raw.length);
	}

	public static String toPrettyHexString(byte[] raw, int srcPos, int length) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			sb.append(String.format("%02x", raw[i + srcPos]));
			sb.append(' ');
			if ((i + srcPos) % 8 == 0) {
				sb.append(' ');
			}
		}

		return sb.toString();
	}
}
