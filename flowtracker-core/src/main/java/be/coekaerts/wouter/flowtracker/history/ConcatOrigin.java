package be.coekaerts.wouter.flowtracker.history;

import java.util.Arrays;
import java.util.List;

public class ConcatOrigin implements Origin {
	private final List<String> parts;
	
	public ConcatOrigin(String origin1, String origin2) {
		this(Arrays.asList(origin1, origin2));
	}
	
	public ConcatOrigin(List<String> origins) {
		this.parts = origins;
	}
	
	public List<String> getParts() {
		return parts;
	}
}
