import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.openpnp.Translations;
import org.openpnp.gui.components.LocalizedComboBox;
import org.openpnp.model.Solutions;
import org.openpnp.spi.Camera;
import org.openpnp.vision.pipeline.stages.BlurGaussian;

public class ChineseLocalizationTest {
    private Properties catalog(String suffix) throws Exception {
        Properties result = new Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                assertFalse(containsKey(key), "Duplicate translation key: " + key);
                return super.put(key, value);
            }
        };
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream(
                "/org/openpnp/translations" + suffix + ".properties"), StandardCharsets.UTF_8)) {
            result.load(reader);
        }
        return result;
    }

    private Map<String, Integer> placeholders(String value) {
        Map<String, Integer> result = new TreeMap<>();
        Matcher matcher = Pattern.compile("%(?:(\\d+)\\$)?[-#+0,(<]*\\d*(?:\\.\\d+)?([bBhHsScCdoxXeEfgGaAn%])").matcher(value);
        int position = 0;
        while (matcher.find()) {
            String type = matcher.group(2);
            if (type.equals("%") || type.equals("n")) { continue; }
            String index = matcher.group(1) == null ? Integer.toString(++position) : matcher.group(1);
            result.merge(index + ":" + type, 1, Integer::sum);
        }
        return result;
    }

    @Test public void chineseCatalogCoversEnglishAndRetainsFormatArguments() throws Exception {
        Properties english = catalog("");
        Properties chinese = catalog("_zh_CN");
        List<String> errors = new ArrayList<>();
        for (String key : english.stringPropertyNames()) {
            String translated = chinese.getProperty(key);
            if (translated == null || translated.trim().isEmpty()) {
                errors.add("Missing/empty: " + key);
            } else if (!placeholders(english.getProperty(key)).equals(placeholders(translated))) {
                errors.add("Format arguments: " + key + " " + placeholders(english.getProperty(key))
                        + " vs " + placeholders(translated));
            }
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }

    @Test public void literalResourceReferencesResolve() throws Exception {
        Properties english = catalog("");
        Pattern reference = Pattern.compile("Translations\\.(?:getString|format|message)\\(\\\"([^\\\"]+)\\\"\\s*[,)]");
        List<String> missing = new ArrayList<>();
        try (java.util.stream.Stream<Path> files = Files.walk(Paths.get("src/main/java/org/openpnp"))) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    Matcher matcher = reference.matcher(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
                    while (matcher.find()) {
                        if (!english.containsKey(matcher.group(1))) { missing.add(p + ": " + matcher.group(1)); }
                    }
                } catch (IOException e) { throw new UncheckedIOException(e); }
            });
        }
        assertTrue(missing.isEmpty(), String.join("\n", missing));
    }

    @Test public void languageSwitchDoesNotFreezeTheFirstBundle() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            assertEquals("Job", Translations.getString("MainFrame.RightComponent.tabs.Job"));
            Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
            assertEquals("贴装任务", Translations.getString("MainFrame.RightComponent.tabs.Job"));
            assertEquals("对当前图像进行高斯模糊。", new BlurGaussian().getDescription());
            Locale.setDefault(Locale.US);
            assertEquals("Performs gaussian blurring on the working image.", new BlurGaussian().getDescription());
        } finally { Locale.setDefault(previous); }
    }

    @Test public void diagnosticFingerprintRemainsTheOriginalEnglishIdentity() {
        Locale previous = Locale.getDefault();
        try {
            String original = "Nozzle %1$s does not have a Z axis assigned.";
            String key = "Local." + org.apache.commons.codec.digest.DigestUtils.sha256Hex(original).substring(0, 16);
            Solutions.Subject subject = new Solutions.Subject() {
                @Override public String getSubjectText() { return "test-nozzle"; }
            };
            Solutions.Issue before = new Solutions.PlainIssue(subject,
                    "Nozzle N1 does not have a Z axis assigned.", "", Solutions.Severity.Error, null);
            Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
            Solutions.Issue after = new Solutions.PlainIssue(subject, Translations.message(key, "N1"),
                    "", Solutions.Severity.Error, null);
            assertEquals("吸嘴组件 N1 尚未分配 Z 轴。", after.getIssue());
            assertEquals(before.getFingerprint(), after.getFingerprint());
            Locale.setDefault(Locale.US);
            assertEquals(before.getIssue(), after.getIssue());
            assertEquals(before.getFingerprint(), after.getFingerprint());
        } finally { Locale.setDefault(previous); }
    }

    @Test public void enumRenderingRetainsModelValuesAndCustomRenderer() throws Exception {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
            SwingUtilities.invokeAndWait(() -> {
                LocalizedComboBox<Camera.Looking> combo = new LocalizedComboBox<>(Camera.Looking.values());
                Icon icon = UIManager.getIcon("OptionPane.informationIcon");
                combo.setRenderer((list, value, index, selected, focus) -> new JLabel(value.name(), icon, JLabel.LEFT));
                combo.setSelectedItem(Camera.Looking.Up);
                JLabel label = (JLabel) combo.getRenderer().getListCellRendererComponent(new JList<>(),
                        Camera.Looking.Up, 0, false, false);
                assertEquals("上视", label.getText());
                assertSame(icon, label.getIcon());
                assertSame(Camera.Looking.Up, combo.getSelectedItem());
                assertEquals("Up", ((Camera.Looking) combo.getSelectedItem()).name());
                assertTrue(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 14).canDisplayUpTo("吸嘴基准点") == -1);
            });
        } finally { Locale.setDefault(previous); }
    }

    @Test public void visionAnnotationsHaveChineseDescriptionsAndPropertyLabels() throws Exception {
        Properties chinese = catalog("_zh_CN");
        List<String> missing = new ArrayList<>();
        try (io.github.classgraph.ScanResult scan = new io.github.classgraph.ClassGraph().enableClassInfo()
                .acceptPackages("org.openpnp.vision.pipeline.stages").scan()) {
            for (Class<?> type : scan.getSubclasses("org.openpnp.vision.pipeline.CvStage").loadClasses()) {
                org.openpnp.vision.pipeline.Stage stage = type.getAnnotation(org.openpnp.vision.pipeline.Stage.class);
                if (stage != null && !stage.description().isEmpty()) {
                    String key = "Local." + org.apache.commons.codec.digest.DigestUtils.sha256Hex(stage.description()).substring(0, 16);
                    if (!chinese.containsKey(key)) { missing.add(type.getSimpleName() + " description"); }
                }
                for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                    org.openpnp.vision.pipeline.Property property = field.getAnnotation(org.openpnp.vision.pipeline.Property.class);
                    if (property != null) {
                        String key = "Local." + org.apache.commons.codec.digest.DigestUtils.sha256Hex(property.description()).substring(0, 16);
                        if (!property.description().isEmpty() && !chinese.containsKey(key)) { missing.add(type.getSimpleName() + "." + field.getName() + " description"); }
                        if (!chinese.containsKey("Vision.Property." + field.getName())) { missing.add(type.getSimpleName() + "." + field.getName() + " label"); }
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), String.join("\n", missing));
    }
}
