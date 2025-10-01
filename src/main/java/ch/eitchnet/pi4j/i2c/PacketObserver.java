package ch.eitchnet.pi4j.i2c;

public interface PacketObserver {

	void notifySent(byte[] sent);

	void notifyReceived(byte[] received);

	void notifyInfo(String msg);

	void notifyError(String msg);

	void notifyError(String msg, Throwable e);
}
