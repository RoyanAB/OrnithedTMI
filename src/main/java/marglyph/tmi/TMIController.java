package marglyph.tmi;

import net.minecraft.client.gui.screen.inventory.menu.InventoryMenuScreen;
import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.inventory.menu.ChestMenu;
import net.minecraft.inventory.menu.CraftingTableMenu;
import net.minecraft.inventory.menu.PlayerMenu;
import net.minecraft.inventory.slot.InventorySlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

public class TMIController implements _tmi_MgButtonHandler, _tmi_MgItemHandler {
	private final InventoryMenuScreen window;
	private final TMIConfig config = TMIConfig.getInstance();
	private final TMIView view;
	long lastKeyPressTime = 0L;
	long lastPrefsLoadTime = 0L;
	long deleteAllWaitUntil = 0L;
	public static final long KEY_DELAY = 200L;
	public static final long PREFS_DELAY = 2000L;
	private final _tmi_MgCanvas canvas;

	public TMIController(InventoryMenuScreen windowScreen, ItemRenderer itemRenderer) {
		this.window = windowScreen;
		this.canvas = new _tmi_MgCanvas(this.window, itemRenderer);
		this.view = new TMIView(this.canvas, this.config, this);
	}

	public void onEnterFrame(int mouseX, int mouseY, int panelWidth, int panelHeight) {
		try {
			if (System.currentTimeMillis() - this.lastKeyPressTime > KEY_DELAY) {
				if (Keyboard.isKeyDown(24)) {
					this.config.toggleEnabled();
					TMIUtils.savePreferences(this.config);
					this.lastKeyPressTime = System.currentTimeMillis();
				}

				TMIUtils.updateUnlimitedItems();
			}

			if (System.currentTimeMillis() - this.lastPrefsLoadTime > PREFS_DELAY) {
				TMIUtils.loadPreferences(this.config);
				if (this.lastPrefsLoadTime == 0L) {
					TMIUtils.savePreferences(this.config);
				}

				TMIUtils.loadItems(this.config);
				this.lastPrefsLoadTime = System.currentTimeMillis();
			}

			if (this.config.isEnabled()) {
				this.canvas.windowX = (this.window.width - panelWidth) / 2;
				this.canvas.windowY = (this.window.height - panelHeight) / 2;
				this.canvas.drawText(2, 11, "TooManyItems " + TMIConfig.VERSION, 4539717);
				this.view.layout(this.window.width, this.window.height, panelWidth, panelHeight);
				this.canvas.drawWidgets(mouseX, mouseY);
				this.view.showToolTip(mouseX, mouseY);
				this.canvas.hardSetFlatMode(false);
			}
		} catch (Exception exception) {
			TMIUtils.safeReportException(exception);
			this.disable();
		}
	}

	public boolean onClick(int mouseX, int mouseY, int mouseButton) {
		try {
			if (this.config.isEnabled()) {
				this.canvas.sortByZOrder();

				for (_tmi_MgWidget widget : this.canvas.widgets) {
					if (widget.contains(mouseX, mouseY)) {
						return widget.click(mouseX, mouseY, mouseButton);
					}
				}
			}
		} catch (Exception exception) {
			TMIUtils.safeReportException(exception);
			this.disable();
		}

		return true;
	}

	@Override
	public boolean onButtonPress(Object data) {
		if (data instanceof TMIStateButtonData) {
			TMIStateButtonData stateButtonData = (TMIStateButtonData) data;
			if (stateButtonData.action == 1) {
				this.config.clearState(stateButtonData.state);
				TMIUtils.savePreferences(this.config);
			} else if (this.config.isStateSaved(stateButtonData.state)) {
				this.config.loadState(stateButtonData.state);
				TMIUtils.savePreferences(this.config);
			} else {
				this.config.saveState(stateButtonData.state);
				TMIUtils.savePreferences(this.config);
			}
		} else if (data instanceof String) {
			String buttonAction = (String) data;
			switch (buttonAction) {
				case "rain":
					TMIUtils.getMinecraft().world.getData().setRaining(!TMIUtils.getMinecraft().world.getData().isRaining());
					break;
				case "next":
					_tmi_MgItemPanel.page++;
					break;
				case "prev":
					_tmi_MgItemPanel.page--;
					break;
				case "trash":
					if (!Keyboard.isKeyDown(54) && !Keyboard.isKeyDown(42)) {
						TMIUtils.deleteHeldItem();
					} else if (TMIUtils.getHeldItem() == null) {
						if (System.currentTimeMillis() > this.deleteAllWaitUntil) {
							for (int slotIndex = 0; slotIndex < this.window.menu.slots.size(); slotIndex++) {
								InventorySlot slot = (InventorySlot) this.window.menu.slots.get(slotIndex);
								slot.setItem(null);
							}
						}
					} else {
						ItemStack heldItem = TMIUtils.getHeldItem();
						TMIUtils.deleteHeldItem();

						for (int slotIndex = 0; slotIndex < this.window.menu.slots.size(); slotIndex++) {
							InventorySlot slot = (InventorySlot) this.window.menu.slots.get(slotIndex);
							if (slot != null) {
								ItemStack itemInSlot = slot.getItem();
								if (itemInSlot != null && itemInSlot.id == heldItem.id && itemInSlot.getMetadata() == heldItem.getMetadata()) {
									slot.setItem(null);
								}
							}
						}

						this.deleteAllWaitUntil = System.currentTimeMillis() + 1000L;
					}
					break;
			}
		}

		return true;
	}

	@Override
	public boolean onItemEvent(ItemStack itemStack, int mouseButton) {
		if (mouseButton != 0) {
			if (mouseButton == 1) {
				TMIUtils.giveStack(itemStack, this.config, 1);
				return false;
			} else {
				return true;
			}
		} else if (!TMIConfig.isMultiplayer() && (Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54))) {
			Item item = Item.BY_ID[itemStack.id];
			if (TMIConfig.isTool(item)) {
				TMIUtils.giveStack(new ItemStack(itemStack.id, 1, -32000), this.config, 1);
				return false;
			} else if (TMIConfig.canItemBeUnlimited(item)) {
				TMIUtils.giveStack(itemStack, this.config, -100);
				return false;
			} else {
				TMIUtils.giveStack(itemStack, this.config);
				return false;
			}
		} else {
			TMIUtils.giveStack(itemStack, this.config);
			return false;
		}
	}


	@Deprecated
	public void onDestroy() {
	}

	public boolean isChest() {
		return this.window.menu instanceof ChestMenu;
	}

	public boolean isCrafting() {
		return this.window.menu instanceof PlayerMenu || this.window.menu instanceof CraftingTableMenu;
	}

	public void disable() {
		this.config.setEnabled(false);
	}
}
