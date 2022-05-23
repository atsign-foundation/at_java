package org.atsign.common;

import static org.junit.Assert.assertEquals;

import org.atsign.client.util.EncryptionUtil;
import org.atsign.common.KeyBuilders.SelfKeyBuilder;
import org.atsign.common.KeyBuilders.SharedKeyBuilder;
import org.atsign.common.Keys.SelfKey;
import org.atsign.common.Keys.SharedKey;
import org.atsign.common.RequestTransformers.SelfkeyEntryAESEncryptionTranformer;
import org.atsign.common.RequestTransformers.SharedKeyEntryAESEncryptionTranformer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RequestTransformersTest {

	@Before
	public void setUp() {
	}

	@Test
	public void SelfkeyEntryAESEncryptionTranformerTest() {

		try {

			SelfKeyBuilder builder = new SelfKeyBuilder(new AtSign("@self"));
			builder.key("phone");
			builder.namespace("test");
			SelfKey key = builder.build();

			AtEntry<SelfKey, String> entry = new AtEntry<SelfKey, String>(key, "12345");

			SelfkeyEntryAESEncryptionTranformer transformer = new RequestTransformers.SelfkeyEntryAESEncryptionTranformer(
					EncryptionUtil.generateAESKeyBase64());
			transformer.tranform(entry);
			assertEquals(true, entry.getKey().metadata.isEncrypted);

		} catch (Exception e) {
			Assert.fail("Exception " + e);
		}
	}

	@Test
	public void SharedkeyEntryAESEncryptionTranformerTest() {

		try {

			SharedKeyBuilder builder = new SharedKeyBuilder(new AtSign("@self"), new AtSign("@other"));
			builder.key("phone");
			builder.namespace("test");
			SharedKey key = builder.build();

			AtEntry<SharedKey, String> entry = new AtEntry<SharedKey, String>(key, "12345");
			String aesKey = EncryptionUtil.generateAESKeyBase64();
			String publicKey = EncryptionUtil.generateRSAKeyPair().getPublic().toString();
			SharedKeyEntryAESEncryptionTranformer transformer = new RequestTransformers.SharedKeyEntryAESEncryptionTranformer(
					aesKey, publicKey);
			transformer.tranform(entry);
			assertEquals(true, entry.getKey().metadata.isEncrypted);
			System.out.println(aesKey);
			System.out.println(publicKey);
			System.out.println(entry.getKey().metadata.sharedKeyEnc);
			System.out.println(entry.getKey().metadata.pubKeyCS);
			System.out.println(entry.getValue());

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception " + e);
		}
	}

	@After
	public void tearDown() {
	}

}
