package tally.render;

import java.io.IOException;
import java.io.OutputStream;

import tally.json.JsonResponse;

public interface ResponseRenderer {
  void render(JsonResponse response, OutputStream outputStream) throws IOException;
}