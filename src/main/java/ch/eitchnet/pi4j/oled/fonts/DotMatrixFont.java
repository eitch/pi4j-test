package ch.eitchnet.pi4j.oled.fonts;

/**
 * This is the base class of a dot matrix font to write text to the SH1106 OLED Display
 *
 * @author Roland Baudouin
 */
public abstract class DotMatrixFont implements IFont {
	private final int fontWidth;
	private final int fontHeight;
	private final int spaceWidth;
	private final int startChar;
	private final int stopChar;
	private final int bytesPerChar;
	private final byte[] array;
	private final FONT_SCAN_MODE scanMode;

	public enum FONT_SCAN_MODE {
		FONT_SCAN_H,
		FONT_SCAN_V
	}

	public DotMatrixFont(int fontWidth, int fontHeight, int spaceWidth, int startChar, int stopChar, int bytesPerChar, byte[] array, FONT_SCAN_MODE scanMode) {
		this.fontWidth = fontWidth;
		this.fontHeight = fontHeight;
		this.spaceWidth = spaceWidth;
		this.startChar = startChar;
		this.stopChar = stopChar;
		this.bytesPerChar = bytesPerChar;
		this.array = array;
		this.scanMode = scanMode;
	}

	@Override
	public int getFontWidth() {
		return fontWidth;
	}

	@Override
	public int getFontHeight() {
		return fontHeight;
	}

	@Override
	public int getSpaceWidth() {
		return spaceWidth;
	}

	@Override
	public FONT_SCAN_MODE getScanMode() {
		return scanMode;
	}

	@Override
	public byte[] getBytes(char c) {
		byte[] result = new byte[bytesPerChar];
		int p = c;
		if (p < startChar) p = startChar;
		if (p > stopChar) p = stopChar;
		p = (p - startChar) * bytesPerChar;
		System.arraycopy(array, p, result, 0, bytesPerChar);
		return result;
	}

	public static byte[] reverse(byte[] array) {
		byte[] result = new byte[array.length];
		for (int i = 0; i < array.length; i++) {
			result[i] = reverse(array[i]);
		}
		return result;
	}

	public static byte reverse(byte x) {
		byte b = 0;
		for (int i = 0; i < 8; ++i) {
			b <<= 1;
			b |= (x & 1);
			x >>= 1;
		}
		return b;
	}

	public static void display(String string, IFont font) {
		byte[] bytes = new byte[string.length() * (font.getFontWidth() + font.getSpaceWidth())];
		char[] chars = new char[string.length()];
		string.getChars(0, string.length(), chars, 0);
		int pos = 0;
		for (char c : chars) {
			System.arraycopy(reverse(font.getBytes(c)), 0, bytes, pos, font.getFontWidth());
			pos += (font.getFontWidth() + font.getSpaceWidth());
		}
		System.out.println("####################");
		for (int i = 0; i < font.getFontHeight() + 1; i++) {
			StringBuilder line = new StringBuilder();
			byte mask = (byte) (1 << (font.getFontHeight() - i));
			for (byte b : bytes) {
				if ((b & mask) == mask) line.append("X");
				else line.append(" ");
			}
			System.out.println(line);
		}
		System.out.println("####################");
	}

	public static void main(String[] args) {
		display("Hello", new DotMatrixFont5x7());
		display("WORLD", new DotMatrixFont3x5());
//		byte[] reversed = reverse(DotMatrixFont3x5.DotMatrixFont3x5);
//		for (int i = 0; i < reversed.length; i+=3) {
////			System.out.printf("0x%02X,0x%02X,0x%02X,\n",(byte)(reversed[i]),(byte)(reversed[i+1]),(byte)(reversed[i+2]));
//			System.out.printf("0x%02X,0x%02X,0x%02X,\n",(byte)(((reversed[i]<<1) & 0x1F0)>>4),(byte)(((reversed[i+1]<<1) & 0x1F0)>>4),(byte)(((reversed[i+2]<<1) & 0x1F0)>>4));
//		}
	}

}
