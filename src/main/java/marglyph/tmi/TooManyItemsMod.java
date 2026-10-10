package marglyph.tmi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("unused")
public class TooManyItemsMod {
	public static final Logger LOGGER = LogManager.getLogger("TMI");

	public String getVersion() {
		return TMIConfig.VERSION;
	}
}
