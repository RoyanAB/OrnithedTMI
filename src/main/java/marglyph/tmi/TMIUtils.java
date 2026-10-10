package marglyph.tmi;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.locale.Language;

import java.io.*;
import java.lang.reflect.Field;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class TMIUtils {
	public static final String CONFIG_FILENAME = "TooManyItems.txt";

	public static Minecraft getMinecraft() {
		return Minecraft.INSTANCE;
	}

	public static void loadPreferences(TMIConfig config) {
		try {
			Map<String, String> settings = config.getSettings();
			File configFile = new File(Minecraft.getWorkingDirectory("minecraft"), CONFIG_FILENAME);
			if (configFile.exists()) {
				BufferedReader reader = new BufferedReader(new FileReader(configFile));

				String line;
				while ((line = reader.readLine()) != null) {
					String[] keyValuePair = line.split(":", 2);
					if (keyValuePair.length > 1 && settings.containsKey(keyValuePair[0])) {
						settings.put(keyValuePair[0], keyValuePair[1]);
					}
				}

				reader.close();

				for (int saveIndex = 0; saveIndex < config.getNumSaves(); saveIndex++) {
					config.decodeState(saveIndex, settings.get("save" + (saveIndex + 1)));
				}
			}
		} catch (Exception exception) {
			TooManyItemsMod.LOGGER.error(exception);
		}
	}

	public static void savePreferences(TMIConfig config) {
		try {
			Map<String, String> settings = config.getSettings();
			File configFile = new File(Minecraft.getWorkingDirectory("minecraft"), CONFIG_FILENAME);
			PrintWriter writer = new PrintWriter(new FileWriter(configFile));

			for (String key : settings.keySet()) {
				writer.println(key + ":" + settings.get(key));
			}

			writer.close();
		} catch (Exception exception) {
			TooManyItemsMod.LOGGER.error(exception);
		}
	}

	public static void loadItems(TMIConfig config) {
		List<ItemStack> items = config.getItems();
		items.clear();

		for (Item item : Item.BY_ID) {
			if (item != null && TMIConfig.isItemIncluded(item.id)) {
				HashSet<String> seenSpriteKeys = new HashSet<>();
				int maxDamage = config.areDamageVariantsShown() ? 15 : 0;

				for (int damage = 0; damage <= maxDamage; damage++) {
					ItemStack itemStack = new ItemStack(item, item.getMaxStackSize(), damage);

					try {
						int spriteIndex = item.getSprite(itemStack);
						String spriteKey = itemStack.getUseDuration() + "@" + spriteIndex;
						if (!seenSpriteKeys.contains(spriteKey) || item.id == 18 && damage == 2) {
							items.add(itemStack);
							seenSpriteKeys.add(spriteKey);
						}
					} catch (NullPointerException | IndexOutOfBoundsException ignored) {
					}
				}
			}
		}

		if (config.areDamageVariantsShown()) {
			for (int potionValue : config.getPotionValues()) {
				ItemStack potionStack = new ItemStack(373, 64, potionValue);
				items.add(potionStack);
			}
		}
	}

	public static void safeReportException(Exception exception) {
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat(".yyyyMMdd.HHmmss");
			StringBuffer dateString = new StringBuffer();
			dateFormat.format(new Date(), dateString, new FieldPosition(1));
			String filename = "tmi" + dateString + ".txt";
			File reportFile = new File(Minecraft.getWorkingDirectory("minecraft"), filename);
			PrintWriter writer = new PrintWriter(new FileWriter(reportFile));
			writer.print("[code]TMI Version: " + TMIConfig.VERSION + "\n");
			exception.printStackTrace(writer);
			writer.println("[/code]");
			writer.close();
			getMinecraft().gui.addChatMessage("Error written to " + filename);
		} catch (Exception reportException) {
			TooManyItemsMod.LOGGER.error("Error during safeReportException:{}", String.valueOf(reportException));
		}
	}

	public static List<String> itemDisplayNameMultiline(ItemStack itemStack, boolean showIdAndDamage) {
		List<String> tooltip = null;
		if (isValidItem(itemStack)) {
			try {
				tooltip = itemStack.getTooltip();
			} catch (Exception ignored) {
			}
		}

		if (tooltip == null || tooltip.isEmpty()) {
			tooltip = new ArrayList<>();
			tooltip.add("Unnamed");
		}

		String name = (tooltip.get(0)).trim();
		if (name.isEmpty()) {
			name = "Unnamed";
			tooltip.set(0, name);
		}

		if (showIdAndDamage && itemStack != null) {
			name = name + " " + itemStack.id;
			if (itemStack.getMetadata() != 0) {
				name = name + " : " + itemStack.getMetadata();
			}

			tooltip.set(0, name);
		}

		return tooltip;
	}

	public static String itemDisplayName(ItemStack itemStack) {
		try {
			String name = Language.getInstance().translateName(Item.BY_ID[itemStack.id].getTranslationKey(itemStack)).trim();
			if (!name.isEmpty()) {
				return name;
			}
		} catch (NullPointerException ignored) {
		}

		return "Unnamed";
	}

	public static boolean isValidItem(ItemStack itemStack) {
		return itemStack == null || itemStack.id >= 0 && itemStack.id < Item.BY_ID.length && Item.BY_ID[itemStack.id] != null;
	}

	public static ItemStack getValidItem(ItemStack itemStack) {
		return isValidItem(itemStack) ? itemStack : new ItemStack(Block.FIRE);
	}

	public static String getValidItemDisplayName(ItemStack itemStack) {
		return isValidItem(itemStack) ? itemDisplayName(itemStack) : "Undefined Item";
	}

	public static void deleteHeldItem() {
		getMinecraft().player.inventory.setCursorItem(null);
	}

	public static ItemStack getHeldItem() {
		return getMinecraft().player.inventory.getCursorItem();
	}

	public static void giveStack(ItemStack itemStack, TMIConfig config) {
		giveStack(itemStack, config, itemStack.size);
	}

	public static void giveStack(ItemStack itemStack, TMIConfig config, int amount) {
		ItemStack stackToGive = copyStack(itemStack, amount);
		Minecraft minecraft = getMinecraft();
		if (TMIConfig.isMultiplayer()) {
			NumberFormat numberFormat = NumberFormat.getIntegerInstance();
			numberFormat.setGroupingUsed(false);
			MessageFormat commandFormat = new MessageFormat(config.getSettings().get("give-command"));
			commandFormat.setFormatByArgumentIndex(1, numberFormat);
			commandFormat.setFormatByArgumentIndex(2, numberFormat);
			commandFormat.setFormatByArgumentIndex(3, numberFormat);
			Object[] commandArgs = new Object[]{minecraft.player.name, stackToGive.id, stackToGive.size, stackToGive.getMetadata()};
			minecraft.player.sendChat(commandFormat.format(commandArgs));
		} else {
			minecraft.player.inventory.addItem(stackToGive);
		}
	}

	public static ItemStack copyStack(ItemStack itemStack, int amount) {
		if (itemStack == null) {
			return null;
		}

		itemStack.size += amount;
		return itemStack.split(amount);
	}

	public static ItemStack copyStack(ItemStack itemStack) {
		return itemStack == null ? null : copyStack(itemStack, itemStack.size);
	}

	public static void updateUnlimitedItems() {
		if (!TMIConfig.isMultiplayer() && TMIConfig.getInstance().isEnabled()) {
			for (ItemStack itemStack : getMinecraft().player.inventory.items) {
				if (itemStack != null) {
					if (itemStack.size < 0 || itemStack.size > 64) {
						itemStack.size = 111;
					}

					if (itemStack.getMetadata() < 0) {
						setStackDamage(itemStack, -32000);
					}
				}
			}

			ItemStack heldItem = getHeldItem();
			if (heldItem != null && heldItem.size > 64) {
				heldItem.size = -1;
			}
		}
	}

	public static void setStackDamage(ItemStack itemStack, int damage) {
		try {
			for (Field field : ItemStack.class.getDeclaredFields()) {
				if (field.getName().equals("e")) {
					field.setAccessible(true);
					field.setInt(itemStack, damage);
					break;
				}
			}
		} catch (Exception exception) {
			TooManyItemsMod.LOGGER.error(exception);
		}
	}
}
