package no.javazone.domain;

public class DoNotRetryException extends RuntimeException {

    public DoNotRetryException(String s) {
        super(s);
    }
}
