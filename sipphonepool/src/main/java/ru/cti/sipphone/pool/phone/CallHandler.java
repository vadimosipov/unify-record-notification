package ru.cti.sipphone.pool.phone;

import java.util.function.Consumer;

public class CallHandler {
    public final Consumer<Phone> onSuccess;
    public final Runnable onFailed;

    public CallHandler(Consumer<Phone> onSuccess, Runnable onFailed) {
        this.onSuccess = onSuccess;
        this.onFailed = onFailed;
    }
}
