package codes.som.anthony.niozipfsnullpatch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.Optional;
import java.util.zip.:ZipEntry;
import java.util.zip.ZipFile;

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.NOP;

public class NioZipFSNullPatch {
    private static final String ZIPPATH_CLASS_LOCATION = "/com/sun/nio/zipfs/ZipPath.class";

    public static void main(String[] args) throws IOException {
        String[] bootstrapJars = ManagementFactory.getRuntimeMXBean().getClassPath().split(":");
        Optional<String> zipFSJar = Arrays.stream(bootstrapJars).filter(jar -> jar.endsWith("zipfs.jar")).findFirst();

        if (!zipFSJar.isPresent())
            throw new IllegalStateException("There is no zipfs.jar in the classpath!");

        File zipFSJarFile = new File(zipFSJar.get());
        ZipFile zipFS = new ZipFile(zipFSJarFile);
        ZipEntry zipPathClassEntry = zipFS.getEntry(ZIPPATH_CLASS_LOCATION.substring(1));

        ClassNode zipPathNode = readClassNode(zipFS.getInputStream(zipPathClassEntry));
        patchClassFile(zipPathNode);
        byte[] classBuffer = assembleClassNode(zipPathNode);

        Path newZipFile = Paths.get("zipfile.patched.jar").normalize();
        Files.deleteIfExists(newZipFile);
        Files.copy(zipFSJarFile.toPath(), newZipFile);

        try (FileSystem fs = FileSystems.newFileSystem(newZipFile, null)) {
            Path zipPathClassPath = fs.getPath(ZIPPATH_CLASS_LOCATION);

            Files.deleteIfExists(zipPathClassPath);
            Files.write(zipPathClassPath, classBuffer, StandardOpenOption.CREATE);
        }
    }

    private static void patchClassFile(ClassNode zipPathNode) {
        zipPathNode.methods.stream().filter(method -> method.name.equals("normalize")).forEach(method -> {
            ListIterator<AbstractInsnNode> insnIterator = method.instructions.iterator();

            int constantIndex = 0;
            boolean found = false;
            while (insnIterator.hasNext()) {
                AbstractInsnNode instruction = insnIterator.next();

                if (instruction instanceof LdcInsnNode) {
                    LdcInsnNode ldcInsnNode = (LdcInsnNode) instruction;

                    if (ldcInsnNode.cst.equals("Path: nul character not allowed")) {
                        found = true;
                        break;
                    }
                }

                constantIndex++;
            }

            if (found) {
                InsnList newInstructions = new InsnList();

                for (int i = 0; i < method.instructions.size(); i++) {
                    AbstractInsnNode instruction = method.instructions.get(i);

                    if (i >= constantIndex - 6 && i <= constantIndex + 2) {
                        instruction = new InsnNode(NOP);
                    }

                    newInstructions.add(instruction);
                }

                method.instructions = newInstructions;
            }
        });
    }

    private static ClassNode readClassNode(InputStream inputStream) throws IOException {
        ClassReader classReader = new ClassReader(inputStream);
        ClassNode node = new ClassNode();

        classReader.accept(node, EXPAND_FRAMES);

        return node;
    }

    private static byte[] assembleClassNode(ClassNode node) {
        ClassWriter writer = new ClassWriter(COMPUTE_MAXS);
        node.accept(writer);

        return writer.toByteArray();
    }
}
