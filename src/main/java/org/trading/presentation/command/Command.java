package org.trading.presentation.command;

public interface Command<I,O> {

  O execute(I input) throws Exception;
}
