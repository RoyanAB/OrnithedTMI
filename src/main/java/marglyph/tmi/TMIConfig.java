package marglyph.tmi;

import net.minecraft.inventory.slot.InventorySlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.*;

public class TMIConfig {
	public static final String VERSION = "1.9pre5 2011-10-27";
	public static final int NUM_SAVES = 7;
	public static final int INVENTORY_SIZE = 44;
	private static TMIConfig instance;
	private static final List<ItemStack> items = new ArrayList<>();
	private static final HashSet<Integer> toolIds = new HashSet<>();
	private static final HashSet<Integer> nonUnlimitedIds;
	private final Map<String, String> settings = new LinkedHashMap<>();
	private static final ItemStack[][] states;
	private static final boolean[] statesSaved;
	private static final HashSet<Integer> excludeIds;

	public TMIConfig() {
		this.settings.put("enable", "true");
		this.settings.put("enablemp", "false");
		this.settings.put("give-command", "/give {0} {1} {2}");

		for (int saveIndex = 0; saveIndex < this.getNumSaves(); saveIndex++) {
			this.settings.put("save-name" + (saveIndex + 1), "" + (saveIndex + 1));
		}

		for (int saveIndex = 0; saveIndex < this.getNumSaves(); saveIndex++) {
			this.settings.put("save" + (saveIndex + 1), "");
		}

		this.settings.put("potions", "105");
		instance = this;
	}

	public static boolean isMultiplayer() {
		return TMIUtils.getMinecraft().world.isMultiplayer;
	}

	public static TMIConfig getInstance() {
		if (instance == null) {
			new TMIConfig();
		}

		return instance;
	}

	public Map<String, String> getSettings() {
		return this.settings;
	}

	public List<ItemStack> getItems() {
		return items;
	}

	public List<Integer> getPotionValues() {
		String potionsSetting = this.settings.get("potions");
		String[] potionValueStrings = potionsSetting.split(",");
		ArrayList<Integer> potionValues = new ArrayList<>();

		for (String potionValueString : potionValueStrings) {
			try {
				potionValues.add(Integer.valueOf(potionValueString));
			} catch (NumberFormatException ignored) {
			}
		}

		return potionValues;
	}

	public int getNumSaves() {
		return NUM_SAVES;
	}

	public boolean isStateSaved(int stateIndex) {
		return statesSaved[stateIndex];
	}

	@Deprecated
	public ItemStack[] getState(int stateIndex) {
		return states[stateIndex];
	}

	public boolean getBooleanSetting(String settingKey) {
		return Boolean.parseBoolean(this.settings.get(settingKey));
	}

	public boolean isEnabled() {
		return isMultiplayer() && this.getBooleanSetting("enablemp") || !isMultiplayer() && this.getBooleanSetting("enable");
	}

	public void toggleEnabled() {
		String enableKey = isMultiplayer() ? "enablemp" : "enable";
		this.settings.put(enableKey, Boolean.toString(!this.getBooleanSetting(enableKey)));
	}

	public void setEnabled(boolean enabled) {
		String enableKey = isMultiplayer() ? "enablemp" : "enable";
		this.settings.put(enableKey, Boolean.toString(enabled));
	}

	public static boolean isItemIncluded(int itemId) {
		return !excludeIds.contains(itemId);
	}

	public static boolean isTool(Item item) {
		return toolIds.contains(item.id);
	}

	public static boolean canItemBeUnlimited(Item item) {
		return !nonUnlimitedIds.contains(item.id);
	}

	public boolean areDamageVariantsShown() {
		if (isMultiplayer()) {
			String giveCommand = this.getSettings().get("give-command");
			return giveCommand.contains("{3}");
		} else {
			return true;
		}
	}

	public void clearState(int stateIndex) {
		for (int slotIndex = 0; slotIndex < INVENTORY_SIZE; slotIndex++) {
			states[stateIndex][slotIndex] = null;
			statesSaved[stateIndex] = false;
		}

		this.settings.put("save" + (stateIndex + 1), "");
	}

	public void loadState(int stateIndex) {
		if (statesSaved[stateIndex]) {
			List<InventorySlot> slots = TMIUtils.getMinecraft().player.playerMenu.slots;

			for (int slotIndex = 0; slotIndex < INVENTORY_SIZE; slotIndex++) {
				InventorySlot slot = slots.get(slotIndex + 1);
				slot.setItem(null);
				ItemStack savedItem = TMIUtils.copyStack(states[stateIndex][slotIndex]);
				if (savedItem != null && savedItem.id >= 0 && savedItem.id < Item.BY_ID.length && Item.BY_ID[savedItem.id] != null) {
					slot.setItem(savedItem);
				}
			}
		}
	}

	public void saveState(int stateIndex) {
		List<InventorySlot> slots = TMIUtils.getMinecraft().player.playerMenu.slots;

		for (int slotIndex = 0; slotIndex < INVENTORY_SIZE; slotIndex++) {
			states[stateIndex][slotIndex] = TMIUtils.copyStack((slots.get(slotIndex + 1)).getItem());
		}

		this.settings.put("save" + (stateIndex + 1), this.encodeState(stateIndex));
		statesSaved[stateIndex] = true;
	}

	public String encodeState(int stateIndex) {
		StringBuilder encoded = new StringBuilder();

		for (int slotIndex = 0; slotIndex < INVENTORY_SIZE; slotIndex++) {
			if (states[stateIndex][slotIndex] != null) {
				encoded.append(states[stateIndex][slotIndex].id);
				encoded.append(":");
				encoded.append(states[stateIndex][slotIndex].size);
				encoded.append(":");
				encoded.append(states[stateIndex][slotIndex].getMetadata());
			}

			encoded.append(",");
		}

		return encoded.toString();
	}

	public void decodeState(int stateIndex, String encodedState) {
		if (encodedState.trim().isEmpty()) {
			statesSaved[stateIndex] = false;
		} else {
			String[] slotEntries = encodedState.split(",", 0);

			for (int slotIndex = 0; slotIndex < slotEntries.length && slotIndex < states[stateIndex].length; slotIndex++) {
				String[] itemParts = slotEntries[slotIndex].split(":");
				if (itemParts.length == 3) {
					try {
						states[stateIndex][slotIndex] = new ItemStack(Integer.parseInt(itemParts[0]), Integer.parseInt(itemParts[1]), Integer.parseInt(itemParts[2]));
					} catch (Exception exception) {
						TooManyItemsMod.LOGGER.error(exception);
					}
				}
			}

			statesSaved[stateIndex] = true;
		}
	}

	static {
		for (int itemId = 1; itemId <= 3; itemId++) {
			toolIds.add(itemId + 256);
		}

		for (int itemId = 11; itemId <= 23; itemId++) {
			toolIds.add(itemId + 256);
		}

		for (int itemId = 27; itemId <= 30; itemId++) {
			toolIds.add(itemId + 256);
		}

		for (int itemId = 34; itemId <= 38; itemId++) {
			toolIds.add(itemId + 256);
		}

		for (int itemId = 42; itemId <= 61; itemId++) {
			toolIds.add(itemId + 256);
		}

		toolIds.add(359);
		toolIds.add(346);
		nonUnlimitedIds = new HashSet<>();
		nonUnlimitedIds.add(358);
		states = new ItemStack[NUM_SAVES][INVENTORY_SIZE];
		statesSaved = new boolean[7];
		excludeIds = new HashSet<>();
		excludeIds.add(9);
		excludeIds.add(11);
		excludeIds.add(63);
		excludeIds.add(64);
		excludeIds.add(68);
		excludeIds.add(71);
		excludeIds.add(74);
		excludeIds.add(75);
		excludeIds.add(59);
		excludeIds.add(83);
		excludeIds.add(55);
		excludeIds.add(26);
		excludeIds.add(93);
		excludeIds.add(94);
		excludeIds.add(34);
		excludeIds.add(36);
		excludeIds.add(104);
		excludeIds.add(105);
		excludeIds.add(117);
		excludeIds.add(118);
	}
}
