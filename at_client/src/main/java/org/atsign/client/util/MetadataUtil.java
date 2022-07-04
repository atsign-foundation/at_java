package org.atsign.client.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.atsign.client.api.Secondary;
import org.atsign.common.AtException;

public class MetadataUtil {

	/**
	 * SENT: llookup:all:@farinataanxious:lemon@sportsunconscious
	 * RCVD: @sportsunconscious@data:{"key":"@farinataanxious:lemon@sportsunconscious","data":"+blpIq6zerL3818FJsFEpw==","metaData":{"createdBy":null,"updatedBy":null,"createdAt":"2022-06-19
	 * 20:35:09.962Z","updatedAt":"2022-06-19
	 * 20:35:09.962Z","availableAt":"2022-06-19
	 * 20:35:09.962Z","expiresAt":null,"refreshAt":"2022-06-19
	 * 21:58:29.962Z","status":"active","version":0,"ttl":0,"ttb":0,"ttr":5000,"ccd":true,"isBinary":false,"isEncrypted":false,"dataSignature":null,"sharedKeyEnc":null,"pubKeyCS":null}}
	 */

	// fullKeyName == atKey.toString();

	private String _llookupResponse;

	// METADATA values that this class evaluates
	private String _createdBy;
	private String _updatedBy;

	private String _createdAt;
	private String _updatedAt;

	private String _availableAt;
	private String _expiresAt;
	private String _refreshAt;

	private String _status;

	private Long _version;

	private Long _ttl;
	private Long _ttb;
	private Long _ttr;

	private Boolean _ccd;
	private Boolean _isBinary;
	private Boolean _isEncrypted;

	private String _dataSignature;
	private String _sharedKeyEnc;
	private String _pubKeyCS;
	

	public MetadataUtil(Secondary.Response llookupMetaResponse) {
		// llookupMetaResponse should be the response from a verb command like `llookup:meta:<keyName>`
		this(llookupMetaResponse.data);
	}

	public MetadataUtil(String llookupMetaResponseString) {
		try {
			this._llookupResponse = llookupMetaResponseString;
			this._evaluate();
		} catch (AtException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
	}

	public String getRawLlookupMetaString() {
		return _llookupResponse;
	}

	public String getCreatedBy() {
		return _createdBy;
	}

	public String getUpdatedBy() {
		return _updatedBy;
	}

	public String getCreatedAt() {
		return _createdAt;
	}

	public String getUpdatedAt() {
		return _updatedAt;
	}

	public String getAvailableAt() {
		return _availableAt;
	}

	public String getExpiresAt() {
		return _expiresAt;
	}

	public String getRefreshAt() {
		return _refreshAt;
	}

	public String getStatus() {
		return _status;
	}

	public Long getVersion() {
		return _version;
	}

	public Long getTTL() {
		return _ttl;
	}

	public Long getTTB() {
		return _ttb;
	}

	public Long getTTR() {
		return _ttr;
	}

	public Boolean isCCD() {
		return _ccd;
	}

	public Boolean isBinary() {
		return _isBinary;
	}
	public Boolean isEncrypted() {
		return _isEncrypted;
	}

	public String getDataSignature() {
		return _dataSignature;
	}

	public String getSharedKeyEnc() {
		return _sharedKeyEnc;
	}

	public String getPubKeyCS() {
		return _pubKeyCS;
	}

	private void _evaluate() throws AtException {
		Map<String, Object> metadataRaw = _getMetadataRaw(this._llookupResponse);
		// System.out.println("============ RAW ============");
		// System.out.println(_llookupResponse);
		// ============ RAW ============
		// {"key":"@farinataanxious:lemon@sportsunconscious","data":"+blpIq6zerL3818FJsFEpw==","metaData":{"createdBy":null,"updatedBy":null,"createdAt":"2022-06-19 20:35:09.962Z","updatedAt":"2022-06-19 20:35:09.962Z","availableAt":"2022-06-19 20:35:09.962Z","expiresAt":null,"refreshAt":"2022-06-19 21:58:29.962Z","status":"active","version":0,"ttl":0,"ttb":0,"ttr":5000,"ccd":true,"isBinary":false,"isEncrypted":false,"dataSignature":null,"sharedKeyEnc":null,"pubKeyCS":null}}
		// System.out.println("============ PARSED ============");
		for(Map.Entry<String, Object> entry : metadataRaw.entrySet()) {
			// System.out.print(entry.getKey());
			// System.out.print(" ");
			// if(entry.getValue() == null) {
			// 	System.out.print("null");
			// } else {
			// 	System.out.print(entry.getValue() + " (" + entry.getValue().getClass().getSimpleName() + ")");
			// }
			// System.out.println();
			switch(entry.getKey()) {
				case "createdBy":
					_createdBy = (String) entry.getValue();
					break;
				case "updatedBy":
					_updatedBy = (String) entry.getValue();
					break;
				case "createdAt":
					_createdAt = (String) entry.getValue();
					break;
				case "updatedAt":
					_updatedAt = (String) entry.getValue();
				case "availableAt":
					_availableAt = (String) entry.getValue();
					break;
				case "expiresAt":
					_expiresAt = (String) entry.getValue();
					break;
				case "refreshAt":
					_refreshAt = (String) entry.getValue();
					break;
				case "status":
					_status = (String) entry.getValue();
					break;
				case "version":
					_version = (Long) entry.getValue();
					break;
				case "ttl":
					_ttl = (Long) entry.getValue();
					break;
				case "ttb":
					_ttb = (Long) entry.getValue();
					break;
				case "ttr":
					_ttr = (Long) entry.getValue();
					break;
				case "ccd":
					_ccd = (Boolean) entry.getValue();
					break;
				case "isBinary":
					_isBinary = (Boolean) entry.getValue();
					break;
				case "isEncrypted": 
					_isEncrypted = (Boolean) entry.getValue();
					break;
				case "dataSignature":
					_dataSignature = (String) entry.getValue();
					break;
				case "sharedKeyEnc":
					_sharedKeyEnc = (String) entry.getValue();
					break;
				case "pubKeyCS":
					_pubKeyCS = (String) entry.getValue();
					break;
				default:
					System.out.println("Missed metadata key: " + entry.getKey() + " with value: " + entry.getValue());
			}
		}
		/*
		 * ============ PARSED ============
		 * createdBy null
		 * updatedBy null
		 * createdAt 2022-06-19 20:35:09.962Z (String)
		 * updatedAt 2022-06-19 20:35:09.962Z (String)
		 * availableAt 2022-06-19 20:35:09.962Z (String)
		 * expiresAt null
		 * refreshAt 2022-06-19 21:58:29.962Z (String)
		 * status active (String)
		 * version 0 (Long)
		 * ttl 0 (Long)
		 * ttb 0 (Long)
		 * ttr 5000 (Long)
		 * ccd true (Boolean)
		 * isBinary false (Boolean)
		 * isEncrypted false (Boolean)
		 * dataSignature null
		 * sharedKeyEnc null
		 * pubKeyCS null
		 */
		
	}

	private Map<String, Object> _getMetadataRaw(String rawLlookupMetaString) {
		/**
		 * 	What `rawLlookupMetaString` should look like	
		 * 
		 * 	{
		 * 		"createdBy":null,
		 * 		"updatedBy":null,
		 * 		"createdAt":"2022-06-19 20:35:09.962Z",
		 * 		"updatedAt":"2022-06-19 20:35:09.962Z",
		 * 		"availableAt":"2022-06-19 20:35:09.962Z",
		 * 		"expiresAt":null,
		 * 		"refreshAt":"2022-06-19 21:58:29.962Z",
		 * 		"status":"active",
		 * 		"version":0,
		 * 		"ttl":0,
		 * 		"ttb":0,
		 * 		"ttr":5000,
		 * 		"ccd":true,
		 * 		"isBinary":false,
		 * 		"isEncrypted":false,
		 * 		"dataSignature":null,
		 * 		"sharedKeyEnc":null,
		 * 		"pubKeyCS":null
		 * 	}
		 **/
		String s1 = rawLlookupMetaString.substring(1, rawLlookupMetaString.length()-1); // remove {} at ends
		String[] s2 = s1.split(",");
		Map<String, Object> metadataRaw = new HashMap<String, Object>();
		for(String x : s2) {
			String[] split2 = x.split(":", 2);
			String metadataKeyName = split2[0].replace("\"", "");
			Object metadataValue;
			String toBeVal = split2[1];
			if(toBeVal.contains("\"")) { // remove quotes at ends
				toBeVal = toBeVal.substring(1, split2[1].length()-1);
			}
			if(toBeVal.equalsIgnoreCase("null")) { // metaDataValue is undeclared at first, so should set to null
				metadataValue = null;
			} else if(toBeVal.equalsIgnoreCase("true") || toBeVal.equalsIgnoreCase("false")) {
				metadataValue = Boolean.parseBoolean(toBeVal);
			} else if(StringUtils.isNumeric(toBeVal)) {
				metadataValue = Long.parseLong(toBeVal);
			} else {
				metadataValue = toBeVal;
			}
			metadataRaw.put(metadataKeyName, metadataValue);
		}
		return metadataRaw;
	}
}
