package com.northwind.content.persistence;

import com.embabel.agent.core.hitl.FormBindingRequest;
import com.embabel.agent.core.persistence.BlackboardEntryDeserializationContext;
import com.embabel.agent.core.persistence.BlackboardEntrySerializationContext;
import com.embabel.agent.core.persistence.BlackboardEntrySerializer;
import com.embabel.agent.core.persistence.SerializedBlackboardValue;
import com.embabel.ux.form.Form;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Chapter 67: makes a process paused by WaitFor.formSubmission restorable. In Embabel 1.5.3 the default JSON
 * serializer writes a FormBindingRequest that it cannot read back ("missing value for creator parameter form").
 * This serializer stores what the request needs to be rebuilt: the form's title and id, and the type the
 * person's answer is bound to. Serializer beans are consulted, in @Order, before the JSON fallback.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FormBindingRequestSerializer implements BlackboardEntrySerializer {

    static final String TYPE = FormBindingRequest.class.getName();
    static final String CONTENT_TYPE = "application/x-northwind-form-request";

    @Override
    public boolean supportsSerialization(Object value) {
        return value instanceof FormBindingRequest<?>;
    }

    @Override
    public boolean supportsDeserialization(SerializedBlackboardValue value) {
        return TYPE.equals(value.getTypeName()) && CONTENT_TYPE.equals(value.getContentType());
    }

    @Override
    public SerializedBlackboardValue serialize(Object value, BlackboardEntrySerializationContext context) {
        FormBindingRequest<?> request = (FormBindingRequest<?>) value;
        Form form = request.getPayload();
        // The form's title (the prompt shown to the person) is the payload; metadata carries the rest
        return new SerializedBlackboardValue(TYPE, CONTENT_TYPE, form.getTitle().getBytes(StandardCharsets.UTF_8),
                Map.of("formId", form.getId(), "outputClass", request.getOutputClass().getName()));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object deserialize(SerializedBlackboardValue value, BlackboardEntryDeserializationContext context) {
        try {
            Class<?> outputClass = Class.forName(value.getMetadata().get("outputClass"));
            String title = new String(value.getPayload(), StandardCharsets.UTF_8);
            return new FormBindingRequest(new Form(title, List.of(), value.getMetadata().get("formId")), outputClass);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot restore form request: " + e.getMessage(), e);
        }
    }
}
