package org.trading.application.command;

public interface Command<I,O> {

  O execute(I input) throws Exception;
}
