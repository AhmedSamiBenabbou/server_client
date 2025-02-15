package Common;

public interface CommandCallback {
    void onFailure(String errorMessage);
    void onSuccess(String successMessage);
}
