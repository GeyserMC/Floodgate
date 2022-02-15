package com.minekube.connect.watch;

import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import minekube.connect.v1alpha1.WatchServiceOuterClass.Session;

public class SessionProposal {

    @Getter
    @Setter
    private Session session;
    private final Consumer<com.google.rpc.Status> reject;

    @Getter private State state = State.ACCEPTED;

    public enum State {
        ACCEPTED,
        REJECTED
    }

    public SessionProposal(Session session, Consumer<com.google.rpc.Status> reject) {
        this.session = session;
        this.reject = reject;
    }

    public void reject(com.google.rpc.Status reason) {
        if (state != State.ACCEPTED) {
            state = State.REJECTED;
            reject.accept(reason);
        }
    }
}
