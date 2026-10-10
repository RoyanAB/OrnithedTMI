package marglyph.tmi;

public class _tmi_MgWidget {
	public int x;
	public int y;
	public int z;
	public int width;
	public int height;

	public _tmi_MgWidget(int x, int y, int z, int width, int height) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.width = width;
		this.height = height;
	}

	public static _tmi_MgZOrder getComparator() {
		return new _tmi_MgZOrder();
	}

	public void draw(_tmi_MgCanvas canvas, int mouseX, int mouseY) {
	}

	public boolean click(int mouseX, int mouseY, int mouseButton) {
		return true;
	}

	public boolean contains(int mouseX, int mouseY) {
		return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
	}

	public void resize() {

	}
}
