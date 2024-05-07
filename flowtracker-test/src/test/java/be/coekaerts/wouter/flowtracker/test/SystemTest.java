package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedByteArray;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedCharArray;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.assertThatTracker;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.assertThatTrackerOf;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshot;
import static com.google.common.truth.Truth.assertThat;

import be.coekaerts.wouter.flowtracker.hook.StringHook;
import be.coekaerts.wouter.flowtracker.tracker.CharOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import java.util.Map;
import org.junit.Test;

/**
 * Test for {@link System}
 */
public class SystemTest {
	/** Not actually a test, but helps with debugging maven toolchain stuff */
	@Test public void printJavaVersion() {
		System.out.println("Java version: " + System.getProperty("java.version"));
	}

	@Test
	public void charArrayCopy() {
		char[] abcdef = trackedCharArray("abcdef");
		char[] defabc = new char[6];
		System.arraycopy(abcdef, 0, defabc, 3, 3); // copy abc
		System.arraycopy(abcdef, 3, defabc, 0, 3); // copy def
    assertThatTrackerOf(defabc).matches(snapshot().track(3, abcdef, 3).track(3, abcdef, 0));
		assertThat(defabc[0]).isEqualTo('d');
	}

	@Test
	public void byteArrayCopy() {
		byte[] abcdef = trackedByteArray("abcdef");
		byte[] defabc = new byte[6];
		System.arraycopy(abcdef, 0, defabc, 3, 3); // copy abc
		System.arraycopy(abcdef, 3, defabc, 0, 3); // copy def
    assertThatTrackerOf(defabc).matches(snapshot().track(3, abcdef, 3).track(3, abcdef, 0));
		assertThat(defabc[0]).isEqualTo('d');
	}

	@Test
	public void arrayCopyOntoSelf() {
		byte[] src = trackedByteArray("abcdef");
		byte[] a = new byte[src.length];
		System.arraycopy(src, 0, a, 0, src.length);
		System.arraycopy(a, 4, a, 2, 2); // copy ef
		assertThat(new String(a)).isEqualTo("abefef");
    assertThatTrackerOf(a).matches(snapshot().track(2, src, 0).track(2, src, 4).track(2, src, 4));
	}

	@Test
	public void outAndErr() {
		// IntelliJ and maven/surefire replace System.out and System.err with their own implementations,
		// and we haven't implemented anything yet to handle those, so we don't test anything tied to
		// the _current_ value of System.out and System.err.
		assertThat(TrackerTree.node("System").node("System.out").trackers())
				.isNotEmpty();
		assertThat(TrackerTree.node("System").node("System.out").node("Writer").trackers())
				.isNotEmpty();
		assertThat(TrackerTree.node("System").node("System.err").trackers())
				.isNotEmpty();
		assertThat(TrackerTree.node("System").node("System.err").node("Writer").trackers())
				.isNotEmpty();
	}

	@Test
	public void env() {
		String homeValue = System.getenv().get("HOME");
		String homeKey = getKey(System.getenv(), "HOME");
		CharOriginTracker envTracker = (CharOriginTracker)
				TrackerTree.node("System").node("env").trackers().get(0);
		testKeyValueTracking(envTracker, homeKey, homeValue);
	}

	@Test
	public void properties() {
		String homeValue = System.getProperty("user.home");
		String homeKey = getKey(System.getProperties(), "user.home");
		CharOriginTracker propertiesTracker = (CharOriginTracker)
				TrackerTree.node("System").node("properties").trackers().get(0);
		testKeyValueTracking(propertiesTracker, homeKey, homeValue);
	}

	/**
	 * Return the key in the map used that is used to represent `key`; that is the String instance
	 * that is equal to `key`.
	 */
	private static String getKey(Map<?, ?> map, String key) {
		return (String) map.keySet().stream()
				.filter(k -> k.equals(key))
				.findFirst()
				.orElseThrow();
	}

	/**
	 * Test that `originTracker` contains "key=value", and that the tracking of `key` and `value`
	 * point to that place in the originTracker.
	 */
	private static void testKeyValueTracking(CharOriginTracker originTracker,
			String key, String value) {
		String trackerContent = originTracker.getContent().toString();

		// index in trackerContent where we find key=value, surrounded by \n or at the start
		int keySourceIndex = ('\n' + trackerContent).indexOf('\n' + key + '=' + value + '\n');
		assertThat(keySourceIndex).isGreaterThan(0);
		int valueSourceIndex = keySourceIndex + key.length() + 1; // +1 for the '=' sign

		assertThatTracker(StringHook.getStringTracker(key))
				.matches(snapshot().part(key.length(), originTracker, keySourceIndex));
		assertThatTracker(StringHook.getStringTracker(value))
				.matches(snapshot().part(value.length(), originTracker, valueSourceIndex));
	}
}
