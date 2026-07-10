package com.hms.acquisition.search;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.springframework.web.util.UriUtils;

public class MagnetBuilder {
    private static final String MAGNET_PREFIX = "magnet:?xt=urn:btih:";

    private final Charset outputEncoding;
    private final String infoHash;
    private final List<Parameter> parameters;

    private MagnetBuilder(String infoHash, List<Parameter> parameters, Charset outputEncoding) {
        this.infoHash = infoHash;
        this.parameters = List.of(parameters.toArray(new Parameter[0]));
        this.outputEncoding = outputEncoding;
    }

    private MagnetBuilder(String infoHash) {
        this(infoHash, List.of(), StandardCharsets.UTF_8);
    }

    public static MagnetBuilder newBuilder() {
        return new MagnetBuilder(null);
    }

    public static MagnetBuilder newBuilder(String infoHash) {
        return new MagnetBuilder(infoHash);
    }

    public MagnetBuilder withInfoHash(String infoHash) {
        return new MagnetBuilder(infoHash, parameters, outputEncoding);
    }

    public MagnetBuilder addParameter(String key, String value) {
        List<Parameter> newParameters = new ArrayList<>(parameters);
        newParameters.add(new Parameter(key, value));
        return new MagnetBuilder(infoHash, newParameters, outputEncoding);
    }

    public String build() {
        if (infoHash == null || infoHash.isEmpty()) {
            throw new IllegalStateException("Info hash must be set before building the magnet link.");
        }

        StringBuilder magnetLink = new StringBuilder(MAGNET_PREFIX).append(infoHash);

        for (Parameter parameter : parameters) {
            magnetLink.append(parameter.build(this::encode));
        }

        return magnetLink.toString();
    }

    record Parameter(String key, String value) {
        public String build(Function<String, String> encoder) {
            try {
                return "&" + encoder.apply(key) + "=" + encoder.apply(value);
            } catch (Exception e) {
                throw new RuntimeException("Error encoding URL parameters", e);
            }
        }
    }

    private String encode(final String input) {
        return UriUtils.encode(input, outputEncoding);
    }
}
