package org.atsign.common;

import java.util.List;
import java.util.Map;

import org.atsign.client.api.Secondary.Response;
import org.atsign.common.response_models.LlookupAllResponse;

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
