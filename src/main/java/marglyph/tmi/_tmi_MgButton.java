package marglyph.tmi;

import static marglyph.tmi._tmi_MgCanvas.*;

public class _tmi_MgButton extends _tmi_MgWidget {
	public String label;
	public _tmi_MgButtonHandler controller;
	public Object data;

	public _tmi_MgButton(String label, _tmi_MgButtonHandler controller, Object data) {
		this(0, 0, 0, 0, 0, label, controller, data);
	}

	public _tmi_MgButton(int x, int y, int z, int width, int height, String label, _tmi_MgButtonHandler controller, Object data) {
		super(x, y, z, width, height);
		this.label = label;
		this.controller = controller;
		this.data = data;
	}

	@Override
	public void draw(_tmi_MgCanvas canvas, int mouseX, int mouseY) {
		canvas.drawRect(this.x, this.y, this.width, this.height, this.contains(mouseX, mouseY) ? RED_SHADE : SHADE);
		canvas.drawTextCentered(this.x, this.y, this.width, this.height, this.label, -1);
	}

	@Override
	public boolean click(int mouseX, int mouseY, int mouseButton) {
		return mouseButton != 0 || this.controller.onButtonPress(this.data);
	}
}
