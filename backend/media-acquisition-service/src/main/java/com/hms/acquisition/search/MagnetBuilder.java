package com.hms.acquisition.search;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.springframework.web.util.UriUtils;

import io.mikael.urlbuilder.util.Encoder;

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
        // final StringBuilder sb = new StringBuilder();
        // final char[] inputChars = input.toCharArray();
        // for (int i = 0; i < Character.codePointCount(inputChars, 0, inputChars.length); i++) {
        //     final CharBuffer cb;
        //     final int codePoint = Character.codePointAt(inputChars, i);
        //     if (Character.isBmpCodePoint(codePoint)) {
        //         final char c = Character.toChars(codePoint)[0];
        //         cb = CharBuffer.allocate(1);
        //         cb.append(c);
        //     } else {
        //         cb = CharBuffer.allocate(2);
        //         cb.append(Character.highSurrogate(codePoint));
        //         cb.append(Character.lowSurrogate(codePoint));
        //     }
        //     cb.rewind();
        //     final ByteBuffer bb = outputEncoding.encode(cb);
        //     for (int j = 0; j < bb.limit(); j++) {
        //         // Until someone has a real problem with the performance of this bit,
        //         // I will leave this less optimal, but much simpler implementation in place
        //         sb.append('%');
        //         sb.append(String.format(Locale.US, "%1$02X", bb.get(j)));
        //     }
        // }
        // return sb.toString();
        // Encoder encoder = new Encoder(outputEncoding);
        // return encoder.urlEncode(input, false, false, false);
        return UriUtils.encode(input, outputEncoding);
    }
}
