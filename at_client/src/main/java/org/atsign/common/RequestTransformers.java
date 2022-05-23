package org.atsign.common;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.atsign.client.util.EncryptionUtil;
import org.atsign.common.Keys.PublicKey;
import org.atsign.common.Keys.SelfKey;
import org.atsign.common.Keys.SharedKey;

public class RequestTransformers {

	/// Transforms the data from type T to type V
	public interface RequestTransformer<T, V> {
		/**
		 * Transforms value of type T to value of type R
		 * 
		 * @param value
		 * @return
		 * @throws AtException when the transformation fails due to some reason
		 */
		V tranform(T value) throws AtException;
	}

	/**
	 * 
	 * Encrypts the data meant for a self key using 4096 bit AES key and sets right
	 * metadata attributes
	 *
	 */
	public static class SelfkeyEntryAESEncryptionTranformer
			implements RequestTransformer<AtEntry<SelfKey, String>, AtEntry<SelfKey, String>> {

		private String selfAESEncryptionKey;

		/**
		 * 4096 bit AES encryption key
		 * 
		 * @param selfKey
		 */
		public SelfkeyEntryAESEncryptionTranformer(String selfAESEncryptionKey) {
			this.selfAESEncryptionKey = selfAESEncryptionKey;
		}

		@Override
		public AtEntry<SelfKey, String> tranform(AtEntry<SelfKey, String> entry) throws AtException {

			try {
				// Encrypt value using sharedKey
				entry.setValue(EncryptionUtil.aesEncryptToBase64(entry.getValue(), selfAESEncryptionKey));
				// Set is encrypted to true
				entry.getKey().metadata.isEncrypted = true;
			} catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
					| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
					| NoSuchProviderException e) {
				throw new AtException("Tranformation failed. " + e.getMessage());

			}

			return entry;
		}
	}

	/**
	 * 
	 * Signs the public key data and sets the right metadata attributes
	 *
	 */
	public static class PublicKeyEntrySigningTranformer
			implements RequestTransformer<AtEntry<PublicKey, String>, AtEntry<PublicKey, String>> {

		private String pkamPrivateKey;

		public PublicKeyEntrySigningTranformer(String pkamPrivateKey) {
			this.pkamPrivateKey = pkamPrivateKey;
		}

		@Override
		public AtEntry<PublicKey, String> tranform(AtEntry<PublicKey, String> entry) throws AtException {

			try {
				PrivateKey privateKey = EncryptionUtil.privateKeyFromBase64(pkamPrivateKey);
				String signedPublicData = EncryptionUtil.signSHA256RSA(entry.getValue(), privateKey);
				entry.getKey().metadata.isEncrypted = false;
				entry.getKey().metadata.dataSignature = signedPublicData;
			} catch (Exception e) {
				throw new AtException("Tranformation failed. " + e.getMessage());
			}
			
			// Set checksum of the private key with what the data is signed
			return entry;
		}
	}

	/**
	 * 
	 * Encrypts the shared key with AES 4096 bit key and makes use of public key to
	 * encrypt the shared key
	 *
	 */
	public static class SharedKeyEntryAESEncryptionTranformer
			implements RequestTransformer<AtEntry<SharedKey, String>, AtEntry<SharedKey, String>> {

		private String shareToAESEncryptionKey;
		private String sharedToRSAPublicKey;

		public SharedKeyEntryAESEncryptionTranformer(String shareToAESEncryptionKey, String sharedToRSAPublicKey) {
			this.shareToAESEncryptionKey = shareToAESEncryptionKey;
			this.sharedToRSAPublicKey = sharedToRSAPublicKey;
		}

		@Override
		public AtEntry<SharedKey, String> tranform(AtEntry<SharedKey, String> entry) throws AtException {
			try {
				// Entry and set the value
				String encryptedValue = EncryptionUtil.aesEncryptToBase64(entry.getValue(), shareToAESEncryptionKey);
				// Encrypt the encryption key with RSA public key
				String encryptedshareToAESEncryptionKey = EncryptionUtil.rsaEncryptToBase64(shareToAESEncryptionKey,
						sharedToRSAPublicKey);
				entry.setValue(encryptedValue);
				// Set is encrypted to true
				entry.getKey().metadata.isEncrypted = true;
				entry.getKey().metadata.sharedKeyEnc = encryptedshareToAESEncryptionKey;
				// Set checksum of the public key with what the data is encrypted
				entry.getKey().metadata.pubKeyCS = md5CheckSum(md5CheckSum(sharedToRSAPublicKey));
			} catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
					| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
					| NoSuchProviderException | InvalidKeySpecException e) {
				throw new AtException("Tranformation failed. " + e.getMessage());
			}

			return entry;
		}

		private String md5CheckSum(String data) throws NoSuchAlgorithmException {
			MessageDigest mdigest = MessageDigest.getInstance("MD5");
			mdigest.update(data.getBytes());
			return String.valueOf(mdigest.digest());
		}
	}

}
