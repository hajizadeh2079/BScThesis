package com.example.converter;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class ConverterApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConverterApplication.class, args);
    }
}

@RestController
class MyController {

    private static List<Span> convertToOtlp(String zipkinJson) {
        JSONArray zipkinArray = new JSONArray(zipkinJson);
        List<Span> spans = new ArrayList<>();

        for (int i = 0; i < zipkinArray.length(); i++) {
            JSONObject zipkinObject = zipkinArray.getJSONObject(i);
            Span span = buildSpan(zipkinObject);
            spans.add(span);
        }

        return spans;
    }

    private static Span buildSpan(JSONObject zipkinObject) {
        String traceId = zipkinObject.getString("traceId");
        String spanId = zipkinObject.getString("id");
        String parentId = zipkinObject.optString("parentId", "");
        String name = zipkinObject.getString("name");
        long timestamp = zipkinObject.optLong("timestamp", 0);
        long duration = zipkinObject.optLong("duration", 0);

        Span.Builder spanBuilder = Span.newBuilder()
                .setTraceId(ByteString.copyFrom(traceId.getBytes()))
                .setSpanId(ByteString.copyFrom(spanId.getBytes()))
                .setParentSpanId(ByteString.copyFrom(parentId.getBytes()))
                .setName(name)
                .setStartTimeUnixNano(timestamp)
                .setEndTimeUnixNano(timestamp + duration)
                .setKind(Span.SpanKind.SPAN_KIND_SERVER)
                .setStatus(createStatus(zipkinObject))
                .addAllAttributes(createAttributes(zipkinObject))
                .addAllEvents(createEvents(zipkinObject))
                .addAllLinks(createLinks(zipkinObject));

        return spanBuilder.build();
    }

    private static Status createStatus(JSONObject zipkinObject) {
        String outcome = zipkinObject.getJSONObject("tags").optString("outcome");
        String statusCode = zipkinObject.getJSONObject("tags").optString("status");

        return Status.newBuilder()
                .setCode(getOtlpStatusCode(statusCode))
                .setMessage(outcome)
                .build();
    }

    private static Status.StatusCode getOtlpStatusCode(String statusCode) {
        return switch (statusCode) {
            case "200" -> Status.StatusCode.STATUS_CODE_OK;
            case "ERROR" -> Status.StatusCode.STATUS_CODE_ERROR;
            default -> Status.StatusCode.STATUS_CODE_UNSET;
        };
    }

    private static List<Span.Event> createEvents(JSONObject zipkinObject) {
        JSONArray eventsArray = zipkinObject.optJSONArray("events");
        List<Span.Event> eventList = new ArrayList<>();

        if (eventsArray != null) {
            for (int i = 0; i < eventsArray.length(); i++) {
                JSONObject eventObject = eventsArray.getJSONObject(i);
                long timestamp = eventObject.getLong("timestamp");
                String name = eventObject.getString("name");

                Span.Event.Builder eventBuilder = Span.Event.newBuilder()
                        .setTimeUnixNano(timestamp)
                        .setName(name);

                eventList.add(eventBuilder.build());
            }
        }

        return eventList;
    }

    private static List<Span.Link> createLinks(JSONObject zipkinObject) {
        JSONArray linksArray = zipkinObject.optJSONArray("links");
        List<Span.Link> linkList = new ArrayList<>();

        if (linksArray != null) {
            for (int i = 0; i < linksArray.length(); i++) {
                JSONObject linkObject = linksArray.getJSONObject(i);
                String traceId = linkObject.getString("traceId");
                String spanId = linkObject.getString("spanId");
                String serviceName = linkObject.optString("serviceName");

                Span.Link.Builder linkBuilder = Span.Link.newBuilder()
                        .setTraceId(ByteString.copyFrom(traceId.getBytes()))
                        .setSpanId(ByteString.copyFrom(spanId.getBytes()));

                if (!serviceName.isEmpty()) {
                    linkBuilder.addAttributes(
                            KeyValue.newBuilder()
                                    .setKey("peer.service")
                                    .setValue(AnyValue.newBuilder().setStringValue(serviceName).build())
                                    .build()
                    );
                }

                linkList.add(linkBuilder.build());
            }
        }

        return linkList;
    }

    private static List<KeyValue> createAttributes(JSONObject zipkinObject) {
        JSONObject tagsObject = zipkinObject.optJSONObject("tags");
        List<KeyValue> attributeList = new ArrayList<>();

        if (tagsObject != null) {
            for (String key : tagsObject.keySet()) {
                String value = tagsObject.getString(key);

                KeyValue.Builder attributeBuilder = KeyValue.newBuilder()
                        .setKey(key)
                        .setValue(AnyValue.newBuilder().setStringValue(value).build());

                attributeList.add(attributeBuilder.build());
            }
        }

        return attributeList;
    }

    @PostMapping("/convert")
    public void convertTrace(@RequestBody byte[] zipkinSpans) {
        String zipkinJson = new String(zipkinSpans);
        List<Span> otlpSpans = convertToOtlp(zipkinJson);
        System.out.println("OTLP JSON: " + otlpSpans);
    }
}
