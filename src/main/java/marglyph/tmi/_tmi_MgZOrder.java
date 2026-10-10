package marglyph.tmi;

import java.util.Comparator;

public class _tmi_MgZOrder implements Comparator<_tmi_MgWidget> {
	public int compare(_tmi_MgWidget first, _tmi_MgWidget second) {
		return Integer.compare(first.z, second.z);
	}
}
