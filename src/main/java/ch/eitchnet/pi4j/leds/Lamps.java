package ch.eitchnet.pi4j.leds;

public class Lamps {
	private final Lamp left;
	private final Lamp right;

	public Lamps(Lamp left, Lamp right) {
		this.left = left;
		this.right = right;
	}

	public Lamp getLeft() {
		return this.left;
	}

	public Lamp getRight() {
		return this.right;
	}
}
