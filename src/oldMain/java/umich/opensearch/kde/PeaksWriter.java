package umich.opensearch.kde;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.List;
import umich.opensearch.kde.util.Peak;

/**
 * @author Dmitry Avtonomov
 */
public class PeaksWriter {

  public void write(Path out, OpenSearchParams params, List<Peak> peaks) throws IOException {
    if (!out.toFile().createNewFile()) {
      throw new IllegalStateException("Could not create a new output file: " + out.toString());
    }
    try (BufferedWriter bw = new BufferedWriter(
        new OutputStreamWriter(
            new FileOutputStream(out.toFile(), false),
            Charset.forName("UTF-8")))) {
      DecimalFormat formatMz = params.getOutMzFmt();
      DecimalFormat formatInt = params.getOutIntensityFmt();
      for (Peak peak : peaks) {
        bw.write(formatMz.format(peak.x));
        bw.write("\t");
        bw.write(formatInt.format(peak.y));
        bw.newLine();
      }

      bw.flush();
    }
  }
}
