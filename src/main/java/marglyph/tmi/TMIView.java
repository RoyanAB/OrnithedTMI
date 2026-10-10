package marglyph.tmi;

import org.lwjgl.input.Keyboard;

public class TMIView {
	private final _tmi_MgCanvas canvas;
	private final TMIConfig config;
	private final TMIController controller;
	private boolean widgetsCreated = false;
	private _tmi_MgButton prevButton;
	private _tmi_MgButton nextButton;
	private _tmi_MgButton trashButton;
	private _tmi_MgButton[] stateButtons;
	private final _tmi_MgButton[] deleteButtons;
	public _tmi_MgItemPanel itemPanel;

	public TMIView(_tmi_MgCanvas canvas, TMIConfig config, TMIController controller) {
		this.canvas = canvas;
		this.config = config;
		this.controller = controller;
		this.stateButtons = new _tmi_MgButton[this.config.getNumSaves()];
		this.deleteButtons = new _tmi_MgButton[this.config.getNumSaves()];
		this.createWidgets();
	}

	public void createWidgets() {
		this.prevButton = new _tmi_MgButton("Prev", this.controller, "prev");
		this.canvas.widgets.add(this.prevButton);
		this.nextButton = new _tmi_MgButton("Next", this.controller, "next");
		this.canvas.widgets.add(this.nextButton);
		this.itemPanel = new _tmi_MgItemPanel(0, 0, 0, 0, 0, this.config.getItems(), this.controller);
		this.canvas.widgets.add(this.itemPanel);
		if (!TMIConfig.isMultiplayer()) {
			this.trashButton = new _tmi_MgButton("Trash", this.controller, "trash");
			this.canvas.widgets.add(this.trashButton);
			this.stateButtons = new _tmi_MgButton[this.config.getNumSaves()];

			for (int saveIndex = 0; saveIndex < this.config.getNumSaves(); saveIndex++) {
				this.stateButtons[saveIndex] = new _tmi_MgButton("Save " + (saveIndex + 1), this.controller, new TMIStateButtonData(saveIndex, TMIStateButtonData.STATE));
				this.canvas.widgets.add(this.stateButtons[saveIndex]);
				this.deleteButtons[saveIndex] = new _tmi_MgButton("x", this.controller, new TMIStateButtonData(saveIndex, TMIStateButtonData.CLEAR));
				this.canvas.widgets.add(this.deleteButtons[saveIndex]);
			}
		}

		this.widgetsCreated = true;
	}

	public void layout(int screenWidth, int screenHeight, int panelWidth, int panelHeight) {
		if (!this.isInitialized()) {
			this.createWidgets();
		}

		this.prevButton.x = (panelWidth + screenWidth) / 2;
		this.prevButton.y = 0;
		this.prevButton.width = this.canvas.windowX;
		this.prevButton.height = 28;
		this.nextButton.x = (panelWidth + screenWidth) / 2;
		this.nextButton.y = screenHeight - 28;
		this.nextButton.width = this.canvas.windowX;
		this.nextButton.height = 28;
		this.itemPanel.x = (screenWidth + panelWidth) / 2 + 5;
		this.itemPanel.y = 30;
		this.itemPanel.width = screenWidth - 5 - this.itemPanel.x;
		this.itemPanel.height = screenHeight - 30 - this.itemPanel.y;
		this.itemPanel.resize();
		this.prevButton.label = "Prev (" + (_tmi_MgItemPanel.page + 1) + "/" + this.itemPanel.numPages + ")";
		if (!TMIConfig.isMultiplayer()) {
			this.trashButton.x = 0;
			this.trashButton.y = screenHeight - 30;
			this.trashButton.width = 82;
			this.trashButton.height = 30;
			if (TMIUtils.getHeldItem() != null || !Keyboard.isKeyDown(54) && !Keyboard.isKeyDown(42)) {
				this.trashButton.label = "Trash";
			} else {
				this.trashButton.label = "Delete all";
			}

			int maxButtonWidth = 0;

			for (int saveIndex = 0; saveIndex < this.config.getNumSaves(); saveIndex++) {
				this.deleteButtons[saveIndex].x = -1000;
				this.stateButtons[saveIndex].y = 30 + saveIndex * 22;
				this.stateButtons[saveIndex].height = 20;
				String saveName = this.config.getSettings().get("save-name" + (saveIndex + 1));
				if (saveName == null) {
					saveName = "" + (saveIndex + 1);
				}

				if (this.config.isStateSaved(saveIndex)) {
					this.stateButtons[saveIndex].label = "Load " + saveName;
				} else {
					this.stateButtons[saveIndex].label = "Save " + saveName;
				}

				int buttonWidth = this.canvas.getTextWidth(this.stateButtons[saveIndex].label) + 26;
				if (buttonWidth + 2 + 20 > this.canvas.windowX) {
					buttonWidth = this.canvas.windowX - 20 - 2;
				}

				if (buttonWidth > maxButtonWidth) {
					maxButtonWidth = buttonWidth;
				}
			}

			for (int saveIndex = 0; saveIndex < this.config.getNumSaves(); saveIndex++) {
				this.stateButtons[saveIndex].width = maxButtonWidth;
				if (this.config.isStateSaved(saveIndex)) {
					this.deleteButtons[saveIndex].x = this.stateButtons[saveIndex].width + 2;
					this.deleteButtons[saveIndex].y = this.stateButtons[saveIndex].y;
					this.deleteButtons[saveIndex].width = 20;
					this.deleteButtons[saveIndex].height = 20;
				}
			}
		}
	}

	public void showToolTip(int mouseX, int mouseY) {
		if (!TMIConfig.isMultiplayer() && this.trashButton.contains(mouseX, mouseY)) {
			if (!Keyboard.isKeyDown(54) && !Keyboard.isKeyDown(42)) {
				if (TMIUtils.getHeldItem() == null) {
					this.canvas.drawTip(mouseX, mouseY, "Drop item here to delete");
				} else {
					this.canvas.drawTip(mouseX, mouseY, "Delete " + TMIUtils.itemDisplayName(TMIUtils.getHeldItem()));
				}
			} else if (TMIUtils.getHeldItem() == null) {
				this.canvas.drawTip(mouseX, mouseY, "Delete ALL items from current inventory screen");
			} else {
				this.canvas.drawTip(mouseX, mouseY, "Delete ALL " + TMIUtils.itemDisplayName(TMIUtils.getHeldItem()));
			}
		}
	}

	public boolean isInitialized() {
		return this.widgetsCreated;
	}
}
