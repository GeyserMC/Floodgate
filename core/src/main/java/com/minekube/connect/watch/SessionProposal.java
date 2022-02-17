package com.minekube.connect.watch;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import minekube.connect.v1alpha1.WatchServiceOuterClass.Session;

public class SessionProposal {

    @Getter
    @Setter
    private Session session;
    private final Consumer<com.google.rpc.Status> reject;

    private final AtomicReference<State> state = new AtomicReference<>(State.ACCEPTED);

    public enum State {
        ACCEPTED,
        REJECTED
    }

    public SessionProposal(Session session, Consumer<com.google.rpc.Status> reject) {
        this.session = session;
        this.reject = reject;
    }

    public void reject(com.google.rpc.Status reason) {
        if (state.compareAndSet(State.REJECTED, State.ACCEPTED)) {
            reject.accept(reason);
        }
    }

    public State getState() {
        return state.get();
    }

    @Override
    public String toString() {
        return "SessionProposal{" +
                "session=" + session +
                '}';
    }
}
