package wua.app.wwwgcs;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageOptions;

@WebServlet(name = "MainServlet", value = "/")
public class MainServlet extends HttpServlet {
  private static final Logger LOG = Logger.getLogger("MainServlet");
  private static final long serialVersionUID = 1L;
  private static final String BUCKET_NAME = System.getProperty("BUCKET_NAME");
  private static final Storage STORAGE = StorageOptions.getDefaultInstance().getService();

  // TODO: move the fetch logic to a separate class.
  // TODO: cache content in memcache
  // TODO: support for displaying md as html
  // TODO: use an HTML template to display folder listing
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    String path = request.getRequestURI();
    if (path.startsWith("/favicon.ico")) {
      return; // ignore the request for favicon.ico
    }
    String objectName = path.substring(1); // objectname = path minus leading forward slash
    if (objectName.equals("")) {
      objectName = "index.html";
    }
    objectName = URLDecoder.decode(objectName, "UTF-8");

    LOG.info("Fetching gs://" + BUCKET_NAME + "/" + objectName);
    BlobId blobId = BlobId.of(BUCKET_NAME, objectName);
    Blob blob = STORAGE.get(blobId); // i.e. get the file, not folder.
    if (blob == null) {
      // If blob does not exist, try getting a folder listing
      // BlobListOption.prefix(objectName) => browse this folder
      // BlobListOption.currentDirectory() => no recursive listing
      Page<Blob> blobs = STORAGE.list(BUCKET_NAME, BlobListOption.prefix(objectName), BlobListOption.currentDirectory());
      List<String> results = new ArrayList<>();
      blobs.iterateAll().forEach(b->results.add("/" + b.getName()));
      if (results.isEmpty()) {
        // Path does not point to a file nor folder
        LOG.info("No such object");
        response.setStatus(404);
      }
      else {
        response.setContentType("text/html");
        String before = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "\n" +
            "<head>\n" +
            "  <title>Central Repository: </title>\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "  <style>\n" +
            "body {\n" +
            "  background: #fff;\n" +
            "}\n" +
            "  </style>\n" +
            "</head>\n" +
            "\n" +
            "<body>\n" +
            "  <header>\n" +
            "    <h1></h1>\n" +
            "  </header>\n" +
            "  <hr/>\n" +
            "  <main>\n" +
            "    <pre id=\"contents\">";
        String after = "</pre>\n" +
            "  </main>\n" +
            "  <hr/>\n" +
            "</body>\n" +
            "</html>";
        response.getWriter().println(before);
        response.getWriter().println("<a href=\"../\">../</a>");
        results.forEach(s->{
          try {
            response.getWriter().println("<a href=\"" + s + "\">" + s + "</a>");
          } catch (IOException e) {
            response.setStatus(500);
            LOG.info("No such object");
          }
        });
        response.getWriter().println(after);
      }
      return; // Blob not found. Done with the request.
    }

    // Blob is found, so let's display it
    response.setContentType(blob.getContentType());

    // Reference: https://github.com/GoogleCloudPlatform/google-cloud-java/blob/master/google-cloud-examples/src/main/java/com/google/cloud/examples/storage/StorageExample.java
    if (blob.getSize() < 1_000_000) {
      // Blob is small read all its content in one request
      byte[] content = blob.getContent();
      response.getOutputStream().write(content);
      response.setContentType(blob.getContentType());
    }
    else {
      // When Blob size is big or unknown use the blob's channel reader.
      try (ReadChannel reader = blob.reader()) {
        WritableByteChannel channel = Channels.newChannel(response.getOutputStream());
        ByteBuffer bytes = ByteBuffer.allocate(64 * 1024);
        while (reader.read(bytes) > 0) {
          bytes.flip();
          channel.write(bytes);
          bytes.clear();
        }
      }
    }
  }


}
