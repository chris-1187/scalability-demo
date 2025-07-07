package demo;

import org.example.qservice.External;

public interface ConsumerI {

    External.Message peek();

    boolean pop(String messageId);
}
