import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DuplicateImageFinder {

    private static final Map<String, String> imageChecksums = new HashMap<>();
    private static final String FOLDER_TO_ANALIZE = "C:\\FolderToAnalize";
    private static final String LOG_DUPLICATED_FILE = "log_duplicated.txt";

    public static void main(String[] args) throws FileNotFoundException {
        // String homePath = System.getProperty("user.home").replace("\\", "\\\\");
        String folderPath = FOLDER_TO_ANALIZE;

        initializeFileLog();

        findDuplicateImages(folderPath);
    }

    private static void initializeFileLog() throws FileNotFoundException {
        File file = new File(LOG_DUPLICATED_FILE);
        PrintStream stream = new PrintStream(file);
        System.out.println("From now on "+file.getAbsolutePath()+" will be your console");
        System.setOut(stream);
    }

    private static void findDuplicateImages(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.isDirectory()) {
            System.out.println("The specified path is not a directory!");
            return;
        }
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isFile() && isImageFile(file)) {
                String checksum = getFileChecksum(file);
                if (imageChecksums.containsKey(checksum)) {
                    System.out.println("Duplicate found: " + file.getAbsolutePath()
                            + " and " + imageChecksums.get(checksum));
                    // if (file.delete()) {
                        System.out.println("Foto/Vídeo seria deletado :" + file.getAbsolutePath());
                    // }
                } else {
                    imageChecksums.put(checksum, file.getAbsolutePath());
                }
            } else if (file.isDirectory()) {
                findDuplicateImages(file.getAbsolutePath());
            }
        }
    }

    private static boolean isImageFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")
            || fileName.endsWith(".png") || fileName.endsWith(".bmp")
            || fileName.endsWith(".mpg") || fileName.endsWith(".mp4")
            || fileName.endsWith(".mov");
    }

    private static String getFileChecksum(File file) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
