package xyz.sheeld.app.api.interfaces;

public interface DataCallbackInterface<T> {
    void onSuccess(T data);
    void onFailure(Throwable t);
}
