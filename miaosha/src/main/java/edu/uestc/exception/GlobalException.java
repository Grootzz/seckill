package edu.uestc.exception;

import edu.uestc.controller.result.CodeMsg;

/**
 * 全局异常处理器
 */
public class GlobalException extends RuntimeException {

    private CodeMsg codeMsg;

    /**
     * 使用构造器接收CodeMsg
     *
     * @param codeMsg
     */
    public GlobalException(CodeMsg codeMsg) {
        this.codeMsg = codeMsg;
    }

    public CodeMsg getCodeMsg() {
        return codeMsg;
    }
}
