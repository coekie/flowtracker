package be.coekaerts.wouter.flowtracker.test;

import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;

import be.coekaerts.wouter.flowtracker.history.ConcatOrigin;
import be.coekaerts.wouter.flowtracker.history.StringHistory;
import be.coekaerts.wouter.flowtracker.history.StringOrigin;
import be.coekaerts.wouter.flowtracker.history.SubstringOrigin;

public class StringTest {
	@Test
	public void unkownTest() {
		String a = "unkownTest";
		Assert.assertSame(StringHistory.UNKNOWN, StringHistory.getHistory(a));
	}
	
	@Test
	public void concatTest() {
		String a = "concatTest1";
		String b = "concatTest2";
		String ab = a.concat(b);
		Assert.assertEquals("concatTest1concatTest2", ab);
		
		ConcatOrigin concatOrigin = getOrigin(ab, ConcatOrigin.class);
		Assert.assertEquals(Arrays.asList(a, b), concatOrigin.getParts());
	}
	
	@Test
	public void concatFirstEmpty() {
		String empty = "";
		String foo = "foo";
		String result = empty.concat(foo);
		Assert.assertEquals("foo", result);
		Assert.assertNotSame(result, foo);
		
		ConcatOrigin concatOrigin = getOrigin(result, ConcatOrigin.class);
		Assert.assertEquals(Arrays.asList(empty, foo), concatOrigin.getParts());
	}
	
	@Test
	public void concatSecondEmpty() {
		String foo = "foo";
		String empty = "";
		String result = foo.concat(empty);
		Assert.assertEquals("foo", result);
		Assert.assertNotSame(result, foo);
		
		ConcatOrigin concatOrigin = getOrigin(result, ConcatOrigin.class);
		Assert.assertEquals(Arrays.asList(foo, empty), concatOrigin.getParts());
	}
	
	
	@Test
	public void substringBeginTest()  {
		String foobar = "foobar";
		String foo = foobar.substring(0, 3);
		Assert.assertEquals("foo", foo);
		
		SubstringOrigin fooOrigin = getOrigin(foo, SubstringOrigin.class);
		Assert.assertEquals(foobar, fooOrigin.getSource());
		Assert.assertEquals(0, fooOrigin.getBeginIndex());
		Assert.assertEquals(3, fooOrigin.getEndIndex());
	}
	
	@Test
	public void substringEndTest()  {
		String foobar = "foobar";
		String bar = foobar.substring(3);
		Assert.assertEquals("bar", bar);
		
		SubstringOrigin barOrigin = getOrigin(bar, SubstringOrigin.class);
		Assert.assertEquals(foobar, barOrigin.getSource());
		Assert.assertEquals(3, barOrigin.getBeginIndex());
		Assert.assertEquals(6, barOrigin.getEndIndex());
	}
	
	private <T extends StringOrigin> T getOrigin(String o, Class<? extends T> expectedClazz) {
		StringOrigin origin = StringHistory.getHistory(o).getOrigin();
		Assert.assertNotNull(origin);
		Assert.assertTrue(expectedClazz.isInstance(origin));
		return expectedClazz.cast(origin);
	}
}
