package com.hms.html;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;

import com.hms.html.document.DefaultDocumentBuilder;

public class DocumentBuilderFactory {
    private Map<String, DocumentBuilder> documentBuilderMap = new HashMap<>();
    private static DocumentBuilderFactory instance = new DocumentBuilderFactory();

    private DocumentBuilderFactory() {
        // ClassGraph classGraph = new
        // ClassGraph().enableAllInfo().acceptPackages("com.hms.html.document");
        // try (ScanResult scanResult = classGraph.scan()) {
        // List<ClassInfo> documentBuilderClasses = scanResult
        // .getClassesWithAnnotation(DocumentTag.class);

        // for (ClassInfo classInfo : documentBuilderClasses) {
        // Class<?> clazz = classInfo.loadClass();
        // // Register component builder class if needed
        // Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
        // documentBuilderMap.put(clazz.getAnnotation(DocumentTag.class).name(),
        // constructor);
        // }
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        // documentBuilderMap.put("default",
        // DefaultDocumentBuilder.class.getDeclaredConstructors()[0]);

        ResourceLoader resourceLoader = new DefaultResourceLoader();
        // Resource resource =
        // resourceLoader.getResource(ResourceLoader.CLASSPATH_URL_PREFIX +
        // "/templates");

        try {
            Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                    .getResources("classpath*:templates/*.html");
            for (Resource resource : resources) {
                if (resource.getFilePath().toString().endsWith("index.html")) {
                    String keyPath = buildKeyPath(resource.getFilePath()).toString().replace("\\", "/");
                    documentBuilderMap.put(keyPath, new DefaultDocumentBuilder(buildSourcePath(resource.getFilePath()).toString()));
                }
            }

            // Files.walkFileTree(resource.getFilePath(), new SimpleFileVisitor<Path>() {
            //     @Override
            //     public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            //         if (file.toString().endsWith("index.html")) {
            //             String keyPath = buildKeyPath(file).toString().replace("\\", "/");
            //             documentBuilderMap.put(keyPath, new DefaultDocumentBuilder(buildSourcePath(file).toString()));
            //         }
            //         return FileVisitResult.CONTINUE;
            //     }
            // });
            // Path resourcePath = resource.getFilePath();
            // Arrays.stream(resource.getFile().listFiles((dir, name) ->
            // name.endsWith("index.html"))).forEach(file -> {
            // Path filePath = resourcePath.resolve(file.toPath());

            // String fileName = file.getName();
            // // String nameWithoutExtension = fileName.substring(0,
            // // fileName.lastIndexOf('.'));
            // // if (file.getParentFile() != null &&
            // // !file.getParentFile().getName().equals("templates")) {
            // // nameWithoutExtension = file.getParentFile().getName();
            // // }
            // String keyPath = buildKeyPath(filePath).toString().replace("\\", "/");
            // documentBuilderMap.put(keyPath, new
            // DefaultDocumentBuilder(buildSourcePath(filePath).toString()));
            // });
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static DocumentBuilder getDocumentBuilder(String name) throws Exception {
        // Constructor<?> constructor = getInstance().documentBuilderMap.get(name);
        // if (constructor == null) {
        // constructor = getInstance().documentBuilderMap.get("default");
        // }
        // constructor.setAccessible(true);
        // constructor.getParameters();
        // List<String> paramNames = new ArrayList<>();

        // for (var param : constructor.getParameters()) {
        // if (param.isAnnotationPresent(ComponentField.class)) {
        // String fieldName = param.getAnnotation(ComponentField.class).name();
        // if (fieldName == null || fieldName.isEmpty()) {
        // fieldName = param.getName();
        // }
        // paramNames.add(fieldName);
        // }
        // }
        // List<Object> paramValues = new ArrayList<>();
        // for (String paramName : paramNames) {
        // String value = ""; // Replace with appropriate logic to get the value for the
        // parameter
        // paramValues.add(value);
        // }

        // return (DocumentBuilder) constructor.newInstance(paramValues.toArray());
        return getInstance().documentBuilderMap.get(name);
    }

    private Path buildKeyPath(Path absolutePath) {
        boolean hasTemplatesFolder = false;
        Path keyPath = null;
        for (Path part : absolutePath) {
            if (part.toString().endsWith(".html")) {
                if (keyPath == null) {
                    keyPath = Path.of(part.getFileName().toString().replace(".html", ""));
                }
                break;
            }
            if (hasTemplatesFolder && keyPath != null) {
                keyPath = keyPath.resolve(part);
            }
            if (hasTemplatesFolder && keyPath == null) {
                keyPath = part;
            }
            if (part.toString().equals("templates")) {
                hasTemplatesFolder = true;
            }
        }
        if (!hasTemplatesFolder || keyPath == null) {
            throw new IllegalArgumentException("The provided path does not contain a 'templates' folder.");
        }
        return keyPath;
    }

    private Path buildSourcePath(Path absolutePath) {
        boolean hasTemplatesFolder = false;
        Path sourcePath = null;
        for (Path part : absolutePath) {
            // if (part.toString().endsWith(".html")) {
            // break;
            // }
            if (hasTemplatesFolder && sourcePath != null) {
                sourcePath = sourcePath.resolve(part);
            }
            if (part.toString().equals("templates")) {
                hasTemplatesFolder = true;
                sourcePath = part;
            }
        }
        if (!hasTemplatesFolder || sourcePath == null) {
            throw new IllegalArgumentException("The provided path does not contain a 'templates' folder.");
        }
        return sourcePath;
    }

    public static DocumentBuilderFactory getInstance() {
        return instance;
    }
}
