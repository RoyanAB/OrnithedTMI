package marglyph.tmi;

import net.minecraft.item.ItemStack;

import java.util.List;

import static marglyph.tmi._tmi_MgCanvas.LIGHT_SHADE;

class _tmi_MgItemPanel extends _tmi_MgWidget {
	private final _tmi_MgItemHandler handler;
	private final List<ItemStack> items;
	public static int page = 0;
	public int numPages = 0;
	public static final int SPACING = 18;
	private ItemStack hoverItem;
	private int marginLeft;
	private int marginTop;
	private int cols;
	private int rows;
	private int itemsPerPage;

	public _tmi_MgItemPanel(int x, int y, int z, int width, int height, List<ItemStack> items, _tmi_MgItemHandler handler) {
		super(x, y, z, width, height);
		this.items = items;
		this.handler = handler;
	}

	@Override
	public void resize() {
		this.marginLeft = this.x + this.width % SPACING / 2;
		this.marginTop = this.y + this.height % SPACING / 2;
		this.cols = this.width / SPACING;
		this.rows = this.height / SPACING;
		this.itemsPerPage = this.rows * this.cols;
		this.numPages = (int) Math.ceil(1.0F * this.items.size() / this.itemsPerPage);
		page = page < 0 ? this.numPages - 1 : (page >= this.numPages ? 0 : page);
	}

	@Override
	public void draw(_tmi_MgCanvas canvas, int mouseX, int mouseY) {
		int itemIndex = page * this.itemsPerPage;
		int column = 0;
		int row = 0;
		this.hoverItem = null;

		while (itemIndex < this.itemsPerPage * (page + 1) && itemIndex < this.items.size()) {
			ItemStack item = this.items.get(itemIndex++);
			int itemX = this.marginLeft + column * SPACING;
			int itemY = this.marginTop + row * SPACING;
			if (mouseX >= itemX && mouseX < itemX + SPACING && mouseY >= itemY && mouseY < itemY + SPACING) {
				this.hoverItem = item;
				canvas.drawRect(itemX - 1, itemY - 1, SPACING, SPACING, LIGHT_SHADE);
			}

			canvas.drawItem(itemX, itemY, item);
			if (++column == this.cols) {
				column = 0;
				row++;
			}
		}

		if (this.hoverItem != null) {
			List<String> tooltip = TMIUtils.itemDisplayNameMultiline(this.hoverItem, true);
			canvas.drawMultilineTip(mouseX, mouseY, tooltip);
		}
	}

	@Override
	public boolean click(int mouseX, int mouseY, int mouseButton) {
		return this.hoverItem == null || this.handler.onItemEvent(this.hoverItem, mouseButton);
	}
}
