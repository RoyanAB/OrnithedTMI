package marglyph.tmi.mixin;

import marglyph.tmi.TMICompatibility;
import marglyph.tmi.TMIConfig;
import marglyph.tmi.TMIController;
import marglyph.tmi.TMIUtils;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.menu.InventoryMenuScreen;
import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.entity.mob.player.PlayerInventory;
import net.minecraft.inventory.menu.InventoryMenu;
import net.minecraft.inventory.slot.InventorySlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;


@Mixin(InventoryMenuScreen.class)
public abstract class InventoryMenuScreenMixin extends Screen {

	@Shadow
	protected static ItemRenderer itemRenderer;

	@Shadow
	protected abstract void clickSlot(InventorySlot actualSlot, int slot, int button, boolean quickMove);

	@Shadow
	public InventoryMenu menu;
	@Shadow
	protected int x;
	@Shadow
	protected int y;
	@Shadow
	protected int backgroundWidth;
	@Shadow
	protected int backgroundHeight;

	@Shadow
	protected abstract InventorySlot getSlot(int x, int y);

	@Unique
	private TMIController tmi;

	@Inject(method = "init", at = @At("TAIL"))
	private void initTMI(CallbackInfo ci) {
		this.tmi = new TMIController((InventoryMenuScreen) (Object) this, itemRenderer);
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void onRender(int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
		this.x = (this.width - this.backgroundWidth) / 2;
		this.y = (this.height - this.backgroundHeight) / 2;
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V", ordinal = 1, shift = At.Shift.AFTER))
	private void afterGlColor4f(int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
		this.tmi.onEnterFrame(mouseX, mouseY, this.backgroundWidth, this.backgroundHeight);
	}

	@ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/player/PlayerInventory;getCursorItem()Lnet/minecraft/item/ItemStack;", ordinal = 0))
	private ItemStack onlyFlyIfAllowed(ItemStack original) {
		return null;
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDisable(I)V", ordinal = 2))
	private void beforeGlDisable(int mouseX, int mouseY, float tickDelta, CallbackInfo ci, @Local(name = "playerInventory") PlayerInventory playerInventory) {
		if (playerInventory.getCursorItem() != null) {
			ItemStack cursorItem = TMIUtils.getValidItem(playerInventory.getCursorItem());
			GL11.glTranslatef(0.0F, 0.0F, 32.0F);
			this.drawOffset = 200.0F;
			itemRenderer.zOffset = 200.0F;
			itemRenderer.renderGuiItemWithEnchantmentGlint(this.textRenderer, this.minecraft.textureManager, cursorItem, mouseX - this.x - 8, mouseY - this.y - 8);
			itemRenderer.renderGuiItemDecoration(this.textRenderer, this.minecraft.textureManager, cursorItem, mouseX - this.x - 8, mouseY - this.y - 8);
			this.drawOffset = 0.0F;
			itemRenderer.zOffset = 0.0F;
		}
	}

	@ModifyVariable(method = "render", at = @At("STORE"), name = "list")
	private List injected(List list, @Local(name = "itemStack") ItemStack itemStack) {
		return TMIUtils.itemDisplayNameMultiline(itemStack, TMIConfig.getInstance().isEnabled());
	}

	/**
	 * @author AB
	 * @reason To make tmi work normal
	 */
	@Overwrite
	public void mouseClicked(int mouseX, int mouseY, int button) {
		super.mouseClicked(mouseX, mouseY, button);
		if (button >= 0 && button <= 2) {
			InventorySlot slot = this.getSlot(mouseX, mouseY);
			int guiX = this.x;
			int guiY = this.y;
			boolean clickedOutside = mouseX < guiX || mouseY < guiY || mouseX >= guiX + this.backgroundWidth || mouseY >= guiY + this.backgroundHeight;
			this.replacementClickHandler(mouseX, mouseY, button, !clickedOutside, slot, this.minecraft, this.menu);
		}
	}

	@Unique
	public void replacementClickHandler(int mouseX, int mouseY, int button, boolean clickedInside, InventorySlot slot, Minecraft minecraft, InventoryMenu inventoryMenu) {
		if (this.tmi.onClick(mouseX, mouseY, button)) {
			int slotIndex = -1;
			if (slot != null) {
				slotIndex = slot.index;
			} else if (!clickedInside) {
				slotIndex = -999;
				ItemStack heldItem = TMIUtils.getHeldItem();
				if (heldItem != null && (heldItem.size < 0 || heldItem.size > 64)) {
					heldItem.size = 1;
				}
			}

			boolean quickMove = clickedInside && (Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54));
			if (!TMICompatibility.callConvenientInventoryHandler(slotIndex, button, quickMove, minecraft, inventoryMenu)) {
				if (slotIndex == 0 && button == 1 && this.tmi.isCrafting()) {
					for (int iteration = 0; iteration < 64; iteration++) {
						this.clickSlot(slot, slotIndex, button, quickMove);
					}
				} else if (slotIndex != -1) {
					if (this.tmi.isChest()
						&& slotIndex >= 0
						&& !TMIConfig.isMultiplayer()
						&& (Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54))
						&& TMIUtils.getHeldItem() != null
						&& TMIUtils.isValidItem(TMIUtils.getHeldItem())) {
						try {
							this.fastTransfer(slotIndex, button);
						} catch (Exception exception) {
							TMIUtils.safeReportException(exception);
						}
					} else {
						this.clickSlot(slot, slotIndex, button, quickMove);
					}
				}
			}
		}
	}

	@Unique
	private void fastTransfer(int clickedSlotIndex, int button) {
		ItemStack heldItem = TMIUtils.getHeldItem();
		this.minecraft.interactionManager.clickSlot(this.menu.networkId, clickedSlotIndex, button, false, this.minecraft.player);
		int chestSlotCount = this.menu.items.size() - 36;
		boolean clickedInChest = clickedSlotIndex < chestSlotCount;
		int scanIndex = 0;
		int scanEnd = chestSlotCount;
		int targetIndex = chestSlotCount - 1;
		int targetEnd = this.menu.items.size();
		if (clickedInChest) {
			scanIndex = chestSlotCount;
			scanEnd = this.menu.items.size();
			targetIndex = -1;
			targetEnd = chestSlotCount;
		}

		boolean useClickedSlot = true;

		while (scanIndex < scanEnd && targetIndex < targetEnd) {
			InventorySlot sourceSlot = (InventorySlot) this.menu.slots.get(scanIndex);
			if (sourceSlot != null) {
				ItemStack sourceItem = sourceSlot.getItem();
				if (sourceItem != null && sourceItem.id == heldItem.id && sourceItem.getMetadata() == heldItem.getMetadata()) {
					InventorySlot targetSlot;
					if (useClickedSlot) {
						targetSlot = (InventorySlot) this.menu.slots.get(clickedSlotIndex);
					} else {
						targetSlot = (InventorySlot) this.menu.slots.get(targetIndex);
					}

					if (targetSlot == null) {
						targetIndex++;
						useClickedSlot = false;
						continue;
					}

					ItemStack targetItem = targetSlot.getItem();
					if (targetItem == null) {
						targetSlot.setItem(sourceItem);
						sourceSlot.setItem(null);
					} else {
						if (targetItem.id != sourceItem.id || targetItem.getMetadata() != sourceItem.getMetadata()) {
							targetIndex++;
							useClickedSlot = false;
							continue;
						}

						int combinedSize = sourceItem.size + targetItem.size;
						int maxStackSize = Item.BY_ID[sourceItem.id].getMaxStackSize();
						int overflow = combinedSize - maxStackSize;
						if (overflow > 0) {
							targetItem.size = maxStackSize;
							sourceItem.size = overflow;
							targetIndex++;
							useClickedSlot = false;
							continue;
						}

						targetItem.size = combinedSize;
						sourceSlot.setItem(null);
					}
				}
			}

			scanIndex++;
		}
	}
}
