package marglyph.tmi;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class _tmi_MgCanvas {
	public int windowX = 0;
	public int windowY = 0;
	public Screen window;
	private final ItemRenderer drawItems;
	public List<_tmi_MgWidget> widgets = new ArrayList<>();
	public static final int WHITE = -1;
	public static final int SHADE = -301989888;
	public static final int RED_SHADE = -297791480;
	public static final int LIGHT_SHADE = -296397483;
	private boolean flatMode = false;

	public _tmi_MgCanvas(Screen window, ItemRenderer itemRenderer) {
		this.window = window;
		this.drawItems = itemRenderer;
	}

	public void drawRect(int x, int y, int width, int height, int color) {
		this.flatMode(true);
		this.window.fillGradient(x - this.windowX, y - this.windowY, x + width - this.windowX, y + height - this.windowY, color, color);
	}

	public void drawText(int x, int y, String text, int color) {
		this.flatMode(true);
		this.window.textRenderer.drawWithShadow(text, x - this.windowX, y - this.windowY, color);
	}

	public void drawTextCentered(int x, int y, int width, int height, String text, int color) {
		this.drawText(x + (width - this.getTextWidth(text)) / 2, y + (height - 8) / 2, text, color);
	}

	public void drawText(int x, int y, String text) {
		this.drawText(x, y, text, WHITE);
	}

	public void drawTip(int mouseX, int mouseY, String text) {
		byte padding = 3;
		int tipX = mouseX + 12;
		int tipY = mouseY - 15;
		if (tipY < 0) {
			tipY = 0;
		}

		int tipWidth = this.getTextWidth(text) + padding * 2;
		if (tipX + tipWidth > this.window.width) {
			tipX -= tipX + tipWidth - this.window.width;
		}

		this.drawRect(tipX, tipY, tipWidth, 8 + padding * 2, SHADE);
		this.drawText(tipX + padding, tipY + padding, text, WHITE);
	}

	public void drawMultilineTip(int mouseX, int mouseY, List<String> lines) {
		if (!lines.isEmpty()) {
			byte padding = 3;
			int maxLineWidth = 0;

			for (String line : lines) {
				int lineWidth = this.getTextWidth(line);
				if (lineWidth > maxLineWidth) {
					maxLineWidth = lineWidth;
				}
			}

			int tipHeight = 8;
			if (lines.size() > 1) {
				tipHeight += 2 + (lines.size() - 1) * 10;
			}

			int tipWidth = maxLineWidth + padding * 2;
			int tipTotalHeight = tipHeight + padding * 2;
			int tipX = mouseX + 12;
			int tipY = mouseY - 15;
			if (tipY < 0) {
				tipY = 0;
			}

			if (tipX + tipWidth > this.window.width) {
				tipX = this.window.width - tipWidth;
			}

			if (mouseX >= tipX && mouseX <= tipX + tipWidth && mouseY >= tipY && mouseY <= tipY + tipTotalHeight) {
				tipY = mouseY - tipTotalHeight - 2;
				if (tipY < 0) {
					tipY = mouseY + 2;
				}
			}

			this.drawRect(tipX, tipY, tipWidth, tipTotalHeight, SHADE);
			tipX += padding;
			tipY += padding;
			boolean isFirstLine = true;

			for (String line : lines) {
				this.drawText(tipX, tipY, line);
				if (isFirstLine) {
					tipY += 2;
					isFirstLine = false;
				}

				tipY += 10;
			}
		}
	}

	public void drawItem(int x, int y, ItemStack itemStack) {
		this.hardSetFlatMode(false);

		try {
			this.drawItems
				.renderGuiItemWithEnchantmentGlint(this.window.textRenderer, this.window.minecraft.textureManager, itemStack, x - this.windowX, y - this.windowY);
		} catch (RuntimeException runtimeException) {
			this.drawItems
				.renderGuiItemWithEnchantmentGlint(
					this.window.textRenderer, this.window.minecraft.textureManager, new ItemStack(51, 1, 0), x - this.windowX, y - this.windowY
				);
		}
	}

	public void sortByZOrder() {
		this.widgets.sort(_tmi_MgWidget.getComparator());
	}

	public void drawWidgets(int mouseX, int mouseY) {
		this.sortByZOrder();

		for (_tmi_MgWidget widget : this.widgets) {
			widget.draw(this, mouseX, mouseY);
		}
	}

	public int getTextWidth(String text) {
		return this.window.textRenderer.getWidth(text);
	}

	public void flatMode(boolean enabled) {
		if (enabled && !this.flatMode) {
			GL11.glDisable(2896);
			GL11.glDisable(2929);
		} else if (!enabled && this.flatMode) {
			GL11.glEnable(2896);
			GL11.glEnable(2929);
		}

		this.flatMode = enabled;
	}

	public void hardSetFlatMode(boolean enabled) {
		if (enabled) {
			GL11.glDisable(2896);
			GL11.glDisable(2929);
		} else {
			GL11.glEnable(2896);
			GL11.glEnable(2929);
		}

		this.flatMode = enabled;
	}
}
