package org.atsign.common;

import java.util.List;

import org.atsign.client.api.Secondary.Response;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponseTransformers {
	static final ObjectMapper mapper = new ObjectMapper();

	/// Transforms the data from type T to type V
	public interface ResponseTransformer<T, V> {
		V transform(T value);
	}

	public static class ScanResponseTransformer implements ResponseTransformer<Response, List<String>> {

		@Override
		public List<String> transform(Response value) {

			if (value.getRawDataResponse() == null || value.getRawDataResponse().isEmpty()) {
				return null;
			}

			try {
				return mapper.readerForListOf(String.class).readValue(value.getRawDataResponse());
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

	}

	public static class NotifyResponseTransformer implements ResponseTransformer<Response, String> {
		@Override
		public String transform(Response value) {
			throw new RuntimeException("Not Implemented");
		}
	}
	
	
	public static class NotificationStatusResponseTransformer implements ResponseTransformer<Response, NotificationStatus> {
		@Override
		public NotificationStatus transform(Response value) {
			throw new RuntimeException("Not Implemented");
		}
	}

}
