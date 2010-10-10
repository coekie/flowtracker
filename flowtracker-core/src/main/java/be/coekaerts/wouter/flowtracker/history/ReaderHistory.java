package be.coekaerts.wouter.flowtracker.history;

import java.io.Reader;

public class ReaderHistory extends History {
	private static final HistoryKeeper<Reader, ReaderHistory> KEEPER =
		new HistoryKeeper<Reader, ReaderHistory>(null) {
		protected ReaderHistory doCreateHistory(Origin origin) {
			return new ReaderHistory(origin);
		};
	};
	
	public static ReaderHistory getHistory(Reader reader) {
		return KEEPER.getHistory(reader);
	}
	
	public static ReaderHistory createHistory(Reader reader, Origin origin) {
		return KEEPER.createHistory(reader, origin);
	}
	
	private StringBuilder readContent = new StringBuilder();
	
	private ReaderHistory(Origin origin) {
		super(origin);
	}
	
	public void addRead(char c) {
		readContent.append(c);
	}

	public void addRead(char[] cbuf, int offset, int len) {
		readContent.append(cbuf, offset, len);
	}
	
	public CharSequence getReadContent() {
		return readContent;
	}
}
