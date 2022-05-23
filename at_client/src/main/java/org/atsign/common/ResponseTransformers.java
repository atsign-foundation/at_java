package org.atsign.common;

import java.util.List;

import org.atsign.client.api.Secondary.Response;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponseTransformers {

	/// Transforms the data from type T to type V
	public interface ResponseTransformer<T, V> {
		V tranform(T value);
	}

	public class ScanResponseTransformer implements ResponseTransformer<Response, List<String>> {

		@Override
		public List<String> tranform(Response value) {

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

	public class NotifyResponseTransformer implements ResponseTransformer<Response, String> {
		@Override
		public String tranform(Response value) {
			return value.data;
		}
	}
	
	
	public class NotificationStatusResponseTransformer implements ResponseTransformer<Response, NotificationStatus> {
		@Override
		public NotificationStatus tranform(Response value) {
			return NotificationStatus.valueOf(value.data);
		}
	}

}
