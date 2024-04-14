package ch.eitchnet.pi4j.oled.fonts;

public interface IFont {

	int getFontWidth();

	int getFontHeight();

	int getSpaceWidth();

	byte[] getBytes(char c);
	DotMatrixFont.FONT_SCAN_MODE getScanMode();
}
