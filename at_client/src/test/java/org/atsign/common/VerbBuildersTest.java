package org.atsign.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.atsign.common.VerbBuilders.CRAMVerbBuilder;
import org.atsign.common.VerbBuilders.FromVerbBuilder;
import org.atsign.common.VerbBuilders.LlookupVerbBuilder;
import org.atsign.common.VerbBuilders.LookupVerbBuilder;
import org.atsign.common.VerbBuilders.NotificationStatusVerbBuilder;
import org.atsign.common.VerbBuilders.NotifyKeyChangeBuilder;
import org.atsign.common.VerbBuilders.NotifyTextVerbBuilder;
import org.atsign.common.VerbBuilders.PKAMVerbBuilder;
import org.atsign.common.VerbBuilders.PlookupVerbBuilder;
import org.atsign.common.VerbBuilders.ScanVerbBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VerbBuildersTest {

	@Before
	public void setUp() {
	}

	@Test
	public void fromVerbBuilderTest() {
		FromVerbBuilder builder = new FromVerbBuilder("@bob");
		String command = builder.build(); // "from:@bob"
		assertEquals("from:@bob", command);
	}

	@Test
	public void cramVerbBuilderTest() {
		CRAMVerbBuilder builder = new CRAMVerbBuilder("digest");
		String command = builder.build(); // "cram:digest"
		assertEquals("cram:digest", command);
	}

	@Test
	public void pkamVerbBuilderTest() {
		PKAMVerbBuilder builder = new PKAMVerbBuilder("digest");
		String command = builder.build(); // "pkam:digest"
		assertEquals("pkam:digest", command);
	}

	@Test
	public void llookupVerbBuilderTest() {
		LlookupVerbBuilder builder;
		String command;

		// Type.NONE
		builder = new LlookupVerbBuilder("test@alice");
		command = builder.build(); // "llookup:test@alice"
		assertEquals("llookup:test@alice", command);

		// Type.METADATA
		builder = new LlookupVerbBuilder("test@alice", LlookupVerbBuilder.Type.METADATA);
		command = builder.build(); // "llookup:meta:test@alice"
		assertEquals("llookup:meta:test@alice", command);

		// Type.ALL
		builder = new LlookupVerbBuilder("cached:public:publickey@alice", LlookupVerbBuilder.Type.ALL);
		command = builder.build(); // "llookup:cached:public:publickey@alice:all"
		assertEquals("llookup:all:cached:public:publickey@alice", command);
	}

	@Test
	public void lookupVerbBuilderTest() {
		LookupVerbBuilder builder;
		String command;

		// Type.NONE
		builder = new LookupVerbBuilder("test@alice");
		command = builder.build(); // "lookup:test@alice"
		assertEquals("lookup:test@alice", command);

		// Type.METADATA
		builder = new LookupVerbBuilder("test@alice", LookupVerbBuilder.Type.METADATA);
		command = builder.build(); // "lookup:meta:test@alice"
		assertEquals("lookup:meta:test@alice", command);

		// Type.ALL
		builder = new LookupVerbBuilder("cached:public:publickey@alice", LookupVerbBuilder.Type.ALL);
		command = builder.build(); // "lookup:cached:public:publickey@alice:all"
		assertEquals("lookup:all:cached:public:publickey@alice", command);
		
	}

	@Test
	public void plookupVerbBuilderTest() {
		PlookupVerbBuilder builder;
		String command;

		// Type.NONE
		builder = new PlookupVerbBuilder("test@alice");
		command = builder.build(); // "plookup:test@alice"
		assertEquals("plookup:test@alice", command);

		// Type.METADATA
		builder = new PlookupVerbBuilder("test@alice", PlookupVerbBuilder.Type.METADATA);
		command = builder.build(); // "plookup:meta:test@alice"

		// Type.ALL
		builder = new PlookupVerbBuilder("cached:public:publickey@alice", PlookupVerbBuilder.Type.ALL);
		command = builder.build(); // "plookup:cached:public:publickey@alice:all"
	}

	@Test
	public void scanVerbBuilderTest() {

		// Test not setting any parameters
		ScanVerbBuilder scanVerbBuilder = new ScanVerbBuilder();
		String command = scanVerbBuilder.build();
		assertEquals("Just scan test", "scan", command);

		// Test setting just regex
		scanVerbBuilder = new ScanVerbBuilder();
		scanVerbBuilder.setRegex("*.public");
		command = scanVerbBuilder.build();
		assertEquals("Scan with regex", "scan *.public", command);

		// Test setting just fromAtSign
		scanVerbBuilder = new ScanVerbBuilder();
		scanVerbBuilder.setFromAtSign("@other");
		command = scanVerbBuilder.build();
		assertEquals("Scan from another @sign", "scan:@other", command);

		// Test seting just showHidden
		scanVerbBuilder = new ScanVerbBuilder();
		scanVerbBuilder.setShowHidden(true);
		command = scanVerbBuilder.build();
		assertEquals("Scan with showHidden", "scan:showHidden:true", command);

		// Test setting regex & fromAtSign
		scanVerbBuilder = new ScanVerbBuilder();
		scanVerbBuilder.setRegex("*.public");
		scanVerbBuilder.setFromAtSign("@other");
		command = scanVerbBuilder.build();
		assertEquals("Scan with regex from another @sign", "scan:@other *.public", command);

		// Test setting regex & showHidden
		scanVerbBuilder = new ScanVerbBuilder();
		scanVerbBuilder.setRegex("*.public");
		scanVerbBuilder.setShowHidden(true);
		command = scanVerbBuilder.build();
		assertEquals("Scan with regex & showHidden", "scan:showHidden:true *.public", command);

		// Test setting fromAtSign & showHidden
		scanVerbBuilder = new ScanVerbBuilder();
		scanVerbBuilder.setFromAtSign("@other");
		scanVerbBuilder.setShowHidden(true);
		command = scanVerbBuilder.build();
		assertEquals("Scan with fromAtSign & showHidden", "scan:showHidden:true:@other", command);

		// Test setting regex & fromAtSign & showHidden
		scanVerbBuilder = new ScanVerbBuilder();
		scanVerbBuilder.setRegex("*.public");
		scanVerbBuilder.setFromAtSign("@other");
		scanVerbBuilder.setShowHidden(true);
		command = scanVerbBuilder.build();
		assertEquals("Scan with regex, fromAtSign & showHidden", "scan:showHidden:true:@other *.public", command);
	}

	@Test
	public void notifyTextBuilderTest() {
		// Test not setting any parameters
		assertThrows("Recipient @sign and text are mandatory. Expecting a IllegalArgumentException being thrown.",
				IllegalArgumentException.class, () -> {
					final NotifyTextVerbBuilder notifyTextBuilder = new NotifyTextVerbBuilder();
					// Expect build to throw Illegal argument exception for not setting the text
					notifyTextBuilder.build();
				});

		// Test not setting the text
		assertThrows("Text is mandatory. Expecting a IllegalArgumentException being thrown.",
				IllegalArgumentException.class, () -> {
					final NotifyTextVerbBuilder notifyTextBuilder = new NotifyTextVerbBuilder();
					notifyTextBuilder.setRecipientAtSign("@test");
					// Expect build to throw Illegal argument exception for not setting the text
					notifyTextBuilder.build();
				});

		NotifyTextVerbBuilder notifyTextBuilder = new NotifyTextVerbBuilder();
		notifyTextBuilder.setText("Hi");
		notifyTextBuilder.setRecipientAtSign("@test");
		String expectedResult = "notify:messageType:text:@test:Hi";
		assertEquals("Notify text to an @sign", expectedResult, notifyTextBuilder.build());

		// test not setting an '@' sign to the recipients at sign and expect it to be
		// appended properly
		notifyTextBuilder = new NotifyTextVerbBuilder();
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
					final NotifyKeyChangeBuilder notifyKeyChangeBuilder = new NotifyKeyChangeBuilder();
					// Expect build to throw Illegal argument exception for not setting key and
					// other mandatory parameters
					notifyKeyChangeBuilder.build();
				});

		// Test not setting the key
		assertThrows("Key is mandatory. Expecting a IllegalArgumentException being thrown.",
				IllegalArgumentException.class, () -> {
					final NotifyKeyChangeBuilder notifyKeyChangeBuilder = new NotifyKeyChangeBuilder();
					notifyKeyChangeBuilder.setOperation("update");
					notifyKeyChangeBuilder.setSenderAtSign("@sender");
					notifyKeyChangeBuilder.setRecipientAtSign("@recipient");
					// Expect build to throw Illegal argument exception for not setting the text
					notifyKeyChangeBuilder.build();
				});

		// Test not setting the key
		assertThrows("Key is mandatory. Expecting a IllegalArgumentException being thrown.",
				IllegalArgumentException.class, () -> {
					final NotifyKeyChangeBuilder notifyKeyChangeBuilder = new NotifyKeyChangeBuilder();
					notifyKeyChangeBuilder.setOperation("update");
					notifyKeyChangeBuilder.setSenderAtSign("@sender");
					notifyKeyChangeBuilder.setRecipientAtSign("@recipient");
					// Expect build to throw Illegal argument exception for not setting the text
					notifyKeyChangeBuilder.build();
				});

		// Test setting the value when ttr has been set
		assertThrows("Value is mandatory if ttr has been set. Expecting a IllegalArgumentException being thrown.",
				IllegalArgumentException.class, () -> {
					final NotifyKeyChangeBuilder notifyKeyChangeBuilder = new NotifyKeyChangeBuilder();
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
					final NotifyKeyChangeBuilder notifyKeyChangeBuilder = new NotifyKeyChangeBuilder();
					notifyKeyChangeBuilder.setOperation("update");
					notifyKeyChangeBuilder.setSenderAtSign("@sender");
					notifyKeyChangeBuilder.setRecipientAtSign("@recipient");
					notifyKeyChangeBuilder.setKey("phone");
					notifyKeyChangeBuilder.setTtr(-100);
					// Expect build to throw Illegal argument exception for not setting the text
					notifyKeyChangeBuilder.build();
				});

		// test command
		NotifyKeyChangeBuilder notifyKeyChangeBuilder = new NotifyKeyChangeBuilder();
		notifyKeyChangeBuilder.setOperation("update");
		notifyKeyChangeBuilder.setSenderAtSign("@sender");
		notifyKeyChangeBuilder.setRecipientAtSign("@recipient");
		notifyKeyChangeBuilder.setKey("phone");
		// Expect build to throw Illegal argument exception for not setting the text
		String command = notifyKeyChangeBuilder.build();
		String expectedResult = "notify:update:messageType:key:@recipient:phone@sender";
		assertEquals(expectedResult, command);

		// test command with a fully formed key
		notifyKeyChangeBuilder = new NotifyKeyChangeBuilder();
		notifyKeyChangeBuilder.setOperation("update");
		notifyKeyChangeBuilder.setKey("@recipient:phone@sender");
		// Expect build to throw Illegal argument exception for not setting the text
		command = notifyKeyChangeBuilder.build();
		expectedResult = "notify:update:messageType:key:@recipient:phone@sender";
		assertEquals(expectedResult, command);

		// test command when ttr and value are present
		notifyKeyChangeBuilder = new NotifyKeyChangeBuilder();
		notifyKeyChangeBuilder.setOperation("update");
		notifyKeyChangeBuilder.setKey("@recipient:phone@sender");
		notifyKeyChangeBuilder.setTtr(1000);
		notifyKeyChangeBuilder.setValue("cache_me");
		// Expect build to throw Illegal argument exception for not setting the text
		command = notifyKeyChangeBuilder.build();
		expectedResult = "notify:update:messageType:key:ttr:1000:@recipient:phone@sender:cache_me";
		assertEquals(expectedResult, command);

	}

	@Test
	public void notificationStatusVerbBuilderTest() {

		// Test not setting any parameters
		assertThrows("Mandatory fields are not set. Expecting a IllegalArgumentException being thrown.",
				IllegalArgumentException.class, () -> {
					final NotificationStatusVerbBuilder notificationStatusVerbBuilder = new NotificationStatusVerbBuilder();
					// Expect build to throw Illegal argument exception for not setting mandatory
					// parameters
					notificationStatusVerbBuilder.build();
				});
		
		final NotificationStatusVerbBuilder notificationStatusVerbBuilder = new NotificationStatusVerbBuilder();
		notificationStatusVerbBuilder.setNotificationId("n1234");
		String expectedResult = "notify:status:n1234";
		assertEquals(expectedResult, notificationStatusVerbBuilder.build());
		
	}

	@After
	public void tearDown() {
	}
}
