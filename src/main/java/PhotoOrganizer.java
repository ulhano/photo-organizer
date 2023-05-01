import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;


public class PhotoOrganizer {
    private static final String SOURCE_DIRECTORY = "C:\\input";
    private static final String TARGET_DIRECTORY = "C:\\output";
    // private static final String NOT_MAPPED_DIRECTORY = "C:\\NoDateOriginal";
    // private static final String DATE_FORMAT = "dd/MM/yyyy";

    private static long counter = 0L;
    public static void main(String[] args) throws FileNotFoundException {
        long startTime = System.currentTimeMillis();

        initializeFileLog();

        processImages(SOURCE_DIRECTORY);

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        System.out.println("\nTotal files processed: " + counter);
        
        BigInteger sizeOfDirectory = FileUtils.sizeOfDirectoryAsBigInteger(new File(SOURCE_DIRECTORY));
        BigDecimal sizeInGb = new BigDecimal(sizeOfDirectory).divide(BigDecimal.TWO.pow(30)).setScale(2,RoundingMode.FLOOR);
        System.out.println("Total size processed: " + sizeInGb + " GB");
        System.out.println("Execution time: " + executionTime + " milliseconds\n");
    }

    private static void processImages(String sourceDirectory) {

        // skip '.sync' directories
        if( sourceDirectory.endsWith(".sync") )
            return;

        File sourceFolder = new File(sourceDirectory);
        if (!sourceFolder.isDirectory()) {
            System.out.println("The specified path is not a directory!");
            return;
        }
        
        try {
            for (File sourceFile : Objects.requireNonNull(sourceFolder.listFiles())) {

                if (sourceFile.isFile() && isImageOrVideoFile(sourceFile)) {
                    // skip if file is empty, 0Kb
                    String checkSum = getFileChecksum(sourceFile);
                    if(checkSum.equalsIgnoreCase("E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855"))
                        continue;

                    processImageFile(sourceFile);
                } else if (sourceFile.isDirectory()) {
                    System.out.println("Directory: " + sourceFile.getAbsolutePath());
                    processImages(sourceFile.getAbsolutePath());
                } else {
                    System.out.println("Unmapped file: " + sourceFile.getAbsolutePath());
                }
            }
        } catch (IOException | ImageProcessingException e) {
            System.out.println("Error processing file: " + e.getMessage());
        }
    }

    private static void processImageFile(File sourceFile) throws IOException, ImageProcessingException {
        System.out.println("Processing file: " + sourceFile.getAbsolutePath());
        Path sourceFilePath = sourceFile.toPath();
        String fileExtension = FilenameUtils.getExtension(sourceFile.getName());
        Metadata metadata;

        try (InputStream stream = new FileInputStream(sourceFile)) {
            metadata = ImageMetadataReader.readMetadata(stream);
        }

        Directory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        Date date = null;
        String cameraModel = null;

        if (directory != null) {
            date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);

            directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            cameraModel = directory.getString(ExifIFD0Directory.TAG_MODEL);

            if( cameraModel == null )
                cameraModel = "NoModel";

            // Printing out all metadata in a file (https://drewnoakes.com/code/exif/)
            // for (Directory d : metadata.getDirectories()) {
            //     System.out.println("--- Directory: " + d.getName());
            //     for (Tag tag : d.getTags()) {
            //         System.out.format("[%s] - %s = %s\n",
            //             d.getName(), tag.getTagName(), tag.getDescription());
            //     }
            //     if (d.hasErrors()) {
            //         for (String error : d.getErrors()) {
            //             System.err.format("ERROR: %s", error);
            //         }
            //     }
            // }
        }

        Calendar calendar;
        // Imagens
        if (date != null) {
            calendar = getCalendar(date);

            // TARGET_DIRECTORY/ ANO/ ANO.MES/
            String month = (calendar.get(Calendar.MONTH) + 1)  > 10 ? (calendar.get(Calendar.MONTH) + 1) + "" : "0" + (calendar.get(Calendar.MONTH) + 1);
            String targetDirectoryPath = String.format("%s/%s/%s.%s/%s", TARGET_DIRECTORY, calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR), month, cameraModel );

            File targetDirectoryFile = new File(targetDirectoryPath);
            if (!targetDirectoryFile.exists()) {
                targetDirectoryFile.mkdirs();
            }

            //normaliza fileName para ANO_MES_DIA_HORAMINUTOSEGUNDO (se igual acrescenta -1 incrementando o numero)
            int fileNameCounter = 0;
            SimpleDateFormat sdf1 = new SimpleDateFormat("YYYYMMdd_HHmmss", Locale.forLanguageTag("pt_BR"));
            sdf1.setTimeZone(calendar.getTimeZone());
            String baseFileName = sdf1.format(calendar.getTime());
            copyFile(baseFileName, fileNameCounter, fileExtension, sourceFilePath, targetDirectoryPath);
        }
        // Videos
        else {
            // System.out.println("No ExifSubIFDDirectory found in metadata");
            // copyToFallbackFolder(sourceFile);
            Path path = Paths.get(sourceFile.getAbsolutePath());
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
            date = new Date(attr.lastModifiedTime().toMillis());
            calendar = getCalendar(date);

             // TARGET_DIRECTORY/ ANO/ ANO.MES/ Videos/
             String month = (calendar.get(Calendar.MONTH) + 1)  > 10 ? (calendar.get(Calendar.MONTH) + 1) + "" : "0" + (calendar.get(Calendar.MONTH) + 1);
             String targetDirectoryPath = String.format("%s/%s/%s.%s/Videos", TARGET_DIRECTORY, calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR), month );

            File targetDirectoryFile = new File(targetDirectoryPath);
            if (!targetDirectoryFile.exists()) {
                targetDirectoryFile.mkdirs();
            }

            //normaliza fileName para ANO_MES_DIA_HORAMINUTOSEGUNDO (se igual acrescenta -1 incrementando o numero)
            int fileNameCounter = 0;
            SimpleDateFormat sdf1 = new SimpleDateFormat("YYYYMMdd_HHmmss", Locale.forLanguageTag("pt_BR"));
            // nao entendi, mas os videos ele interpreta corretamente sem precisar definir timezone como as imagens
            // sdf1.setTimeZone(calendar.getTimeZone());
            String baseFileName = sdf1.format(calendar.getTime());
            copyFile(baseFileName, fileNameCounter, fileExtension, sourceFilePath, targetDirectoryPath);
        }
        counter++;
        // System.out.println("==================================================");
    }

    private static void copyFile(String baseFileName, int count, String fileExtension, Path sourceFilePath, String targetDirectoryPath ) throws IOException {
        String newFileName = baseFileName + (count>0 ? "-"+count : "") + "." + fileExtension;

        Path targetFilePath = Paths.get(String.format("%s/%s", targetDirectoryPath, newFileName));
        if( Files.notExists(targetFilePath, LinkOption.NOFOLLOW_LINKS) ) {
            Files.copy(sourceFilePath, targetFilePath);
        } else {
            copyFile(baseFileName, ++count, fileExtension, sourceFilePath, targetDirectoryPath);
        }
    }

    private static boolean isImageOrVideoFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")
            || fileName.endsWith(".png") || fileName.endsWith(".bmp")
            || fileName.endsWith(".mpg") || fileName.endsWith(".mp4")
            || fileName.endsWith(".mov");
    }

    private static Calendar getCalendar(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("BRT")); // para nao ficar com -3h
        calendar.setTime(date);
        // SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        // String formattedDate = formatter.format(calendar.getTime());
        // System.out.println("Formatted date: " + formattedDate);
        return calendar;
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

    private static void initializeFileLog() throws FileNotFoundException {
        File file = new File("photo_organizer_log.txt");
        PrintStream stream = new PrintStream(file);
        System.out.println("From now on "+ file.getAbsolutePath() +" will be your console");
        System.setOut(stream);
    }

    // private static void copyToFallbackFolder(File sourceFile) throws IOException {
    //     File notMappedDirectory = new File(NOT_MAPPED_DIRECTORY);
    //     if (!notMappedDirectory.exists()) {
    //         notMappedDirectory.mkdirs();
    //     }

    //     Path targetFilePath = Paths.get(notMappedDirectory + "/" + sourceFile.getName());
    //     Files.copy(sourceFile.toPath(), targetFilePath);
    // }
}
