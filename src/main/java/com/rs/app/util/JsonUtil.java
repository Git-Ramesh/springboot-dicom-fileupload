package com.rs.app.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class JsonUtil {
	private static final ObjectMapper OBJECT_MAPPER;
	private static final ObjectWriter OBJECT_WRITER;

	static {
		OBJECT_MAPPER = new ObjectMapper();
		OBJECT_WRITER = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();
	}

	private JsonUtil() {
		// No Impl..
	}

	public static String convertJavaObjectToJson(Object obj, boolean prettier) throws JsonProcessingException {
		String json = "";
		if (prettier) {
			json = OBJECT_WRITER.writeValueAsString(obj);
		} else {
			json = OBJECT_MAPPER.writeValueAsString(obj);
		}
		return json;
	}

	public static <T> T convertJsonToJavaObject(String json, Class<T> targetObject) throws IOException {
		return OBJECT_MAPPER.readValue(json, targetObject);
	}
}
