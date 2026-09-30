package com.vital.manatech.rune;

import java.util.ArrayList;
import java.util.List;

/** Immutable snapshot of the bottom-to-top page order. */
public record AssembledSpell(List<String> layers) {
    public static final int MAX_LAYERS = 16;
    private static final int MAX_RAW_LENGTH=MAX_LAYERS*32767;

    public AssembledSpell {
        layers = List.copyOf(layers);
    }

    public static AssembledSpell read(String raw) {
        if (raw == null || raw.length() > 32767 || raw.isEmpty()) return new AssembledSpell(List.of());
        if(raw.startsWith("z1:")) {
            try(var in=new java.util.zip.InflaterInputStream(new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(raw.substring(3))))) {
                byte[] bytes=in.readNBytes(MAX_RAW_LENGTH+1);
                if(bytes.length>MAX_RAW_LENGTH)return new AssembledSpell(List.of());
                raw=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
            } catch(java.io.IOException|IllegalArgumentException exception) { return new AssembledSpell(List.of()); }
        }
        String[] entries = raw.split("\n", -1);
        if (entries.length > MAX_LAYERS) return new AssembledSpell(List.of());
        List<String> valid = new ArrayList<>();
        for (String entry : entries) {
            if (entry.length() > 32767 || DiagramLayer.decode(entry).isEmpty()) return new AssembledSpell(List.of());
            valid.add(entry);
        }
        return new AssembledSpell(valid);
    }

    public String encode() {
        String raw=String.join("\n",layers);
        if(raw.length()<=32767)return raw;
        try {
            var out=new java.io.ByteArrayOutputStream();
            try(var deflate=new java.util.zip.DeflaterOutputStream(out)) { deflate.write(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
            return "z1:"+java.util.Base64.getEncoder().encodeToString(out.toByteArray());
        } catch(java.io.IOException exception) { throw new IllegalStateException(exception); }
    }
    public boolean isEmpty() { return layers.isEmpty(); }
    public List<DiagramLayer> diagrams() { return layers.stream().map(DiagramLayer::decode).toList(); }

    public boolean hasCentralCreation() { return !isEmpty() && diagrams().stream().allMatch(DiagramLayer::hasCentralCreation); }

    public int manaCost() {
        int strokes = diagrams().stream().mapToInt(d -> d.strokes().size()).sum();
        int symbols = diagrams().stream().mapToInt(d -> d.symbols().size()).sum();
        return 10 + 10 * layers.size() + strokes + 4 * symbols;
    }
}
