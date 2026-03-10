package org.atsign.cucumber.helpers;


import static org.atsign.client.impl.common.Preconditions.checkFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Using the virtual env requires some corresponding keys and secrets which are available in the
 * dart package
 * at_demo_data. The maven pom should download and extract those files. This class encapsulates
 * using those files.
 */
public class AtDemoData {

  public static final File AT_DEMO_DATA_ROOT = new File("target/at_demo_data");

  private static Map<String, String> FILE_CONTENTS = new HashMap<>();

  static {
    checkFile(AT_DEMO_DATA_ROOT,
              File::exists,
              AT_DEMO_DATA_ROOT + " does not exist have you run mvn generate-test-resources?");
  }

  public static String getClassConst(String filename, String className, String constName) {
    try {
      String fileContents = getFileContents(new File(AT_DEMO_DATA_ROOT, "lib/src/" + filename));
      String regex = "class " + className + " \\{ static const String " + constName + " = '([^']+)'";
      Matcher matcher = Pattern.compile(regex).matcher(fileContents);
      if (matcher.find()) {
        return matcher.group(1);
      }
      throw new RuntimeException("unable to match " + regex + " in " + filename + " contents");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String getFileContents(File file) throws IOException {
    String key = file.getCanonicalPath();
    if (!FILE_CONTENTS.containsKey(key)) {
      String fileContents = Files.lines(file.toPath())
          .filter(line -> !line.matches("^\\s*//.+"))
          .collect(Collectors.joining(" "))
          .replaceAll("\\s+", " ");
      FILE_CONTENTS.put(key, fileContents);
    }
    return FILE_CONTENTS.get(key);
  }

  public static File getDir(File dir) {
    return checkFile(new File(AT_DEMO_DATA_ROOT, dir.getPath()), File::isDirectory, "no such directory");
  }
}
