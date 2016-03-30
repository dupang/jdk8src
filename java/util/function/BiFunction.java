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
 * Represents a function that accepts two arguments and produces a result.
 * This is the two-arity specialization of {@link Function}.
 *
 * 表示一个函数，这个函数接受两个参数，并且返回一个结果，这个一个两目运算函数
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #apply(Object, Object)}.
 * 这是一个函数式接口,它的函数方法是apply(Object)
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * @param <R> the type of the result of the function
 *            函数的第一个参数类型
 *            函数的第二个参数类型
 *            返回结果的类型
 * @see Function
 * @since 1.8
 */
@FunctionalInterface
public interface BiFunction<T, U, R> {

    /**
     * Applies this function to the given arguments.
     * 用给定的参数调用这个函数
     * @param t the first function argument
     * @param u the second function argument
     * @return the function result
     * 第一个函数参数
     * 第二个函数参数
     * 函数结果
     */
    R apply(T t, U u);

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     * 返回一个复合函数，首先用入参调用这个函数，然后用返回结果作为入参调用after函数
     * 如果其中任一一个函数调用抛出一个异常，这个异常被传递给调用者
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     *           after函数的返回类型，复合函数的返回类型
     * @param after the function to apply after this function is applied
     *              这个函数被调用后的调用函数
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     *          一个复合函数，首先这个函数被调用，然后用返回结果作入参调用after函数
     * @throws NullPointerException if after is null
     */
    default <V> BiFunction<T, U, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t, U u) -> after.apply(apply(t, u));
    }
}
