package demo;

import java.util.concurrent.ExecutionException;

public interface ProducerI {

    boolean pushMessage(String message) throws ExecutionException;
}
