import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Run with Java 11+: java console/tools/NormalizeTranslations.java <properties files...> */
class NormalizeTranslations {
    public static void main(String[] args) throws Exception {
        for (String filename : args) {
            Path path = Paths.get(filename);
            Properties properties = new Properties() {
                @Override public Set<Map.Entry<Object, Object>> entrySet() {
                    Set<Map.Entry<Object, Object>> entries = new TreeSet<>(Comparator.comparing(e -> e.getKey().toString()));
                    entries.addAll(super.entrySet());
                    return entries;
                }
            };
            String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            properties.load(new StringReader(source));
            StringWriter writer = new StringWriter();
            properties.store(writer, null);
            StringBuilder output = new StringBuilder();
            for (String line : source.split("\\R")) {
                if (line.startsWith("#")) { output.append(line).append('\n'); }
            }
            for (String line : writer.toString().split("\\R")) {
                if (!line.startsWith("#")) { output.append(line).append('\n'); }
            }
            Files.write(path, output.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
