package ch.eitchnet.pi4j.i2c;

import com.jcraft.jsch.Packet;

public interface PacketObserver {

	void notify(Packet packet);

	void notifySent(byte[] sent);

	void notifyReceived(byte[] received);

	void notifyInfo(String msg);

	void notifyError(String msg);

	void notifyError(String msg, Throwable e);
}
