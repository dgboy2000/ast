package ast;

import com.google.common.io.Files;
import junit.framework.TestCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dannygoodman
 * Date: 6/7/14
 * Time: 3:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class PostOrderVisitorTest extends TestCase
{
    public void testOnMetromileCode() throws IOException {
        String dirname = "/Users/dannygoodman/Sites/metromile/mms/";
//        String dirname = "/Users/dannygoodman/Sites/metromile/mms/Services/src/main/java/com/metromile/parking/";
        for (File file : FileListing.listFiles(dirname))
        {
            String filename = file.toString();
            if (filename.length() > 5 && filename.substring(filename.length()-5).equalsIgnoreCase(".java"))
            {
                String thisFileContents = Files.toString(file, Charset.defaultCharset());
                System.out.println("Minified "+filename+":");
                String minified = PostOrderVisitor.minify(thisFileContents);
                System.out.println(minified);
                assertEquals(minified, PostOrderVisitor.minify(minified));
            }
        }
    }





    public static final class FileListing {
        public static List<File> listFiles(String directory) throws FileNotFoundException {
            File startingDirectory= new File(directory);
            FileListing listing = new FileListing();
            return listing.getFileListing(startingDirectory);
        }

        /**
         * Recursively walk a directory tree and return a List of all
         * Files found; the List is sorted using File.compareTo().
         *
         * @param aStartingDir is a valid directory, which can be read.
         */
        public List<File> getFileListing(
                File aStartingDir
        ) throws FileNotFoundException {
            validateDirectory(aStartingDir);
            List<File> result = getFileListingNoSort(aStartingDir);
            Collections.sort(result);
            return result;
        }

        // PRIVATE

        private List<File> getFileListingNoSort(
                File aStartingDir
        ) throws FileNotFoundException {
            List<File> result = new ArrayList<File>();
            File[] filesAndDirs = aStartingDir.listFiles();
            List<File> filesDirs = Arrays.asList(filesAndDirs);
            for(File file : filesDirs) {
                result.add(file); //always add, even if directory
                if (! file.isFile()) {
                    //must be a directory
                    //recursive call!
                    List<File> deeperList = getFileListingNoSort(file);
                    result.addAll(deeperList);
                }
            }
            return result;
        }

        /**
         * Directory is valid if it exists, does not represent a file, and can be read.
         */
        private void validateDirectory (
                File aDirectory
        ) throws FileNotFoundException {
            if (aDirectory == null) {
                throw new IllegalArgumentException("Directory should not be null.");
            }
            if (!aDirectory.exists()) {
                throw new FileNotFoundException("Directory does not exist: " + aDirectory);
            }
            if (!aDirectory.isDirectory()) {
                throw new IllegalArgumentException("Is not a directory: " + aDirectory);
            }
            if (!aDirectory.canRead()) {
                throw new IllegalArgumentException("Directory cannot be read: " + aDirectory);
            }
        }
    }
}
