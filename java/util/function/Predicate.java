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
 * Represents a predicate (boolean-valued function) of one argument.
 * 表示这个参数的断言(布尔值函数)
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #test(Object)}.
 * 这是一个函数式接口，这经的函数方法是test(Object)
 * @param <T> the type of the input to the predicate
 *
 * @since 1.8
 */
@FunctionalInterface
public interface Predicate<T> {

    /**
     * Evaluates this predicate on the given argument.
     * 用给定的参数计算这个断言
     *
     * @param t the input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     *
     * 入参
     * 如果入参匹配这个断言，返回true,否则返回false
     */
    boolean test(T t);

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * AND of this predicate and another.  When evaluating the composed
     * predicate, if this predicate is {@code false}, then the {@code other}
     * predicate is not evaluated.
     *
     * 返回一个复合断言，它表示一个这个断言和另一个断言的断路逻辑AND
     * 在计算这个复合断言时，如果这个断言是false，那么other断言不会被计算
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     * 在计算这些断言时有任何异常被抛出，这个异常就会被返回给调用者，如果在计算 这个断言时抛出异常，那么other断言就不会被计算
     * @param other a predicate that will be logically-ANDed with this
     *              predicate
     *              被用来和这个断言逻辑AND的另一个断言
     * @return a composed predicate that represents the short-circuiting logical
     * AND of this predicate and the {@code other} predicate
     *         一个复合断言，它表示一个这个断言和另一个断言的断路逻辑AND
     * @throws NullPointerException if other is null
     */
    default Predicate<T> and(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) && other.test(t);
    }

    /**
     * Returns a predicate that represents the logical negation of this
     * predicate.
     * 返回一个断言，这个断言表示这个断言的逻辑非
     * @return a predicate that represents the logical negation of this
     * predicate
     * 返回一个断言，这个断言表示这个断言的逻辑非
     */
    default Predicate<T> negate() {
        return (t) -> !test(t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * OR of this predicate and another.  When evaluating the composed
     * predicate, if this predicate is {@code true}, then the {@code other}
     * predicate is not evaluated.
     * 返回一个复合断言，这个断言表示这个断言和另一个断言的逻辑Or,当计算 这个复合断言时，如果这个断言为true,那么另一个断言不会被计算
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     * 在计算这些断言时，任一一个抛出异常，就会传递给他的调用者，如果在计算这个断言时抛出异常 ，那么other断言就不会被计算
     * @param other a predicate that will be logically-ORed with this
     *              predicate
     *              一个被用来和这个断言进行逻辑或的另一个断言
     * @return a composed predicate that represents the short-circuiting logical
     * OR of this predicate and the {@code other} predicate
     *         一个表示这个断言和另一个断言进行逻辑或的复合断言
     * @throws NullPointerException if other is null
     */
    default Predicate<T> or(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) || other.test(t);
    }

    /**
     * Returns a predicate that tests if two arguments are equal according
     * to {@link Objects#equals(Object, Object)}.
     *
     * 返回一个断言，这个断言根据Object.equals(Object,Object)方法判断两个参数是否相等
     *
     * @param <T> the type of arguments to the predicate
     * @param targetRef the object reference with which to compare for equality,
     *               which may be {@code null}
     *            断言的参数类型，
     *            用来比较是否相等的对象引用，可能为null
     * @return a predicate that tests if two arguments are equal according
     * to {@link Objects#equals(Object, Object)}
     */
    static <T> Predicate<T> isEqual(Object targetRef) {
        return (null == targetRef)
                ? Objects::isNull
                : object -> targetRef.equals(object);
    }
}
