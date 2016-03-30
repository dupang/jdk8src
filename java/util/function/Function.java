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
 * Represents a function that accepts one argument and produces a result.
 * 此函数接受一个参数并产生一个结果的方法
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #apply(Object)}.
 * 这是一个函数式接口,它的函数方法是apply(Object)
 *
 * @param <T> the type of the input to the function
 *            入参的类型
 * @param <R> the type of the result of the function
 *            函数返回的类型
 * @since 1.8
 */
@FunctionalInterface
public interface Function<T, R> {

    /**
     * Applies this function to the given argument.
     * 用给定的参数作用于此函数
     * @param t the function argument
     *          函数的参数
     * @return the function result
     *          函数的结果
     */
    R apply(T t);

    /**
     * Returns a composed function that first applies the {@code before}
     * function to its input, and then applies this function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     * 返回一个复合函数,入参首先调用before函数的apply,然后用返回结果调用apply方法
     * 如果其中任一一个函数调用抛出一个异常，这个异常被传递给调用者
     * @param <V> the type of input to the {@code before} function, and to the
     *           composed function
     *           befoe函数的入参类型，复合函数的返回类型
     * @param before the function to apply before this function is applied
     *               在这个函数调用前被调用的函数
     * @return a composed function that first applies the {@code before}
     * function and then applies this function
     *         首先调用befor函数，然后再调用的函数
     * @throws NullPointerException if before is null
     *         空指针异常如果before为null
     *
     * @see #andThen(Function)
     */
    default <V> Function<V, R> compose(Function<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return (V v) -> apply(before.apply(v));
    }

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * 首先用入参调用 这个函数，然后调用after函数，
     * 如果任一一个函数抛出异常，这个异常被传递给调用者
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     *           after函数的返回类型，复合函数的返回类型
     * @param after the function to apply after this function is applied
     *              这个函数被调用后的调用函数
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     *         一个复合函数，这个函数首先被调用，然后用调用结果作为入参再调用after函数
     * @throws NullPointerException if after is null
     *
     * @see #compose(Function)
     */
    default <V> Function<T, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.apply(apply(t));
    }

    /**
     * Returns a function that always returns its input argument.
     *
     * 返回一个函数，这个函数总是返回它的入参
     *
     * @param <T> the type of the input and output objects to the function
     * @return a function that always returns its input argument
     */
    static <T> Function<T, T> identity() {
        return t -> t;
    }
}
