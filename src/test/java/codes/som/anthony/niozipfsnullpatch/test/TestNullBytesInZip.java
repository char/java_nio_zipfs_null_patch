package codes.som.anthony.niozipfsnullpatch.test;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TestNullBytesInZip {
    @Test
    public void test() throws IOException {
        File tempFile = File.createTempFile("null-", ".zip");
        writeNullPathsToZip(tempFile);

        try (FileSystem fs = FileSystems.newFileSystem(tempFile.toPath(), null)) {
            Files.walkFileTree(fs.getPath("/"), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith("/hello.txt")) {
                        List<String> lines = Files.readAllLines(file);
                        Assert.assertTrue(lines.contains("Hello, world"));
                    }

                    return super.visitFile(file, attrs);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    private static void writeNullPathsToZip(File file) throws IOException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file));

        zipOutputStream.putNextEntry(new ZipEntry("\u0000directory/"));
        zipOutputStream.closeEntry();

        zipOutputStream.putNextEntry(new ZipEntry("\u0000directory/hello.txt"));
        zipOutputStream.write("Hello, world".getBytes());
        zipOutputStream.closeEntry();

        zipOutputStream.close();
    }
}
