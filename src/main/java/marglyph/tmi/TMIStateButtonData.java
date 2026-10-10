package marglyph.tmi;

class TMIStateButtonData {
	public int state;
	public int action;
	public static final int STATE = 0;
	public static final int CLEAR = 1;

	public TMIStateButtonData(int state, int action) {
		this.state = state;
		this.action = action;
	}
}
