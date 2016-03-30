/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package java.util.function;

import java.util.Objects;

/**
 * Represents an operation that accepts a single input argument and returns no
 * result. Unlike most other functional interfaces, {@code Consumer} is expected
 * to operate via side-effects.
 * 表示一个操作，这个操作接受一个入参，并且没有返回结果，不同于其它函数式接口，Consumer被用来产生副作用
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #accept(Object)}.
 *  这个一个函数式接口，它的函数方法是accept
 * @param <T> the type of the input to the operation
 *
 * @since 1.8
 */
@FunctionalInterface
public interface Consumer<T> {

    /**
     * Performs this operation on the given argument.
     * 在给定的参数上执行此操作
     * @param t the input argument
     *          入参
     */
    void accept(T t);

    /**
     * Returns a composed {@code Consumer} that performs, in sequence, this
     *
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * 返回一个要执行的复合Consumer，按顺序这个操作在after操作之后，
     * 如果执行任一一个操作抛出一个异常，这个异常被返回给调用者，如果执行这个操作抛出一个异常，after操作就不会被执行
     * @param after the operation to perform after this operation
     *              在这个操作之后要执行的操作
     * @return a composed {@code Consumer} that performs in sequence this
     * operation followed by the {@code after} operation
     *         在after操作之后要执行的操作
     *
     * @throws NullPointerException if {@code after} is null
     */
    default Consumer<T> andThen(Consumer<? super T> after) {
        Objects.requireNonNull(after);
        return (T t) -> { accept(t); after.accept(t); };
    }
}
