package queue;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import misc.Constants;

public class MessageQueue implements Serializable {

    private final String name;
    private ConcurrentLinkedDeque<Message> queue = new ConcurrentLinkedDeque<>();

    private volatile Instant nextTimeout = Instant.MIN;
    private volatile Optional<String> lastClientToken = Optional.empty();

    public MessageQueue(String name){
        this.name = name;
    }

    public MessageQueue(String name, List<Message> snapshot){
        this.name = name;
        queue = new ConcurrentLinkedDeque<>(snapshot);
    }

    public synchronized void commit(UUID messageId){
        if(!queue.isEmpty() && queue.peekLast().getMessageId().equals(messageId)){
            queue.pollLast();
            lastClientToken = Optional.empty();
            nextTimeout = Instant.MIN;
        }
    }

    public synchronized void insert(Message message){
        if(queue.isEmpty() || message.getLogIndex() > queue.peekFirst().getLogIndex()){
            queue.addFirst(message);
        }
    }

    public synchronized Optional<Message> peek(Optional<String> clientToken) {
        Optional<Message> returnValue;
        if(!queue.isEmpty() && (nextTimeout.compareTo(Instant.now()) < 0) ||
                clientToken.isPresent() && clientToken.equals(lastClientToken)){
            nextTimeout = Instant.now().plus(Constants.REDELIVERY_TIMEOUT);
            returnValue = Optional.of(queue.peekLast());
            lastClientToken = clientToken;
        } else {
            returnValue = Optional.empty();
        }
        return returnValue;
    }

    public List<Message> snapshot(){
        List<Message> res = new ArrayList<>();
        //Snapshot is lock free and therefore not perfectly consistent, but it is sufficient to converge to the correct state when applying the log entries
        for(Message message : queue){
            res.add(message);
        }
        return res;
    }

}
