package marglyph.tmi;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.menu.InventoryMenu;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TMICompatibility {
	private static boolean prevZanEnabled = false;

	@Deprecated
	public static void disableCompetingMods() {
		prevZanEnabled = setZanMinimapEnabled(false);
	}

	@Deprecated
	public static void restoreCompetingMods() {
		setZanMinimapEnabled(prevZanEnabled);
	}

	public static boolean setZanMinimapEnabled(boolean enabled) {
		try {
			boolean previousEnabled = true;
			Class<?> zanMinimapClass = Class.forName("mod_ZanMinimap");
			Field instanceField = zanMinimapClass.getField("instance");
			Object zanMinimapInstance = instanceField.get(null);
			Field[] declaredFields = zanMinimapClass.getDeclaredFields();

			for (Field field : declaredFields) {
				if (field.getName().equals("hide")) {
					field.setAccessible(true);
					previousEnabled = !field.getBoolean(zanMinimapInstance);
					field.setBoolean(zanMinimapInstance, !enabled);
				}
			}

			return previousEnabled;
		} catch (ClassNotFoundException ignored) {
		} catch (IllegalAccessException illegalAccessException) {
			System.out.println("IllegalAccessException in setZanMinimapEnabled.");
		} catch (NoSuchFieldException noSuchFieldException) {
			System.out.println("NoSuchFieldException in setZanMinimapEnabled.");
		}

		return true;
	}

	public static boolean callConvenientInventoryHandler(int slotIndex, int mouseButton, boolean shiftHeld, Minecraft minecraft, InventoryMenu inventoryMenu) {
		try {
			Class<?> convenientInventoryClass = Class.forName("ConvenientInventory");
			Class<?>[] parameterTypes = new Class[]{int.class, int.class, boolean.class, Minecraft.class, InventoryMenu.class};
			Method method = convenientInventoryClass.getMethod("mod_convenientInventory_handleClickOnSlot", parameterTypes);
			Object[] arguments = new Object[]{slotIndex, mouseButton, shiftHeld, minecraft, inventoryMenu};
			method.invoke(null, arguments);
			return true;
		} catch (ClassNotFoundException | NoSuchMethodException classNotFoundException) {
			return false;
		} catch (InvocationTargetException invocationTargetException) {
			System.out.println("callConvenientInventoryHandler: " + invocationTargetException.getCause());
			return false;
		} catch (IllegalAccessException illegalAccessException) {
			System.out.println("callConvenientInventoryHandler: " + illegalAccessException);
			return false;
		}
	}
}
