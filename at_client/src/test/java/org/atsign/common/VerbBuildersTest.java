package org.atsign.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.atsign.common.Keys.PublicKey;
import org.atsign.common.Keys.SelfKey;
import org.atsign.common.Keys.SharedKey;
import org.atsign.common.VerbBuilders.CRAMVerbBuilder;
import org.atsign.common.VerbBuilders.DeleteVerbBuilder;
import org.atsign.common.VerbBuilders.FromVerbBuilder;
import org.atsign.common.VerbBuilders.LlookupVerbBuilder;
import org.atsign.common.VerbBuilders.LookupVerbBuilder;
import org.atsign.common.VerbBuilders.NotificationStatusVerbBuilder;
import org.atsign.common.VerbBuilders.NotifyKeyChangeBuilder;
import org.atsign.common.VerbBuilders.NotifyTextVerbBuilder;
import org.atsign.common.VerbBuilders.PKAMVerbBuilder;
import org.atsign.common.VerbBuilders.POLVerbBuilder;
import org.atsign.common.VerbBuilders.PlookupVerbBuilder;
import org.atsign.common.VerbBuilders.ScanVerbBuilder;
import org.atsign.common.VerbBuilders.UpdateVerbBuilder;
import org.atsign.common.VerbBuilders.PlookupVerbBuilder.Type;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VerbBuildersTest {

	@Before
	public void setUp() {
	}

	@Test
	public void fromVerbBuilderTest() {
		FromVerbBuilder builder;
		String command;

		builder = new FromVerbBuilder();
		builder.setAtSign("@bob");
		command = builder.build(); // "from:@bob"
		assertEquals("from:@bob", command);
	}

	@Test
	public void cramVerbBuilderTest() {
		CRAMVerbBuilder builder;
		String command;

		builder = new CRAMVerbBuilder();
		builder.setDigest("digest");
		command = builder.build(); // "cram:digest"
		assertEquals("cram:digest", command);
	}

	@Test
	public void polVerbBuilderTest() {
		POLVerbBuilder builder;
		String command;

		builder = new POLVerbBuilder();
		command = builder.build(); // "pol"
		assertEquals("pol", command);
	}

	@Test
	public void pkamVerbBuilderTest() {
		PKAMVerbBuilder builder;
		String command;

		builder = new PKAMVerbBuilder();
		builder.setDigest("digest");
		command = builder.build(); // "pkam:digest"
		assertEquals("pkam:digest", command);
	}

	@Test
	public void updateVerbBuilderTest() {
		UpdateVerbBuilder builder;
		String command;

		// self key
		builder = new UpdateVerbBuilder();
		builder.setKeyName("test");
		builder.setSharedBy("@bob");
		builder.setValue("my Value 123");
		command = builder.build(); // "update:test@bob my Value 123"
		assertEquals("update:test@bob my Value 123", command); 

		// self key but shared with self
		builder = new UpdateVerbBuilder();
		builder.setKeyName("test");
		builder.setSharedBy("@bob");
		builder.setSharedWith("@bob");
		builder.setValue("My value 123");
		command = builder.build(); // "update:@alice:test@bob My value 123"
		assertEquals("update:@bob:test@bob My value 123", command);

		// public key
		builder = new UpdateVerbBuilder();
		builder.setKeyName("publickey");
		builder.setSharedBy("@bob");
		builder.setIsPublic(true);
		builder.setValue("my Value 123");
		command = builder.build(); // "update:public:publickey@bob my Value 123"
		assertEquals("update:public:publickey@bob my Value 123", command);

		// cached public key
		builder = new UpdateVerbBuilder();
		builder.setKeyName("publickey");
		builder.setSharedBy("@alice");
		builder.setIsPublic(true);
		builder.setIsCached(true);
		builder.setValue("my Value 123");
		command = builder.build(); // "update:cached:public:publickey@alice my Value 123"
		assertEquals("update:cached:public:publickey@alice my Value 123", command);

		// shared key
		builder = new UpdateVerbBuilder();
		builder.setKeyName("sharedkey");
		builder.setSharedBy("@bob");
		builder.setSharedWith("@alice");
		builder.setValue("my Value 123");
		command = builder.build(); // "update:@alice:sharedkey@bob my Value 123"
		assertEquals("update:@alice:sharedkey@bob my Value 123", command);

		// with shared key
		builder = new UpdateVerbBuilder();
		SharedKey sk1 = new KeyBuilders.SharedKeyBuilder(new AtSign("@bob"), new AtSign("@alice")).key("test").build();
		sk1.metadata.isBinary = true;
		sk1.metadata.ttl = 1000*60*10; // 10 minutes
		builder.with(sk1, "myBinaryValue123456");
		command = builder.build(); // update:ttl:600000:isBinary:true:isEncrypted:true:@alice:test@bob myBinaryValue123456
		assertEquals("update:ttl:600000:isBinary:true:isEncrypted:true:@alice:test@bob myBinaryValue123456", command);

		// with public key
		builder = new UpdateVerbBuilder();
		PublicKey pk1 = new KeyBuilders.PublicKeyBuilder(new AtSign("@bob")).key("test").build();
		pk1.metadata.isCached = true;
		builder.with(pk1, "myValue123");
		command = builder.build(); // update:cached:public:test@bob myValue123
		assertEquals("update:isBinary:false:isEncrypted:false:cached:public:test@bob myValue123", command);

		// with self key
		builder = new UpdateVerbBuilder();
		SelfKey sk2 = new KeyBuilders.SelfKeyBuilder(new AtSign("@bob")).key("test").build();
		sk2.metadata.ttl = 1000*60*10; // 10 minutes
		builder.with(sk2, "myValue123");
		command = builder.build(); // update:ttl:600000:test@bob myValue123
		assertEquals("update:ttl:600000:isBinary:false:isEncrypted:true:test@bob myValue123", command);

		// with self key (shared with self)
		builder = new UpdateVerbBuilder();
		AtSign bob = new AtSign("@bob");
		SelfKey sk3 = new KeyBuilders.SelfKeyBuilder(bob, bob).key("test").build();
		sk3.metadata.ttl = 1000*60*10; // 10 minutes
		builder.with(sk3, "myValue123");
		command = builder.build(); // update:ttl:600000:@bob:test@bob myValue123
		assertEquals("update:ttl:600000:isBinary:false:isEncrypted:true:@bob:test@bob myValue123", command);

		// private hidden key
		// TODO with private hidden key when implemented
	}

	@Test
	public void llookupVerbBuilderTest() {
		LlookupVerbBuilder builder;
		String command;

		// Type.NONE self key
		builder = new LlookupVerbBuilder();
		builder.setKeyName("test");
		builder.setSharedBy("@alice");
		command = builder.build(); // "llookup:test@alice"
		assertEquals("llookup:test@alice", command);

		// Type.METADATA self key
		builder = new LlookupVerbBuilder();
		builder.setKeyName("test");
		builder.setSharedBy("@alice");
		builder.setType(LlookupVerbBuilder.Type.METADATA);
		command = builder.build(); // "llookup:meta:test@alice"
		assertEquals("llookup:meta:test@alice", command);

		// hidden self key, meta
		builder = new LlookupVerbBuilder();
		builder.setKeyName("test");
		builder.setSharedBy("@alice");
		builder.setType(LlookupVerbBuilder.Type.METADATA);
		builder.setIsHidden(true);
		command = builder.build(); // "llookup:meta:_test@alice"
		assertEquals("llookup:meta:_test@alice", command);
		
		// Type.ALL public cached key
		builder = new LlookupVerbBuilder();
		builder.setKeyName("publickey");
		builder.setSharedBy("@alice");
		builder.setIsCached(true);
		builder.setIsPublic(true);
		builder.setType(LlookupVerbBuilder.Type.ALL);
		command = builder.build(); // "llookup:cached:public:publickey@alice:all"
		assertEquals("llookup:all:cached:public:publickey@alice", command);

		// no key name
		assertThrows(IllegalArgumentException.class, () -> {
			LlookupVerbBuilder b = new LlookupVerbBuilder();
			b = new LlookupVerbBuilder();
			b.setSharedBy("@alice");
			b.build();
		});
		
		// no shared by
		assertThrows(IllegalArgumentException.class, () -> {
			LlookupVerbBuilder b = new LlookupVerbBuilder();
			b = new LlookupVerbBuilder();
			b.setKeyName("test");
			b.build();
		});

		// no key name and no shared by
		assertThrows(IllegalArgumentException.class, () -> {
			LlookupVerbBuilder b = new LlookupVerbBuilder();
			b.build();
		});

		// with public key
		builder = new LlookupVerbBuilder();
		PublicKey pk = new KeyBuilders.PublicKeyBuilder(new AtSign("@bob")).key("publickey").build();
		builder.with(pk, LlookupVerbBuilder.Type.METADATA);
		command = builder.build(); // "llookup:meta:public:publickey@bob"
		assertEquals("llookup:meta:public:publickey@bob", command);

		// with shared key
		builder = new LlookupVerbBuilder();
		SharedKey sk = new KeyBuilders.SharedKeyBuilder(new AtSign("@bob"), new AtSign("@alice")).key("sharedkey").build();
		builder.with(sk, LlookupVerbBuilder.Type.NONE);
		command = builder.build(); // "llookup:@alice:sharedkey@bob"
		assertEquals("llookup:@alice:sharedkey@bob", command);

		// with self key
		builder = new LlookupVerbBuilder();
		SelfKey selfKey1 = new KeyBuilders.SelfKeyBuilder(new AtSign("@bob")).key("test").build();
		builder.with(selfKey1, LlookupVerbBuilder.Type.ALL);
		command = builder.build(); // "llookup:all:test@bob"
		assertEquals("llookup:all:test@bob", command);

		// with self key (shared with self)
		builder = new LlookupVerbBuilder();
		AtSign as = new AtSign("@bob");
		SelfKey selfKey2 = new KeyBuilders.SelfKeyBuilder(as, as).key("test").build();
		builder.with(selfKey2, LlookupVerbBuilder.Type.ALL);
		command = builder.build(); // "llookup:all:@bob:test@bob"
		assertEquals("llookup:all:@bob:test@bob", command);

		
		// with cached public key
		builder = new LlookupVerbBuilder();
		PublicKey pk2 = new KeyBuilders.PublicKeyBuilder(new AtSign("@bob")).key("publickey").build();
		pk2.metadata.isCached = true;
		builder.with(pk2, LlookupVerbBuilder.Type.ALL);
		command = builder.build(); // "llookup:all:cached:public:publickey@bob"
		assertEquals("llookup:all:cached:public:publickey@bob", command);
		
		// with cached shared key
		builder = new LlookupVerbBuilder();
		SharedKey sk2 = new KeyBuilders.SharedKeyBuilder(new AtSign("@bob"), new AtSign("@alice")).key("sharedkey").build();
		sk2.metadata.isCached = true;
		builder.with(sk2, LlookupVerbBuilder.Type.NONE);
		command = builder.build(); // "llookup:cached:@alice:sharedkey@bob"
		assertEquals("llookup:cached:@alice:sharedkey@bob", command);

		// with private hidden key 
		// TODO: not implemented yet

	}

	@Test
	public void lookupVerbBuilderTest() {
		LookupVerbBuilder builder;
		String command;

		// Type.NONE
		builder = new LookupVerbBuilder();
		builder.setKeyName("test");
		builder.setSharedWith("@alice");
		command = builder.build(); // "lookup:test@alice"
		assertEquals("lookup:test@alice", command);

		// Type.METADATA
		builder = new LookupVerbBuilder();
		builder.setKeyName("test");
		builder.setSharedWith("@alice");
		builder.setType(LookupVerbBuilder.Type.METADATA);
		command = builder.build(); // "lookup:meta:test@alice"
		assertEquals("lookup:meta:test@alice", command);

		// Type.ALL
		builder = new LookupVerbBuilder();
		builder.setKeyName("test");
		builder.setSharedWith("@alice");
		builder.setType(LookupVerbBuilder.Type.ALL);
		command = builder.build(); // "lookup:test@alice"
		assertEquals("lookup:all:test@alice", command);
		
		// no key name
		assertThrows(IllegalArgumentException.class, () -> {
			LookupVerbBuilder b = new LookupVerbBuilder();
			b = new LookupVerbBuilder();
			b.setSharedWith("@alice");
			b.build();
		});

		// no sharedWith
		assertThrows(IllegalArgumentException.class, () -> {
			LookupVerbBuilder b = new LookupVerbBuilder();
			b = new LookupVerbBuilder();
			b.setKeyName("test");
			b.build();
		});

		// no key name and no shared with
		assertThrows(IllegalArgumentException.class, () -> {
			LookupVerbBuilder b = new LookupVerbBuilder();
			b.build();
		});

		// with shared key
		builder = new LookupVerbBuilder();
		SharedKey sk = new KeyBuilders.SharedKeyBuilder(new AtSign("@sharedby"), new AtSign("@sharedwith")).key("test").build();
		builder.with(sk, LookupVerbBuilder.Type.METADATA);
		command = builder.build(); // "lookup:meta:test@sharedby"
		assertEquals("lookup:meta:test@sharedwith", command);
	}

	@Test
	public void plookupVerbBuilderTest() {
		PlookupVerbBuilder builder;
		String command;

		// Type.NONE
		builder = new PlookupVerbBuilder();
		builder.setKeyName("publickey");
		builder.setSharedBy("@alice");
		command = builder.build(); // "plookup:publickey@alice"
		assertEquals("plookup:publickey@alice", command);

		// Type.METADATA
		builder = new PlookupVerbBuilder();
		builder.setKeyName("publickey");
		builder.setSharedBy("@alice");
		builder.setType(PlookupVerbBuilder.Type.METADATA);
		command = builder.build(); // "plookup:meta:publickey@alice"
		assertEquals("plookup:meta:publickey@alice", command);

		// Type.ALL
		builder = new PlookupVerbBuilder();
		builder.setKeyName("publickey");
		builder.setSharedBy("@alice");
		builder.setType(PlookupVerbBuilder.Type.ALL);
		command = builder.build(); // "plookup:all:publickey@alice"
		assertEquals("plookup:all:publickey@alice", command);

		// no key
		assertThrows(IllegalArgumentException.class, () -> {
			PlookupVerbBuilder b = new PlookupVerbBuilder();
			b.setSharedBy("@alice");
			b.setType(Type.ALL);
			b.build();
		});

		// no shared by
		assertThrows(IllegalArgumentException.class, () -> {
			PlookupVerbBuilder b = new PlookupVerbBuilder();
			b.setKeyName("publickey");
			b.setType(Type.ALL);
			b.build();
		});

		// no key and no shared by
		assertThrows(IllegalArgumentException.class, () -> {
			PlookupVerbBuilder b = new PlookupVerbBuilder();
			b.setType(Type.ALL);
			b.build();
		});

		// with
		builder = new PlookupVerbBuilder();
		PublicKey pk = new KeyBuilders.PublicKeyBuilder(new AtSign("@bob")).key("publickey").build();
		builder.with(pk, Type.ALL);
		command = builder.build(); // "plookup:all:publickey@bob"
		assertEquals("plookup:all:publickey@bob", command);

		// bypasscache true
		builder = new PlookupVerbBuilder();
		builder.setKeyName("publickey");
		builder.setSharedBy("@alice");
		builder.setBypassCache(true);
		builder.setType(Type.ALL);
		command = builder.build(); // "plookup:bypassCache:true:all:publickey@alice"
		assertEquals("plookup:bypassCache:true:all:publickey@alice", command);
	}

	@Test
	public void deleteVerbBuilderTest() {
		DeleteVerbBuilder builder;
		String command;

		// delete a public key
		builder = new DeleteVerbBuilder();
		builder.setIsPublic(true);
		builder.setKeyName("publickey");
		builder.setSharedBy("@alice");
		command = builder.build();
		assertEquals("delete:public:publickey@alice", command);

		// delete a cached public key
		builder = new DeleteVerbBuilder();
		builder.setIsCached(true);
		builder.setIsPublic(true);
		builder.setKeyName("publickey");
		builder.setSharedBy("@bob");
		command = builder.build();
		assertEquals("delete:cached:public:publickey@bob", command);

		// delete a self key
		builder = new DeleteVerbBuilder();
		builder.setKeyName("test");
		builder.setSharedBy("@alice");
		command = builder.build();
		assertEquals("delete:test@alice", command);
		
		// delete a hidden self key
		builder = new DeleteVerbBuilder();
		builder.setIsHidden(true);
		builder.setKeyName("test");
		builder.setSharedBy("@alice");
		command = builder.build();
		assertEquals("delete:_test@alice", command);

		// delete a shared key
		builder = new DeleteVerbBuilder();
		builder.setKeyName("test");
		builder.setSharedBy("@alice");
		builder.setSharedWith("@bob");
		command = builder.build();
		assertEquals("delete:@bob:test@alice", command);

		// delete a cached shared key
		builder = new DeleteVerbBuilder();
		builder.setIsCached(true);
		builder.setKeyName("test");
		builder.setSharedBy("@alice");
		builder.setSharedWith("@bob");
		command = builder.build();
		assertEquals("delete:cached:@bob:test@alice", command);

		// missing key name
		assertThrows(IllegalArgumentException.class, () -> {
			DeleteVerbBuilder b = new DeleteVerbBuilder();
			b.setSharedBy("@alice");
			b.setSharedWith("@bob");
			b.build();
		});

		// missing shared by
		assertThrows(IllegalArgumentException.class, () -> {
			DeleteVerbBuilder b = new DeleteVerbBuilder();
			b.setKeyName("test");
			b.build();
		});

		// missing key name and shared by
		assertThrows(IllegalArgumentException.class, () -> {
			DeleteVerbBuilder b = new DeleteVerbBuilder();
			b.build();
		});

		// with self key
		builder = new DeleteVerbBuilder();
		SelfKey selfKey = new KeyBuilders.SelfKeyBuilder(new AtSign("@alice")).key("test").build();
		builder.with(selfKey);
		command = builder.build();
		assertEquals("delete:test@alice", command);

		// with public key
		builder = new DeleteVerbBuilder();
		PublicKey pk = new KeyBuilders.PublicKeyBuilder(new AtSign("@bob")).key("publickey").build();
		builder.with(pk);
		command = builder.build();

		// with shared key
		builder = new DeleteVerbBuilder();
		SharedKey sk = new KeyBuilders.SharedKeyBuilder(new AtSign("@alice"), new AtSign("@bob")).key("test").build();
		builder.with(sk);
		command = builder.build();
		assertEquals("delete:@bob:test@alice", command);

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
