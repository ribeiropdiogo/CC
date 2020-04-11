import java.nio.ByteBuffer;
import java.util.Arrays;

public final class AnonPacket {
	public static final int MAX_SIZE = 1000;
	private static final int SEQ_OSET = 0;
	private static final int ACK_OSET = 32;

	private static final int SYN_FLAG = 64;
	private static final int ACK_FLAG = 65;
	private static final int FIN_FLAG = 66;

	private ByteBuffer workbuf;

	public AnonPacket() {
		this.workbuf = ByteBuffer.allocate(MAX_SIZE);
	}

	public AnonPacket(byte[] buf) {
		this.workbuf = ByteBuffer.wrap(buf);
	}

	public byte[] getRawData() {
		return workbuf.array();
	}

	public int getSeqNumber() {
		return workbuf.getInt(SEQ_OSET);
	}

	public int getAckNumber() {
		return workbuf.getInt(ACK_OSET);
	}

	public byte[] getData() {
		return Arrays.copyOfRange(workbuf.array(), FIN_FLAG + 1, MAX_SIZE);
	}

	public boolean isSyn() {
		return getBool(SYN_FLAG);
	}

	public boolean isAck() {
		return getBool(ACK_FLAG);
	}

	public boolean isFin() {
		return getBool(FIN_FLAG);
	}

	public AnonPacket setSyn(boolean flag) {
		return putBool(SYN_FLAG, flag);
	}

	public AnonPacket setAck(boolean flag) {
		return putBool(ACK_FLAG, flag);
	}

	public AnonPacket setFin(boolean flag) {
		return putBool(FIN_FLAG, flag);
	}

	public AnonPacket setSeqNumber(int seqnum) {
		return putInt(SEQ_OSET, seqnum);
	}

	public AnonPacket setAckNumber(int acknum) {
		return putInt(ACK_OSET, acknum);
	}

	public AnonPacket setData(byte[] buf) {
		int ind = FIN_FLAG + 1;
		int len = MAX_SIZE - ind;

		if(buf.length < len)
			len = buf.length;

		for(int i = 0; i < len; i++) {
			workbuf.put(ind + i, buf[i]);
		}

		return this;
	}

	private AnonPacket putBool(int index, boolean val) {
		short x = 0;

		if(val)
			x = 1;

		workbuf.putShort(index, x);
		return this;
	}

	private AnonPacket putInt(int index, int num) {
		workbuf.putInt(index, num);
		return this;
	}

	private boolean getBool(int index) {
		boolean res = false;
		short flag = workbuf.getShort(index);

		if(flag == 1)
			res = true;

		return res;
	}
}