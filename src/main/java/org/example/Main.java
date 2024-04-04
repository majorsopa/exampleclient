package org.example;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Main {
    public static void agentmain(String ignoredAgentArgs, Instrumentation inst) throws URISyntaxException, IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ExampleClassLoader cl = null;
        Class<?> mcClClazz = null;
        ClassLoader mcCl = null;
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (clazz.getTypeName().startsWith("net.minecraft")) {
                mcCl = clazz.getClassLoader();
                mcClClazz = clazz.getClassLoader().getClass();
                cl = new ExampleClassLoader(clazz.getClassLoader());
                break;
            }
        }
        if (cl == null) {
            throw new RuntimeException("Could not find Minecraft classloader");
        }

        {
            // use reflection to call defineClass on mcClClazz to load this jar
            // brute force dependency ordering :troll:
            JarFile jarFile = ExampleClassLoader.getJar();
            ArrayList<JarEntry> entries = new ArrayList<>();
            for (JarEntry file : jarFile.stream().toList()) {
                if (
                        !file.getName().endsWith(".class")
                                || !file.getName().startsWith("org/example")    // also exclude libraries already in fabric or whatever to avoid conflicts
                )
                {
                    continue;
                }
                entries.add(file);
            }
            while (!entries.isEmpty()) {
                ArrayList<JarEntry> failed = new ArrayList<>();
                for (JarEntry file : entries) {
                    byte[] classBytes = jarFile.getInputStream(file).readAllBytes();
                    String name = file.getName().replace("/", ".").replace(".class", "");
                    Method m = mcClClazz.getMethod("defineClassFwd", String.class, byte[].class, int.class, int.class, CodeSource.class);
                    m.setAccessible(true);
                    try {
                        m.invoke(mcCl, name, classBytes, 0, classBytes.length, null);
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        if (e.getCause() instanceof LinkageError) {
                            failed.add(file);
                        } else {
                            throw e;
                        }
                    }
                }
                if (failed.size() == entries.size()) {
                    throw new RuntimeException("Failed to load any classes");
                } else {
                    entries = failed;
                }
            }
        }

        ExampleClient.runClient();
    }
}