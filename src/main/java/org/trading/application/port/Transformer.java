package org.trading.application.port;

public interface Transformer<I,O> {

  O transform(I input) throws IllegalArgumentException;

  I reverseTransform(O output) throws IllegalArgumentException;
}
