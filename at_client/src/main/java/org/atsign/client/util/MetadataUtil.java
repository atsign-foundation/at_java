package org.atsign.client.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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

	private long _version;

	private long _ttl;
	private long _ttb;
	private long _ttr;

	private boolean _ccd;
	private boolean _isBinary;
	private boolean _isEncrypted;

	private String _dataSignature;
	private String _sharedKeyEnc;
	private String _pubKeyCS;
	

	public MetadataUtil(String llookupResponse) {
		this._llookupResponse = llookupResponse;
		try {
			this._evaluate();
		} catch (AtException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
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

	public long getVersion() {
		return _version;
	}

	public long getTTL() {
		return _ttl;
	}

	public long getTTB() {
		return _ttb;
	}

	public long getTTR() {
		return _ttr;
	}

	public boolean isCCD() {
		return _ccd;
	}

	public boolean isBinary() {
		return _isBinary;
	}

	public boolean isEncrypted() {
		return _isEncrypted;
	}

	public String getDataSignature() {
		return _dataSignature;
	}

	public String getPubKeyCS() {
		return _pubKeyCS;
	}

	private void _evaluate() throws AtException {
		/**
		 * 	What `_llookupResponse` should look like	
		 * 
		 * 	{
		 * 		"key":"@farinataanxious:lemon@sportsunconscious",
		 * 		"data":"+blpIq6zerL3818FJsFEpw==",
		 * 		"metaData":
		 * 			{
		 * 				"createdBy":null,
		 * 				"updatedBy":null,
		 * 				"createdAt":"2022-06-19 20:35:09.962Z",
		 * 				"updatedAt":"2022-06-19 20:35:09.962Z",
		 * 				"availableAt":"2022-06-19 20:35:09.962Z",
		 * 				"expiresAt":null,
		 * 				"refreshAt":"2022-06-19 21:58:29.962Z",
		 * 				"status":"active",
		 * 				"version":0,
		 * 				"ttl":0,
		 * 				"ttb":0,
		 * 				"ttr":5000,
		 * 				"ccd":true,
		 * 				"isBinary":false,
		 * 				"isEncrypted":false,
		 * 				"dataSignature":null,
		 * 				"sharedKeyEnc":null,
		 * 				"pubKeyCS":null
		 * 			}
		 * 	}
		 **/
		String[] split1 = _llookupResponse.split("metaData\":");
		String s = split1[split1.length-1];
		String s1 = s.substring(1, s.length()-2);
		String[] s2 = s1.split(",");
		// for(String x : s2) {
		// 	System.out.println(x);
		// }
		Map<String, Object> metaDataRaw = new HashMap<String, Object>();
		for(String x : s2) {
			String[] split2 = x.split(":", 2);
			// System.out.println(split2[0] + " " + split2[1]);
			String metaDataKeyName = split2[0].replace("\"", "");
			Object metaDataValue;
			String toBeVal = split2[1];
			if(toBeVal.contains("\"")) {
				toBeVal = toBeVal.substring(1, split2[1].length()-1);
			}
			if(toBeVal.equalsIgnoreCase("null")) {
				metaDataValue = null;
			} else if(toBeVal.equalsIgnoreCase("true") || toBeVal.equalsIgnoreCase("false")) {
				metaDataValue = Boolean.parseBoolean(toBeVal);
			} else if(StringUtils.isNumeric(toBeVal)) {
				metaDataValue = Long.parseLong(toBeVal);
			} else {
				metaDataValue = toBeVal;
			}
			metaDataRaw.put(metaDataKeyName, metaDataValue);
		}
		System.out.println("============ RAW ============");
		System.out.println(_llookupResponse);
		// ============ RAW ============
		// {"key":"@farinataanxious:lemon@sportsunconscious","data":"+blpIq6zerL3818FJsFEpw==","metaData":{"createdBy":null,"updatedBy":null,"createdAt":"2022-06-19 20:35:09.962Z","updatedAt":"2022-06-19 20:35:09.962Z","availableAt":"2022-06-19 20:35:09.962Z","expiresAt":null,"refreshAt":"2022-06-19 21:58:29.962Z","status":"active","version":0,"ttl":0,"ttb":0,"ttr":5000,"ccd":true,"isBinary":false,"isEncrypted":false,"dataSignature":null,"sharedKeyEnc":null,"pubKeyCS":null}}
		System.out.println("============ PARSED ============");
		for(Map.Entry<String, Object> entry : metaDataRaw.entrySet()) {
			System.out.print(entry.getKey());
			System.out.print(" ");
			if(entry.getValue() == null) {
				System.out.print("null");
			} else {
				System.out.print(entry.getValue() + " (" + entry.getValue().getClass().getSimpleName() + ")");
			}
			System.out.println();
			switch(entry.getKey()) {
				case "createdBy":
					_createdAt = (String) entry.getValue();
					break;
				case "updatedBy":
					_updatedBy = (String) entry.getValue();
					break;
				case "createdAt":
					_updatedAt = (String) entry.getValue();
					break;
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
					_version = (long) entry.getValue();
					break;
				case "ttl":
					_ttl = (long) entry.getValue();
					break;
				case "ttb":
					_ttb = (long) entry.getValue();
					break;
				case "ttr":
					_ttr = (long) entry.getValue();
					break;
				case "ccd":
					_ccd = (boolean) entry.getValue();
					break;
				case "isBinary":
					_isBinary = (boolean) entry.getValue();
					break;
				case "isEncrypted": 
					_isEncrypted = (boolean) entry.getValue();
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
}
