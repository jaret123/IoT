package com.elyxor.xeros;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class RSMapper extends com.fasterxml.jackson.databind.ObjectMapper {

	private static final long serialVersionUID = 6805914282129159573L;

	public RSMapper() {
	     final AnnotationIntrospector introspector = new JacksonAnnotationIntrospector();
	     
	     super.getDeserializationConfig().with(introspector);
	     
	     super.getSerializationConfig().with(introspector);
	     super.getSerializationConfig().with(SerializationFeature.WRAP_ROOT_VALUE);
	     
	     this.setAnnotationIntrospector(introspector);
	}
}
