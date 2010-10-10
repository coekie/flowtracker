package be.coekaerts.wouter.flowtracker.history;

public class StringHistory extends History {
	public static final StringHistory UNKNOWN = new StringHistory(null);
	
	private static final HistoryKeeper<String, StringHistory> KEEPER =
		new HistoryKeeper<String, StringHistory>(UNKNOWN) {
		protected StringHistory doCreateHistory(Origin origin) {
			return new StringHistory(origin);
		};
	};
	
	public static StringHistory getHistory(String str) {
		return KEEPER.getHistory(str);
	}
	
	public static StringHistory createHistory(String str, Origin origin) {
		return KEEPER.createHistory(str, origin);
	}
	
	private StringHistory(Origin origin) {
		super(origin);
	}
}
