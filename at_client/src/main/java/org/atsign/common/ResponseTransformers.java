package org.atsign.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.atsign.client.api.Secondary.Response;
import org.atsign.common.response_models.LlookupAllResponse;
import org.atsign.common.response_models.NotifyListResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponseTransformers {

	/// Transforms the data from type T to type V
	public interface ResponseTransformer<T, V> {
		V transform(T value);
	}

	public static class ScanResponseTransformer implements ResponseTransformer<Response, List<String>> {

		@Override
		public List<String> transform(Response value) {

			if (value.data == null || value.data.isEmpty()) {
				return null;
			}

			ObjectMapper mapper = new ObjectMapper();
			try {
				return mapper.readerForListOf(String.class).readValue(value.data);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

	}

	public static class LlookupAllResponseTransformer implements ResponseTransformer<Response, LlookupAllResponse> {
		@Override
		public LlookupAllResponse transform(Response value) {
			if (value.data == null || value.data.isEmpty()) {
				return null;
			}

			LlookupAllResponse model = null;
			ObjectMapper mapper = new ObjectMapper();
			try {
				model = mapper.readValue(value.data, LlookupAllResponse.class);
			} catch (JsonProcessingException e) {
				System.err.println(e.toString());
				e.printStackTrace();
			}
			return model;
		}
	}

	public static class LlookupMetadataResponseTransformer implements ResponseTransformer<Response, Map<String, Object>> {
		@Override
		public Map<String, Object> transform(Response value) {
			if (value.data == null || value.data.isEmpty()) {
				return null;
			}
			ObjectMapper mapper = new ObjectMapper();
			try {
				return mapper.readerFor(Map.class).readValue(value.data);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	public static class NotifyListResponseTransformer implements ResponseTransformer<Response, NotifyListResponse> {
		/*
		 * [
		 * {
		 *   "id": "8797feda-d2d6-432a-9b9d-cc23184f60eb",
		 *   "from": "@fascinatingsnow",
		 *   "to": "@smoothalligator",
		 *   "key": "@smoothalligator:test@fascinatingsnow",
		 *   "value": null,
		 *   "operation": "update",
		 *   "epochMillis": 1665514962777,
		 *   "messageType": "MessageType.key",
		 *   "isEncrypted": false
		 * }
		 * ]
		 */
		@Override
		public NotifyListResponse transform(Response value) {
			NotifyListResponse model = new NotifyListResponse();
			model.notifications = new ArrayList<NotifyListResponse.Notification>();

			if(value.data == null || value.data.isEmpty() || value.data.equalsIgnoreCase("null")) {
				return model;
			}

			// value.data == "[{}, {}, {}]"
			String s = value.data.substring(1, value.data.length() - 1); // s == "{}, {}, {}"
			String[] notificationStrs = s.split(",\\s*\\{"); // notificationStrs == ["{}", "{}", "{}"]
			for(int i = 1; i < notificationStrs.length; i++) {
				notificationStrs[i] = "{" + notificationStrs[i];
			}			

			ObjectMapper mapper = new ObjectMapper();
			for(String notificationStr : notificationStrs) {
				try {
					NotifyListResponse.Notification notification = mapper.readValue(notificationStr, NotifyListResponse.Notification.class);
					model.notifications.add(notification);
				} catch (Exception e) {
					e.printStackTrace();
				}
 			}

			return model;
		}
	}

	public static class NotifyResponseTransformer implements ResponseTransformer<Response, String> {
		@Override
		public String transform(Response value) {
			return value.data;
		}
	}
	
	public static class NotificationStatusResponseTransformer implements ResponseTransformer<Response, NotificationStatus> {
		@Override
		public NotificationStatus transform(Response value) {
			return NotificationStatus.valueOf(value.data);
		}
	}

}
