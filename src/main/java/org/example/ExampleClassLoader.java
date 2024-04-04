package org.example;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.jar.JarFile;

public class ExampleClassLoader extends ClassLoader {
    private static JarFile jarFile = null;

    public ExampleClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public Class<?> loadClass(String name) {
        Class<?> ret = null;
        try {
            ret = getParent().loadClass(name);
        } catch (ClassNotFoundException ignored) {}
        if (ret != null) {
            return ret;
        }

        JarFile thisJar;
        try {
            thisJar = getJar();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        if (thisJar.getEntry(name.replace('.', '/') + ".class") != null) {
            byte[] classBytes;
            try {
                classBytes = thisJar.getInputStream(thisJar.getEntry(name.replace('.', '/') + ".class")).readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return defineClass(name, classBytes, 0, classBytes.length);
        }
        return null;
    }

    @Override
    public Class<?> findClass(String name) {
        return loadClass(name);
    }

    public static JarFile getJar() throws URISyntaxException, IOException {
        if (jarFile == null) {
            jarFile = new JarFile(new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        }
        return jarFile;
    }
}