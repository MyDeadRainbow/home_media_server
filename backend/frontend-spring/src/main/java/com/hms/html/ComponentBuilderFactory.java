package com.hms.html;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Element;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import com.hms.html.component.DefaultComponentBuilder;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

public class ComponentBuilderFactory {
    private Map<String, Constructor<?>> componentBuilderMap = new HashMap<>();
    private static ComponentBuilderFactory instance = new ComponentBuilderFactory();

    private ComponentBuilderFactory() {
        ClassGraph classGraph = new ClassGraph().enableAllInfo().acceptPackages("com.hms.html.component");
        try (ScanResult scanResult = classGraph.scan()) {
            List<ClassInfo> componentBuilderClasses = scanResult
                    .getClassesWithAnnotation(ComponentTag.class);

            for (ClassInfo classInfo : componentBuilderClasses) {
                Class<?> clazz = classInfo.loadClass();
                // Register component builder class if needed
                Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
                componentBuilderMap.put(clazz.getAnnotation(ComponentTag.class).tagName(), constructor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        componentBuilderMap.put("default", DefaultComponentBuilder.class.getDeclaredConstructors()[0]);
    }

    public static ComponentBuilder getComponentBuilder(Element tag) throws Exception {
        Constructor<?> constructor = getInstance().componentBuilderMap.get(tag.tagName());
        if (constructor == null) {
            constructor = getInstance().componentBuilderMap.get("default");
        }
        constructor.setAccessible(true);
        constructor.getParameters();
        List<String> paramNames = new ArrayList<>();

        for (var param : constructor.getParameters()) {
            if (param.isAnnotationPresent(ComponentField.class)) {
                String fieldName = param.getAnnotation(ComponentField.class).name();
                if (fieldName == null || fieldName.isEmpty()) {
                    fieldName = param.getName();
                }
                paramNames.add(fieldName);
            }
        }
        List<Object> paramValues = new ArrayList<>();
        for (String paramName : paramNames) {
            String value = tag.attr(paramName);
            paramValues.add(value);
        }

        return (ComponentBuilder) constructor.newInstance(paramValues.toArray());
    }

    public static String componentsSelector() {
        return String.join(",", getInstance().componentBuilderMap.keySet().stream().toArray(String[]::new));
    }

    public static ComponentBuilderFactory getInstance() {
        return instance;
    }
}
