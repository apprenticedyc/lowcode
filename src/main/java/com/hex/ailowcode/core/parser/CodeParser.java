package com.hex.ailowcode.core.parser;

public interface CodeParser<T> {

    /**
     * 策略模式封装接口,不同策略需要实现这个接口--解析AI流式返回的结果, 解析为能用的代码
     */
    T parseCode(String codeContent);
}