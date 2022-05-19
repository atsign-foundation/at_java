package org.atsign.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.atsign.common.VerbBuilders.NotifyKeyChangeBuilder;
import org.atsign.common.VerbBuilders.NotifyTextVerbBuilder;
import org.atsign.common.VerbBuilders.ScanVerbBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VerbBuildersTest {

	@Before
	public void setUp() {
	}

	@Test
	public void scanVerbBuilderTest() {

		// Test not setting any parameters
		ScanVerbBuilder scanVerbBuilder = new VerbBuilders().new ScanVerbBuilder();
		String command = scanVerbBuilder.build();
		assertEquals("Just scan test", "scan", command);

		// Test setting just regex
		scanVerbBuilder = new VerbBuilders().new ScanVerbBuilder();
		scanVerbBuilder.setRegex("*.public");
		command = scanVerbBuilder.build();
		assertEquals("Scan with regex", "scan *.public", command);

		// Test setting just fromAtSign
		scanVerbBuilder = new VerbBuilders().new ScanVerbBuilder();
		scanVerbBuilder.setFromAtSign("@other");
		command = scanVerbBuilder.build();
		assertEquals("Scan from another @sign", "scan:@other", command);

		// Test setting both regex and fromAtSign
		scanVerbBuilder = new VerbBuilders().new ScanVerbBuilder();
		scanVerbBuilder.setRegex("*.public");
		scanVerbBuilder.setFromAtSign("@other");
		command = scanVerbBuilder.build();
		assertEquals("Scan with regex from another @sign", "scan:@other *.public", command);
	}

	@Test
	public void notifyTextBuilderTest() {
		// Test not setting any parameters
		assertThrows("Recipient @sign and text are mandatory. Expecting a IllegalArgumentException being thrown.",
				IllegalArgumentException.class, () -> {
					final NotifyTextVerbBuilder notifyTextBuilder = new VerbBuilders().new NotifyTextVerbBuilder();
					// Expect build to throw Illegal argument exception for not setting the text
					notifyTextBuilder.build();
				});

		// Test not setting the text
		assertThrows("Text is mandatory. Expecting a IllegalArgumentException being thrown.",
				IllegalArgumentException.class, () -> {
					final NotifyTextVerbBuilder notifyTextBuilder = new VerbBuilders().new NotifyTextVerbBuilder();
					notifyTextBuilder.setRecipientAtSign("@test");
					// Expect build to throw Illegal argument exception for not setting the text
					notifyTextBuilder.build();
				});

		NotifyTextVerbBuilder notifyTextBuilder = new VerbBuilders().new NotifyTextVerbBuilder();
		notifyTextBuilder.setText("Hi");
		notifyTextBuilder.setRecipientAtSign("@test");
		String expectedResult = "notify:messageType:text:@test:Hi";
		assertEquals("Notify text to an @sign", expectedResult, notifyTextBuilder.build());

		// test not setting an '@' sign to the recipients at sign and expect it to be
		// appended properly
		notifyTextBuilder = new VerbBuilders().new NotifyTextVerbBuilder();
		notifyTextBuilder.setText("Hello");
		notifyTextBuilder.setRecipientAtSign("test");
		expectedResult = "notify:messageType:text:@test:Hello";
		assertEquals("Notify text to an @sign", expectedResult, notifyTextBuilder.build());

	}

	@Test
	public void notifyKeyChangeBuilderTest() {
		// Test not setting any parameters
		assertThrows("Mandatory fields are not set. Expecting a IllegalArgumentException being thrown.",
				IllegalArgumentException.class, () -> {
					final NotifyKeyChangeBuilder notifyKeyChangeBuilder = new VerbBuilders().new NotifyKeyChangeBuilder();
					// Expect build to throw Illegal argument exception for not setting key and
					// other mandatory parameters
					notifyKeyChangeBuilder.build();
				});

		// Test not setting the key
		assertThrows("Key is mandatory. Expecting a IllegalArgumentException being thrown.",
				IllegalArgumentException.class, () -> {
					final NotifyKeyChangeBuilder notifyKeyChangeBuilder = new VerbBuilders().new NotifyKeyChangeBuilder();
					notifyKeyChangeBuilder.setOperation("update");
					notifyKeyChangeBuilder.setSenderAtSign("@sender");
					notifyKeyChangeBuilder.setRecipientAtSign("@recipient");
					// Expect build to throw Illegal argument exception for not setting the text
					notifyKeyChangeBuilder.build();
				});

		// Test not setting the key
		assertThrows("Key is mandatory. Expecting a IllegalArgumentException being thrown.",
				IllegalArgumentException.class, () -> {
					final NotifyKeyChangeBuilder notifyKeyChangeBuilder = new VerbBuilders().new NotifyKeyChangeBuilder();
					notifyKeyChangeBuilder.setOperation("update");
					notifyKeyChangeBuilder.setSenderAtSign("@sender");
					notifyKeyChangeBuilder.setRecipientAtSign("@recipient");
					// Expect build to throw Illegal argument exception for not setting the text
					notifyKeyChangeBuilder.build();
				});

		// Test setting the value when ttr has been set
		assertThrows("Value is mandatory if ttr has been set. Expecting a IllegalArgumentException being thrown.",
				IllegalArgumentException.class, () -> {
					final NotifyKeyChangeBuilder notifyKeyChangeBuilder = new VerbBuilders().new NotifyKeyChangeBuilder();
					notifyKeyChangeBuilder.setOperation("update");
					notifyKeyChangeBuilder.setSenderAtSign("@sender");
					notifyKeyChangeBuilder.setRecipientAtSign("@recipient");
					notifyKeyChangeBuilder.setKey("phone");
					notifyKeyChangeBuilder.setTtr(10000);
					// Expect build to throw Illegal argument exception for not setting the text
					notifyKeyChangeBuilder.build();
				});

		// Test setting invalid ttr
		assertThrows("Value is mandatory if ttr has been set. Expecting a IllegalArgumentException being thrown.",
				IllegalArgumentException.class, () -> {
					final NotifyKeyChangeBuilder notifyKeyChangeBuilder = new VerbBuilders().new NotifyKeyChangeBuilder();
					notifyKeyChangeBuilder.setOperation("update");
					notifyKeyChangeBuilder.setSenderAtSign("@sender");
					notifyKeyChangeBuilder.setRecipientAtSign("@recipient");
					notifyKeyChangeBuilder.setKey("phone");
					notifyKeyChangeBuilder.setTtr(-100);
					// Expect build to throw Illegal argument exception for not setting the text
					notifyKeyChangeBuilder.build();
				});

		// test command
		NotifyKeyChangeBuilder notifyKeyChangeBuilder = new VerbBuilders().new NotifyKeyChangeBuilder();
		notifyKeyChangeBuilder.setOperation("update");
		notifyKeyChangeBuilder.setSenderAtSign("@sender");
		notifyKeyChangeBuilder.setRecipientAtSign("@recipient");
		notifyKeyChangeBuilder.setKey("phone");
		// Expect build to throw Illegal argument exception for not setting the text
		String command = notifyKeyChangeBuilder.build();
		String expectedResult = "notify:update:messageType:key:@recipient:phone@sender";
		assertEquals(expectedResult, command);

		// test command with a fully formed key
		notifyKeyChangeBuilder = new VerbBuilders().new NotifyKeyChangeBuilder();
		notifyKeyChangeBuilder.setOperation("update");
		notifyKeyChangeBuilder.setKey("@recipient:phone@sender");
		// Expect build to throw Illegal argument exception for not setting the text
		command = notifyKeyChangeBuilder.build();
		expectedResult = "notify:update:messageType:key:@recipient:phone@sender";
		assertEquals(expectedResult, command);

		// test command when ttr and value are present
		notifyKeyChangeBuilder = new VerbBuilders().new NotifyKeyChangeBuilder();
		notifyKeyChangeBuilder.setOperation("update");
		notifyKeyChangeBuilder.setKey("@recipient:phone@sender");
		notifyKeyChangeBuilder.setTtr(1000);
		notifyKeyChangeBuilder.setValue("cache_me");
		// Expect build to throw Illegal argument exception for not setting the text
		command = notifyKeyChangeBuilder.build();
		expectedResult = "notify:update:messageType:key:ttr:1000:@recipient:phone@sender:cache_me";
		assertEquals(expectedResult, command);

	}

	@After
	public void tearDown() {
	}
}
