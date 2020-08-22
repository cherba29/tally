package tally.json;


public class JsonResponse {
  private boolean success;
  private final String message;
  private final JsonData data;

  public JsonResponse(boolean success, String message, JsonData data) {
    this.success = success;
    this.message = message;
    this.data = data;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }

  public JsonData getData() {
    return data;
  }
}
