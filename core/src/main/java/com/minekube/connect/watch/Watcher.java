package com.minekube.connect.watch;

public interface Watcher {

    void onProposal(SessionProposal proposal);

    void onError(final Throwable t);

    default void onCompleted() {
    }
}

